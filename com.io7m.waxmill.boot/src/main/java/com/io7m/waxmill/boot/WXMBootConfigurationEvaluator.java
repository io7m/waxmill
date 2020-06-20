/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.waxmill.boot;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.boot.internal.WXMGRUBDeviceAndPath;
import com.io7m.waxmill.boot.internal.WXMGRUBDeviceMap;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceID;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMException;
import com.io7m.waxmill.machines.WXMExceptionNonexistent;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackends;
import com.io7m.waxmill.machines.WXMVirtualMachine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMEvaluatedBootConfigurationType;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType;

/**
 * Functions over boot configurations.
 */

public final class WXMBootConfigurationEvaluator
{
  private final WXMClientConfiguration clientConfiguration;
  private final WXMBootMessages messages;
  private final WXMVirtualMachine machine;
  private final WXMBootConfigurationName bootName;

  public WXMBootConfigurationEvaluator(
    final WXMClientConfiguration inClientConfiguration,
    final WXMBootMessages inMessages,
    final WXMVirtualMachine inMachine,
    final WXMBootConfigurationName inName)
  {
    this.clientConfiguration =
      Objects.requireNonNull(inClientConfiguration, "clientConfiguration");
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.machine =
      Objects.requireNonNull(inMachine, "machine");
    this.bootName =
      Objects.requireNonNull(inName, "inName");
  }

  public WXMBootConfigurationEvaluator(
    final WXMClientConfiguration inClientConfiguration,
    final WXMVirtualMachine inMachine,
    final WXMBootConfigurationName inName)
  {
    this(
      inClientConfiguration,
      WXMBootMessages.create(),
      inMachine,
      inName
    );
  }

  private WXMEvaluatedBootConfigurationGRUBBhyve evaluateGRUBConfigurationLinux(
    final WXMGRUBDeviceMap deviceMap,
    final WXMGRUBKernelLinux linux)
    throws WXMExceptionNonexistent
  {
    Objects.requireNonNull(linux, "linux");

    final var kernelDevice =
      linux.kernelDevice();
    final var kernelHD =
      deviceMap.searchForHD(kernelDevice);
    final var kernelCD =
      deviceMap.searchForCD(kernelDevice);

    if (kernelHD.isEmpty() && kernelCD.isEmpty()) {
      throw new WXMExceptionNonexistent(
        this.errorNoSuchBootDevice(kernelDevice)
      );
    }

    final var initRDDevice =
      linux.initRDDevice();
    final var initRDHD =
      deviceMap.searchForHD(initRDDevice);
    final var initRDCD =
      deviceMap.searchForCD(initRDDevice);

    if (initRDHD.isEmpty() && initRDCD.isEmpty()) {
      throw new WXMExceptionNonexistent(
        this.errorNoSuchBootDevice(initRDDevice)
      );
    }

    Invariants.checkInvariant(
      kernelHD.isPresent() != kernelCD.isPresent(),
      "A device ID must map to exactly one device"
    );
    Invariants.checkInvariant(
      initRDHD.isPresent() != initRDCD.isPresent(),
      "A device ID must map to exactly one device"
    );

    final var configLines = new ArrayList<String>();

    kernelHD.ifPresent(index -> {
      configLines.add(
        String.format(
          "linux (hd%d)%s %s",
          index,
          linux.kernelPath(),
          String.join(" ", linux.kernelArguments())
        )
      );
    });

    kernelCD.ifPresent(index -> {
      configLines.add(
        String.format(
          "linux (cd%d)%s %s",
          index,
          linux.kernelPath(),
          String.join(" ", linux.kernelArguments())
        )
      );
    });

    initRDHD.ifPresent(index -> {
      configLines.add(
        String.format(
          "initrd (hd%d)%s",
          index,
          linux.initRDPath()
        )
      );
    });

    initRDCD.ifPresent(index -> {
      configLines.add(
        String.format(
          "initrd (cd%d)%s",
          index,
          linux.initRDPath()
        )
      );
    });

    configLines.add("boot");

    return WXMEvaluatedBootConfigurationGRUBBhyve.builder()
      .setDeviceMap(deviceMap.serialize())
      .setGrubConfiguration(configLines)
      .build();
  }

  private WXMEvaluatedBootConfigurationGRUBBhyve evaluateGRUBConfigurationOpenBSD(
    final WXMGRUBDeviceMap deviceMap,
    final WXMGRUBKernelOpenBSD openBSD)
    throws WXMException
  {
    Objects.requireNonNull(openBSD, "openBSD");

    final var bootDevice =
      openBSD.bootDevice();
    final var bootHD =
      deviceMap.searchForHD(bootDevice);
    final var bootCD =
      deviceMap.searchForCD(bootDevice);

    if (bootHD.isEmpty() && bootCD.isEmpty()) {
      throw new WXMExceptionNonexistent(
        this.errorNoSuchBootDevice(bootDevice)
      );
    }

    Invariants.checkInvariant(
      bootHD.isPresent() != bootCD.isPresent(),
      "A device ID must map to exactly one device"
    );

    final var configLines = new ArrayList<String>();

    bootHD.ifPresent(index -> {
      configLines.add(
        String.format(
          "kopenbsd -h com0 -r sd%da (hd%d)%s",
          index,
          index,
          openBSD.kernelPath()
        )
      );
    });

    bootCD.ifPresent(index -> {
      configLines.add(
        String.format(
          "kopenbsd -h com0 (cd%d)%s",
          index,
          openBSD.kernelPath()
        )
      );
    });

    configLines.add("boot");

    return WXMEvaluatedBootConfigurationGRUBBhyve.builder()
      .setDeviceMap(deviceMap.serialize())
      .setGrubConfiguration(configLines)
      .build();
  }

  public WXMEvaluatedBootConfigurationType evaluate()
    throws WXMException
  {
    final var configuration =
      Optional.ofNullable(this.machine.bootConfigurationMap().get(this.bootName))
        .orElseThrow(() -> new WXMExceptionNonexistent(
          this.errorNoSuchConfiguration()
        ));

    switch (configuration.kind()) {
      case GRUB_BHYVE:
        return this.evaluateGRUBConfiguration(
          (WXMBootConfigurationGRUBBhyve) configuration
        );
    }

    throw new UnreachableCodeException();
  }

  private String errorNoZFSVolumeConfigured()
  {
    return this.messages.format(
      "bootNoZFSVolumeConfigured",
      this.machine.id(),
      this.bootName.value(),
      this.bootConfigurationNames()
    );
  }

  private String errorNoSuchConfiguration()
  {
    return this.messages.format(
      "bootNoSuchConfiguration",
      this.machine.id(),
      this.bootName.value(),
      this.bootConfigurationNames()
    );
  }

  private String errorNoSuchBootDevice(
    final WXMDeviceID bootDevice)
  {
    return this.messages.format(
      "bootNoSuchDevice",
      this.machine.id(),
      this.bootName.value(),
      Integer.valueOf(bootDevice.value()),
      this.storageDeviceNames()
    );
  }

  private List<String> storageDeviceNames()
  {
    return this.machine.devices()
      .stream()
      .filter(device -> {
        switch (device.kind()) {
          case WXM_HOSTBRIDGE:
          case WXM_VIRTIO_NETWORK:
          case WXM_LPC:
            return false;
          case WXM_VIRTIO_BLOCK:
          case WXM_AHCI_HD:
          case WXM_AHCI_CD:
            return true;
        }
        throw new UnreachableCodeException();
      })
      .map(WXMDeviceType::id)
      .map(deviceID -> Integer.toString(deviceID.value()))
      .collect(Collectors.toList());
  }

  private List<String> bootConfigurationNames()
  {
    return this.machine.bootConfigurationMap()
      .keySet()
      .stream()
      .map(WXMBootConfigurationName::value)
      .collect(Collectors.toList());
  }

  private WXMEvaluatedBootConfigurationGRUBBhyve evaluateGRUBConfiguration(
    final WXMBootConfigurationGRUBBhyve configuration)
    throws WXMException
  {
    final var deviceMap =
      this.makeGRUBDeviceMap();
    final var kernel =
      configuration.kernelInstructions();

    switch (kernel.kind()) {
      case KERNEL_OPENBSD:
        return this.evaluateGRUBConfigurationOpenBSD(
          deviceMap,
          (WXMGRUBKernelOpenBSD) kernel
        );
      case KERNEL_LINUX:
        return this.evaluateGRUBConfigurationLinux(
          deviceMap,
          (WXMGRUBKernelLinux) kernel
        );
    }

    throw new UnreachableCodeException();
  }

  private WXMGRUBDeviceMap makeGRUBDeviceMap()
    throws WXMException
  {
    final var sortedDevices =
      this.machine.devices()
        .stream()
        .sorted(Comparator.comparing(WXMDeviceType::id))
        .collect(Collectors.toList());

    int hdIndex = 0;
    int cdIndex = 0;

    final var hds =
      new TreeMap<Integer, WXMGRUBDeviceAndPath>();
    final var cds =
      new TreeMap<Integer, WXMGRUBDeviceAndPath>();

    for (final var device : sortedDevices) {
      switch (device.kind()) {
        case WXM_HOSTBRIDGE:
        case WXM_VIRTIO_NETWORK:
        case WXM_LPC:
          break;
        case WXM_VIRTIO_BLOCK:
        case WXM_AHCI_HD:
          hds.put(
            Integer.valueOf(hdIndex),
            this.makeGRUBDeviceMapPath(hdIndex, device));
          ++hdIndex;
          break;
        case WXM_AHCI_CD:
          cds.put(
            Integer.valueOf(cdIndex),
            this.makeGRUBDeviceMapPath(cdIndex, device));
          ++cdIndex;
          break;
      }
    }

    return new WXMGRUBDeviceMap(hds, cds);
  }

  private WXMGRUBDeviceAndPath makeGRUBDeviceMapPath(
    final int index,
    final WXMDeviceType device)
    throws WXMException
  {
    switch (device.kind()) {
      case WXM_HOSTBRIDGE:
      case WXM_VIRTIO_NETWORK:
      case WXM_LPC:
        throw new UnreachableCodeException();

      case WXM_VIRTIO_BLOCK: {
        final var storage = (WXMDeviceVirtioBlockStorage) device;
        return this.makeGRUBDeviceMapPathStorageBackend(
          index,
          device,
          storage.backend()
        );
      }
      case WXM_AHCI_HD: {
        final var storage = (WXMDeviceAHCIDisk) device;
        return this.makeGRUBDeviceMapPathStorageBackend(
          index,
          device,
          storage.backend()
        );
      }
      case WXM_AHCI_CD: {
        final var storage = (WXMDeviceAHCIOpticalDisk) device;
        return this.makeGRUBDeviceMapPathStorageBackend(
          index,
          device,
          storage.backend()
        );
      }
    }

    throw new UnreachableCodeException();
  }

  private WXMGRUBDeviceAndPath makeGRUBDeviceMapPathStorageBackend(
    final int index,
    final WXMDeviceType device,
    final WXMStorageBackendType backend)
    throws WXMException
  {
    switch (backend.kind()) {
      case WXM_STORAGE_FILE:
        final var file = (WXMStorageBackendFile) backend;
        return new WXMGRUBDeviceAndPath(index, device, file.file());

      case WXM_STORAGE_ZFS_VOLUME:
        final var baseVolume =
          this.clientConfiguration.zfsVirtualMachineDirectory()
            .orElseThrow(() -> new WXMExceptionNonexistent(
              this.errorNoZFSVolumeConfigured()
            ));

        final Path path =
          WXMStorageBackends.determineZFSVolumePath(
            baseVolume,
            this.machine.id(),
            device.id()
          );
        return new WXMGRUBDeviceAndPath(index, device, path);

      case WXM_SCSI:
        throw new UnimplementedCodeException();
    }
    throw new UnreachableCodeException();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMBootConfigurationEvaluator 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
