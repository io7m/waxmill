/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.waxmill.boot.internal;

import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptionUnsatisfiedRequirement;
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMBootDiskAttachment;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceSlots;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMZFSFilesystems;
import com.io7m.waxmill.machines.WXMZFSVolumes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMTTYBackends.NMDMSide.NMDM_GUEST;
import static com.io7m.waxmill.machines.WXMTTYBackends.nmdmPath;

/**
 * A device map used for GRUB.
 */

public final class WXMDeviceMap
{
  private final SortedMap<Integer, WXMDeviceAndPath> disks;
  private final SortedMap<Integer, WXMDeviceAndPath> cds;
  private final Map<WXMDeviceSlot, WXMBootDiskAttachment> attachments;
  private final SortedSet<Path> others;
  private final TreeSet<Path> nmdmPaths;

  private WXMDeviceMap(
    final SortedMap<Integer, WXMDeviceAndPath> inDisks,
    final SortedMap<Integer, WXMDeviceAndPath> inCds,
    final Map<WXMDeviceSlot, WXMBootDiskAttachment> inAttachments,
    final TreeSet<Path> inOthers,
    final TreeSet<Path> inNmdmPaths)
  {
    this.disks =
      Objects.requireNonNull(inDisks, "disks");
    this.cds =
      Objects.requireNonNull(inCds, "cds");
    this.attachments =
      Map.copyOf(Objects.requireNonNull(inAttachments, "inAttachments"));
    this.others =
      Objects.requireNonNull(inOthers, "others");
    this.nmdmPaths =
      Objects.requireNonNull(inNmdmPaths, "nmdmPaths");
  }

  /**
   * Create a device map.
   *
   * @param messages            The string resources
   * @param clientConfiguration The client configuration
   * @param configuration       The boot configuration
   * @param machine             The virtual machine
   *
   * @return A new device map
   *
   * @throws WXMException On errors
   */

  public static WXMDeviceMap create(
    final WXMBootMessages messages,
    final WXMClientConfiguration clientConfiguration,
    final WXMBootConfigurationType configuration,
    final WXMVirtualMachine machine)
    throws WXMException
  {
    Objects.requireNonNull(messages, "messages");
    Objects.requireNonNull(clientConfiguration, "clientConfiguration");
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(machine, "machine");

    final var sortedDevices =
      machine.devices()
        .stream()
        .sorted(Comparator.comparing(WXMDeviceType::deviceSlot))
        .collect(Collectors.toList());

    int hdIndex = 0;
    int cdIndex = 0;

    final var hds =
      new TreeMap<Integer, WXMDeviceAndPath>();
    final var cds =
      new TreeMap<Integer, WXMDeviceAndPath>();
    final Map<WXMDeviceSlot, WXMBootDiskAttachment> attachments =
      new HashMap<>();
    final var others =
      new TreeSet<Path>();
    final var nmdmPaths =
      new TreeSet<Path>();

    final var diskAttachments =
      configuration.diskAttachmentMap();
    final var requiredDevices =
      configuration.requiredDevices();

    for (final var device : sortedDevices) {
      final var deviceSlot = device.deviceSlot();

      switch (device.kind()) {
        case WXM_HOSTBRIDGE:
        case WXM_VIRTIO_NETWORK:
        case WXM_PASSTHRU:
        case WXM_FRAMEBUFFER:
        case WXM_E1000:
          break;

        case WXM_LPC:
          processLPC(
            clientConfiguration,
            machine,
            (WXMDeviceLPC) device,
            others,
            nmdmPaths
          );
          break;

        case WXM_VIRTIO_BLOCK:
        case WXM_AHCI_HD: {
          final var deviceAndPath =
            makeDeviceMapPath(
              hdIndex,
              device,
              clientConfiguration,
              machine);
          hds.put(Integer.valueOf(hdIndex), deviceAndPath);
          ++hdIndex;
          break;
        }

        case WXM_AHCI_CD: {
          final var attachment = diskAttachments.get(deviceSlot);
          if (attachment == null) {
            if (requiredDevices.contains(deviceSlot)) {
              throw new WXMExceptionUnsatisfiedRequirement(
                messages.format(
                  "bootDeviceMissingAttachment",
                  machine.id(),
                  deviceSlot
                )
              );
            }
            break;
          }

          attachments.put(deviceSlot, attachment);

          final var backend =
            attachment.backend();
          final var deviceAndPath =
            makeDeviceMapPathStorageBackend(
              cdIndex,
              device,
              backend,
              clientConfiguration,
              machine);

          cds.put(Integer.valueOf(cdIndex), deviceAndPath);
          ++cdIndex;
          break;
        }
      }
    }

    return new WXMDeviceMap(hds, cds, attachments, others, nmdmPaths);
  }

  private static void processLPC(
    final WXMClientConfiguration clientConfiguration,
    final WXMVirtualMachine machine,
    final WXMDeviceLPC device,
    final SortedSet<Path> others,
    final SortedSet<Path> nmdmPaths)
  {
    for (final var backend : device.backends()) {
      final var kind = backend.kind();
      switch (kind) {
        case WXM_FILE:
          final var file = (WXMTTYBackendFile) backend;
          others.add(file.path());
          break;
        case WXM_NMDM:
          final Path path = nmdmPath(
            clientConfiguration.virtualMachineRuntimeFilesystem()
              .mountPoint()
              .getFileSystem(),
            machine.id(),
            NMDM_GUEST
          );
          others.add(path);
          nmdmPaths.add(path);
          break;
        case WXM_STDIO:
          break;
      }
    }
  }

  private static WXMDeviceAndPath makeDeviceMapPath(
    final int index,
    final WXMDeviceType device,
    final WXMClientConfiguration clientConfiguration,
    final WXMVirtualMachine machine)
  {
    switch (device.kind()) {
      case WXM_AHCI_CD:
      case WXM_E1000:
      case WXM_FRAMEBUFFER:
      case WXM_HOSTBRIDGE:
      case WXM_LPC:
      case WXM_PASSTHRU:
      case WXM_VIRTIO_NETWORK:
        throw new UnreachableCodeException();

      case WXM_VIRTIO_BLOCK: {
        final var storage = (WXMDeviceVirtioBlockStorage) device;
        return makeDeviceMapPathStorageBackend(
          index,
          device,
          storage.backend(),
          clientConfiguration,
          machine
        );
      }
      case WXM_AHCI_HD: {
        final var storage = (WXMDeviceAHCIDisk) device;
        return makeDeviceMapPathStorageBackend(
          index,
          device,
          storage.backend(),
          clientConfiguration,
          machine
        );
      }
    }

    throw new UnreachableCodeException();
  }

  private static WXMDeviceAndPath makeDeviceMapPathStorageBackend(
    final int index,
    final WXMDeviceType device,
    final WXMDeviceType.WXMStorageBackendType backend,
    final WXMClientConfiguration clientConfiguration,
    final WXMVirtualMachine machine)
  {
    switch (backend.kind()) {
      case WXM_STORAGE_FILE: {
        final var file = (WXMStorageBackendFile) backend;
        return new WXMDeviceAndPath(index, device, file.file());
      }
      case WXM_STORAGE_ZFS_VOLUME: {
        final var volume =
          WXMZFSVolumes.resolve(
            WXMZFSFilesystems.resolve(
              clientConfiguration.virtualMachineRuntimeFilesystem(),
              machine.id().toString()
            ),
            WXMDeviceSlots.asDiskName(device.deviceSlot())
          );
        return new WXMDeviceAndPath(index, device, volume.device());
      }
      case WXM_SCSI:
        throw new UnimplementedCodeException();
    }
    throw new UnreachableCodeException();
  }

  /**
   * @return The boot disk attachments
   */

  public Map<WXMDeviceSlot, WXMBootDiskAttachment> attachments()
  {
    return this.attachments;
  }

  /**
   * Search for the CD device.
   *
   * @param deviceID The device slot
   *
   * @return The CD device, if any
   */

  public Optional<Integer> searchForCD(
    final WXMDeviceSlot deviceID)
  {
    return this.cds.values()
      .stream()
      .filter(dp -> Objects.equals(dp.device().deviceSlot(), deviceID))
      .map(dp -> Integer.valueOf(dp.index()))
      .findFirst();
  }

  /**
   * Search for the HD device.
   *
   * @param deviceID The device slot
   *
   * @return The HD device, if any
   */

  public Optional<Integer> searchForHD(
    final WXMDeviceSlot deviceID)
  {
    return this.disks.values()
      .stream()
      .filter(dp -> Objects.equals(dp.device().deviceSlot(), deviceID))
      .map(dp -> Integer.valueOf(dp.index()))
      .findFirst();
  }

  /**
   * Serialize the device map.
   *
   * @return The list of device map lines
   */

  public Iterable<String> serialize()
  {
    final var lines =
      new ArrayList<String>(this.disks.size() + this.cds.size());

    for (final var entry : this.disks.entrySet()) {
      final var hd = entry.getValue();
      lines.add(String.format("(hd%d) %s", entry.getKey(), hd.path()));
    }
    for (final var entry : this.cds.entrySet()) {
      final var cd = entry.getValue();
      lines.add(String.format("(cd%d) %s", entry.getKey(), cd.path()));
    }
    return List.copyOf(lines);
  }

  /**
   * @return The set of device paths
   */

  public Collection<Path> paths()
  {
    final var paths =
      new ArrayList<Path>(this.disks.size() + this.cds.size());

    for (final var entry : this.disks.entrySet()) {
      final var hd = entry.getValue();
      paths.add(hd.path());
    }
    for (final var entry : this.cds.entrySet()) {
      final var cd = entry.getValue();
      paths.add(cd.path());
    }

    paths.addAll(this.others);
    return List.copyOf(paths);
  }

  /**
   * @return The set of nmdm paths
   */

  public Collection<Path> nmdmPaths()
  {
    return List.copyOf(this.nmdmPaths);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMDeviceMap 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
