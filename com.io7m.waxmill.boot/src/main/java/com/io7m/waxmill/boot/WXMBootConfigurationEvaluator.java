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
import com.io7m.waxmill.boot.internal.WXMBootMessages;
import com.io7m.waxmill.boot.internal.WXMGRUBDeviceAndPath;
import com.io7m.waxmill.boot.internal.WXMGRUBDeviceMap;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMCommandExecution;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceID;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMDeviceVirtioNetwork;
import com.io7m.waxmill.machines.WXMEvaluatedBootCommands;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMException;
import com.io7m.waxmill.machines.WXMExceptionNonexistent;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackends;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
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
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMTTYBackendType;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMVirtioNetworkBackendType;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendFileType.WXMOpenOption;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType;
import static com.io7m.waxmill.machines.WXMTTYBackends.NMDMSide.NMDM_HOST;
import static com.io7m.waxmill.machines.WXMTTYBackends.nmdmPath;

/**
 * Functions over boot configurations.
 */

public final class WXMBootConfigurationEvaluator
{
  private final WXMClientConfiguration clientConfiguration;
  private final WXMBootMessages messages;
  private final WXMVirtualMachine machine;
  private final WXMBootConfigurationName bootName;

  private WXMBootConfigurationEvaluator(
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

  /**
   * Construct an evaluator.
   *
   * @param inClientConfiguration The client configuration
   * @param inMachine             The virtual machine to be booted
   * @param inName                The name of the boot configuration
   */

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

  private static void configureBhyveFlag(
    final WXMCommandExecution.Builder commandBuilder,
    final boolean enabled,
    final String flagName)
  {
    if (enabled) {
      commandBuilder.addArguments(flagName);
    }
  }

  private ArrayList<String> generateGRUBConfigLinesLinux(
    final WXMGRUBDeviceMap deviceMap,
    final WXMGRUBKernelLinux linux)
    throws WXMExceptionNonexistent
  {
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
    return configLines;
  }

  private ArrayList<String> generateGRUBConfigLinesOpenBSD(
    final WXMGRUBDeviceMap deviceMap,
    final WXMGRUBKernelOpenBSD openBSD)
    throws WXMExceptionNonexistent
  {
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
    return configLines;
  }

  private WXMEvaluatedBootCommands generateGRUBBhyveCommands()
  {
    final WXMCommandExecution grubBhyveCommand =
      this.generateGRUBBhyveCommand();
    final WXMCommandExecution bhyve =
      this.generateBhyveCommand();

    return WXMEvaluatedBootCommands.builder()
      .addConfigurationCommands(grubBhyveCommand)
      .setLastExecution(bhyve)
      .build();
  }

  private WXMCommandExecution generateGRUBBhyveCommand()
  {
    final var machineId =
      this.machine.id();
    final var basePath =
      this.clientConfiguration.virtualMachineRuntimeDirectoryFor(machineId);
    final var deviceMapPath =
      basePath.resolve("grub-device.map");

    final var memoryMB =
      this.machine.memory()
        .totalMegabytes();

    return WXMCommandExecution.builder()
      .setExecutable(this.clientConfiguration.grubBhyveExecutable())
      .addArguments(String.format("--device-map=%s", deviceMapPath))
      .addArguments("--root=host")
      .addArguments(String.format("--directory=%s", basePath))
      .addArguments(String.format("--memory=%sM", memoryMB))
      .addArguments(machineId.toString())
      .build();
  }

  private WXMCommandExecution generateBhyveCommand()
  {
    final var commandBuilder =
      WXMCommandExecution.builder()
        .setExecutable(this.clientConfiguration.bhyveExecutable());

    this.configureBhyveFlags(commandBuilder);
    this.configureBhyveCPUTopology(commandBuilder);
    this.configureBhyveMemory(commandBuilder);

    final var devicesSorted =
      this.machine.devices()
        .stream()
        .sorted(Comparator.comparing(WXMDeviceType::id))
        .collect(Collectors.toList());

    for (final var device : devicesSorted) {
      this.configureBhyveDevice(commandBuilder, device);
    }

    commandBuilder.addArguments(this.machine.id().toString());
    return commandBuilder.build();
  }

  private void configureBhyveDevice(
    final WXMCommandExecution.Builder command,
    final WXMDeviceType device)
  {
    switch (device.kind()) {
      case WXM_HOSTBRIDGE: {
        configureBhyveDeviceHostBridge(command, (WXMDeviceHostBridge) device);
        return;
      }
      case WXM_VIRTIO_NETWORK: {
        configureBhyveDeviceVirtioNetwork(
          command, (WXMDeviceVirtioNetwork) device);
        return;
      }
      case WXM_VIRTIO_BLOCK: {
        this.configureBhyveDeviceVirtioBlock(
          command, (WXMDeviceVirtioBlockStorage) device);
        return;
      }
      case WXM_AHCI_HD: {
        this.configureBhyveDeviceAHCIHD(command, (WXMDeviceAHCIDisk) device);
        return;
      }
      case WXM_AHCI_CD: {
        this.configureBhyveDeviceAHCICD(
          command,
          (WXMDeviceAHCIOpticalDisk) device);
        return;
      }
      case WXM_LPC: {
        this.configureBhyveDeviceLPC(command, (WXMDeviceLPC) device);
        return;
      }
    }
    throw new UnreachableCodeException();
  }

  private void configureBhyveDeviceLPC(
    final WXMCommandExecution.Builder command,
    final WXMDeviceLPC device)
  {
    command.addArguments("-s");
    command.addArguments(
      String.format(
        "%d,%s",
        Integer.valueOf(device.id().value()),
        device.externalName())
    );

    for (final var backend : device.backends()) {
      this.configureBhyveDeviceLPCBackend(command, backend);
    }
  }

  private void configureBhyveDeviceLPCBackend(
    final WXMCommandExecution.Builder command,
    final WXMTTYBackendType backend)
  {
    command.addArguments("-l");

    switch (backend.kind()) {
      case WXM_FILE:
        final WXMTTYBackendFile file = (WXMTTYBackendFile) backend;
        command.addArguments(String.format(
          "%s,%s",
          file.device(),
          file.path()));
        break;

      case WXM_NMDM:
        final WXMTTYBackendNMDM nmdm = (WXMTTYBackendNMDM) backend;
        command.addArguments(
          String.format(
            "%s,%s",
            nmdm.device(),
            nmdmPath(
              this.clientConfiguration.virtualMachineRuntimeDirectory()
                .getFileSystem(),
              this.machine.id(),
              NMDM_HOST
            )
          ));
        break;

      case WXM_STDIO:
        final WXMTTYBackendStdio stdio = (WXMTTYBackendStdio) backend;
        command.addArguments(String.format("%s,stdio", stdio.device()));
        break;
    }
  }

  private static void configureBhyveDeviceVirtioNetwork(
    final WXMCommandExecution.Builder command,
    final WXMDeviceVirtioNetwork device)
  {
    final var id = Integer.valueOf(device.id().value());
    command.addArguments("-s");
    command.addArguments(String.format(
      "%d,%s,%s",
      id,
      device.externalName(),
      configureBhyveNetworkBackend(device.backend())
    ));
  }

  private static String configureBhyveNetworkBackend(
    final WXMVirtioNetworkBackendType backend)
  {
    switch (backend.kind()) {
      case WXM_TAP:
        return configureBhyveNetworkTAP((WXMTap) backend);
      case WXM_VMNET:
        return configureBhyveNetworkVMNet((WXMVMNet) backend);
    }
    throw new UnreachableCodeException();
  }

  private static String configureBhyveNetworkVMNet(
    final WXMVMNet backend)
  {
    return String.format(
      "%s,mac=%s",
      backend.name().value(),
      backend.address().value()
    );
  }

  private static String configureBhyveNetworkTAP(
    final WXMTap backend)
  {
    return String.format(
      "%s,mac=%s",
      backend.name().value(),
      backend.address().value()
    );
  }

  private void configureBhyveDeviceAHCICD(
    final WXMCommandExecution.Builder command,
    final WXMDeviceAHCIOpticalDisk device)
  {
    final var id = Integer.valueOf(device.id().value());
    command.addArguments("-s");
    command.addArguments(String.format(
      "%d,%s,%s",
      id,
      device.externalName(),
      this.configureBhyveDeviceStorageBackend(device, device.backend())
    ));
  }

  private void configureBhyveDeviceVirtioBlock(
    final WXMCommandExecution.Builder command,
    final WXMDeviceVirtioBlockStorage device)
  {
    final var id = Integer.valueOf(device.id().value());
    command.addArguments("-s");
    command.addArguments(String.format(
      "%d,%s,%s",
      id,
      device.externalName(),
      this.configureBhyveDeviceStorageBackend(device, device.backend())
    ));
  }

  private void configureBhyveDeviceAHCIHD(
    final WXMCommandExecution.Builder command,
    final WXMDeviceAHCIDisk device)
  {
    final var id = Integer.valueOf(device.id().value());
    command.addArguments("-s");
    command.addArguments(String.format(
      "%d,%s,%s",
      id,
      device.externalName(),
      this.configureBhyveDeviceStorageBackend(device, device.backend())
    ));
  }

  private String configureBhyveDeviceStorageBackend(
    final WXMDeviceType device,
    final WXMStorageBackendType backend)
  {
    switch (backend.kind()) {
      case WXM_STORAGE_FILE:
        return configureBhyveStorageFile((WXMStorageBackendFile) backend);
      case WXM_STORAGE_ZFS_VOLUME:
        return this.configureBhyveStorageZFS(
          device);
      case WXM_SCSI:
        throw new UnimplementedCodeException();
    }

    throw new UnreachableCodeException();
  }

  private String configureBhyveStorageZFS(
    final WXMDeviceType device)
  {
    return WXMStorageBackends.determineZFSVolumePath(
      this.clientConfiguration.virtualMachineRuntimeDirectory(),
      this.machine.id(),
      device.id())
      .toString();
  }

  private static String configureBhyveStorageFile(
    final WXMStorageBackendFile backend)
  {
    final var builder = new StringBuilder(backend.file().toString());
    final var options = backend.options();
    if (!options.isEmpty()) {
      builder.append(
        options.stream()
          .map(WXMOpenOption::externalName)
          .collect(Collectors.joining(",", ",", ""))
      );
    }

    backend.sectorSizes().ifPresent(sizes -> {
      if (!options.isEmpty()) {
        builder.append(',');
      }
      builder.append("sectorsize=");
      builder.append(sizes.logical());
      builder.append('/');
      builder.append(sizes.physical());
    });
    return builder.toString();
  }

  private static void configureBhyveDeviceHostBridge(
    final WXMCommandExecution.Builder command,
    final WXMDeviceHostBridge device)
  {
    final var id = Integer.valueOf(device.id().value());
    switch (device.vendor()) {
      case WXM_UNSPECIFIED:
        command.addArguments("-s");
        command.addArguments(String.format("%d,%s", id, "hostbridge"));
        return;
      case WXM_AMD:
        command.addArguments("-s");
        command.addArguments(String.format("%d,%s", id, "amd_hostbridge"));
        return;
    }
    throw new UnreachableCodeException();
  }

  private void configureBhyveMemory(
    final WXMCommandExecution.Builder command)
  {
    final var memoryMB = this.machine.memory().totalMegabytes();
    command.addArguments("-m");
    command.addArguments(String.format("%sM", memoryMB));
  }

  private void configureBhyveCPUTopology(
    final WXMCommandExecution.Builder command)
  {
    command.addArguments("-c");
    final var topology = this.machine.cpuTopology();
    command.addArguments(
      String.format(
        "cpus=%d,sockets=%d,cores=%d,threads=%d",
        Integer.valueOf(topology.cpus()),
        Integer.valueOf(topology.sockets()),
        Integer.valueOf(topology.cores()),
        Integer.valueOf(topology.threads())
      ));
  }

  private void configureBhyveFlags(
    final WXMCommandExecution.Builder cmd)
  {
    final var flags = this.machine.flags();
    configureBhyveFlag(cmd, flags.disableMPTableGeneration(), "-Y");
    configureBhyveFlag(cmd, flags.exitOnPAUSE(), "-P");
    configureBhyveFlag(cmd, flags.forceVirtualIOPCIToUseMSI(), "-W");
    configureBhyveFlag(cmd, flags.generateACPITables(), "-A");
    configureBhyveFlag(cmd, flags.guestAPICIsX2APIC(), "-x");
    configureBhyveFlag(cmd, flags.includeGuestMemoryInCoreFiles(), "-C");
    configureBhyveFlag(cmd, flags.realTimeClockIsUTC(), "-u");
    configureBhyveFlag(cmd, flags.wireGuestMemory(), "-S");
    configureBhyveFlag(cmd, flags.yieldCPUOnHLT(), "-H");
  }

  private WXMEvaluatedBootConfigurationGRUBBhyve evaluateGRUBConfigurationOpenBSD(
    final WXMGRUBDeviceMap deviceMap,
    final WXMGRUBKernelOpenBSD openBSD)
    throws WXMException
  {
    Objects.requireNonNull(openBSD, "openBSD");

    final var configLines =
      this.generateGRUBConfigLinesOpenBSD(deviceMap, openBSD);

    return WXMEvaluatedBootConfigurationGRUBBhyve.builder()
      .setCommands(this.generateGRUBBhyveCommands())
      .setDeviceMap(deviceMap.serialize())
      .setGrubConfiguration(configLines)
      .build();
  }

  private WXMEvaluatedBootConfigurationGRUBBhyve evaluateGRUBConfigurationLinux(
    final WXMGRUBDeviceMap deviceMap,
    final WXMGRUBKernelLinux linux)
    throws WXMExceptionNonexistent
  {
    Objects.requireNonNull(linux, "linux");

    final var configLines =
      this.generateGRUBConfigLinesLinux(deviceMap, linux);

    return WXMEvaluatedBootConfigurationGRUBBhyve.builder()
      .setCommands(this.generateGRUBBhyveCommands())
      .setDeviceMap(deviceMap.serialize())
      .setGrubConfiguration(configLines)
      .build();
  }

  /**
   * Evaluate the specified boot configuration, producing a set of commands
   * that can be used to boot the virtual machine.
   *
   * @return A set of boot commands
   *
   * @throws WXMException On errors
   */

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
  {
    switch (backend.kind()) {
      case WXM_STORAGE_FILE:
        final var file = (WXMStorageBackendFile) backend;
        return new WXMGRUBDeviceAndPath(index, device, file.file());

      case WXM_STORAGE_ZFS_VOLUME:
        final Path path =
          WXMStorageBackends.determineZFSVolumePath(
            this.clientConfiguration.virtualMachineRuntimeDirectory(),
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
