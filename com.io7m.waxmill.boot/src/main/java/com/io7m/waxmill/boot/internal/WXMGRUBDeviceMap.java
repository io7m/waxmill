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

package com.io7m.waxmill.boot.internal;

import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptionUnsatisfiedRequirement;
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootDiskAttachment;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackends;
import com.io7m.waxmill.machines.WXMVirtualMachine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class WXMGRUBDeviceMap
{
  private final SortedMap<Integer, WXMGRUBDeviceAndPath> disks;
  private final SortedMap<Integer, WXMGRUBDeviceAndPath> cds;
  private final Map<WXMDeviceSlot, WXMBootDiskAttachment> attachments;

  private WXMGRUBDeviceMap(
    final SortedMap<Integer, WXMGRUBDeviceAndPath> inDisks,
    final SortedMap<Integer, WXMGRUBDeviceAndPath> inCds,
    final Map<WXMDeviceSlot, WXMBootDiskAttachment> inAttachments)
  {
    this.disks =
      Objects.requireNonNull(inDisks, "disks");
    this.cds =
      Objects.requireNonNull(inCds, "cds");
    this.attachments =
      Map.copyOf(Objects.requireNonNull(inAttachments, "inAttachments"));
  }

  public static WXMGRUBDeviceMap create(
    final WXMBootMessages messages,
    final WXMClientConfiguration clientConfiguration,
    final WXMBootConfigurationGRUBBhyve configuration,
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
      new TreeMap<Integer, WXMGRUBDeviceAndPath>();
    final var cds =
      new TreeMap<Integer, WXMGRUBDeviceAndPath>();
    final Map<WXMDeviceSlot, WXMBootDiskAttachment> attachments =
      new HashMap<>();

    final var diskAttachments =
      configuration.diskAttachmentMap();
    final var requiredDevices =
      configuration.requiredDevices();

    for (final var device : sortedDevices) {
      final var deviceSlot = device.deviceSlot();

      switch (device.kind()) {
        case WXM_HOSTBRIDGE:
        case WXM_VIRTIO_NETWORK:
        case WXM_LPC:
          break;

        case WXM_VIRTIO_BLOCK:
        case WXM_AHCI_HD: {
          final var deviceAndPath =
            makeGRUBDeviceMapPath(
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
            makeGRUBDeviceMapPathStorageBackend(
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

    return new WXMGRUBDeviceMap(hds, cds, attachments);
  }

  private static WXMGRUBDeviceAndPath makeGRUBDeviceMapPath(
    final int index,
    final WXMDeviceType device,
    final WXMClientConfiguration clientConfiguration,
    final WXMVirtualMachine machine)
  {
    switch (device.kind()) {
      case WXM_HOSTBRIDGE:
      case WXM_VIRTIO_NETWORK:
      case WXM_LPC:
      case WXM_AHCI_CD:
        throw new UnreachableCodeException();

      case WXM_VIRTIO_BLOCK: {
        final var storage = (WXMDeviceVirtioBlockStorage) device;
        return makeGRUBDeviceMapPathStorageBackend(
          index,
          device,
          storage.backend(),
          clientConfiguration,
          machine
        );
      }
      case WXM_AHCI_HD: {
        final var storage = (WXMDeviceAHCIDisk) device;
        return makeGRUBDeviceMapPathStorageBackend(
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

  private static WXMGRUBDeviceAndPath makeGRUBDeviceMapPathStorageBackend(
    final int index,
    final WXMDeviceType device,
    final WXMDeviceType.WXMStorageBackendType backend,
    final WXMClientConfiguration clientConfiguration,
    final WXMVirtualMachine machine)
  {
    switch (backend.kind()) {
      case WXM_STORAGE_FILE:
        final var file = (WXMStorageBackendFile) backend;
        return new WXMGRUBDeviceAndPath(index, device, file.file());

      case WXM_STORAGE_ZFS_VOLUME:
        final Path path =
          WXMStorageBackends.determineZFSVolumePath(
            clientConfiguration.virtualMachineRuntimeDirectory(),
            machine.id(),
            device.deviceSlot()
          );
        return new WXMGRUBDeviceAndPath(index, device, path);

      case WXM_SCSI:
        throw new UnimplementedCodeException();
    }
    throw new UnreachableCodeException();
  }

  public Map<WXMDeviceSlot, WXMBootDiskAttachment> attachments()
  {
    return this.attachments;
  }

  public Optional<Integer> searchForCD(
    final WXMDeviceSlot deviceID)
  {
    return this.cds.values()
      .stream()
      .filter(dp -> Objects.equals(dp.device().deviceSlot(), deviceID))
      .map(dp -> Integer.valueOf(dp.index()))
      .findFirst();
  }

  public Optional<Integer> searchForHD(
    final WXMDeviceSlot deviceID)
  {
    return this.disks.values()
      .stream()
      .filter(dp -> Objects.equals(dp.device().deviceSlot(), deviceID))
      .map(dp -> Integer.valueOf(dp.index()))
      .findFirst();
  }

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

  public Iterable<Path> paths()
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
    return List.copyOf(paths);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMGRUBDeviceMap 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
