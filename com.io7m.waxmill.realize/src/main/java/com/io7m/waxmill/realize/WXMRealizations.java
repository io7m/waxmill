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

package com.io7m.waxmill.realize;

import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.realize.internal.WXMFileCheck;
import com.io7m.waxmill.realize.internal.WXMRealizeMessages;
import com.io7m.waxmill.realize.internal.WXMZFSVolumeCheck;

import java.util.Objects;

public final class WXMRealizations implements WXMRealizationType
{
  private final WXMProcessesType processes;
  private final WXMRealizeMessages messages;
  private final WXMClientConfiguration clientConfiguration;
  private final WXMVirtualMachine machine;

  private WXMRealizations(
    final WXMProcessesType inProcesses,
    final WXMRealizeMessages inMessages,
    final WXMClientConfiguration inClientConfiguration,
    final WXMVirtualMachine inMachine)
  {
    this.processes =
      Objects.requireNonNull(inProcesses, "inProcesses");
    this.messages =
      Objects.requireNonNull(inMessages, "inMessages");
    this.clientConfiguration =
      Objects.requireNonNull(inClientConfiguration, "clientConfiguration");
    this.machine =
      Objects.requireNonNull(inMachine, "machine");
  }

  public static WXMRealizationType create(
    final WXMProcessesType processes,
    final WXMClientConfiguration clientConfiguration,
    final WXMVirtualMachine machine)
  {
    return new WXMRealizations(
      processes,
      WXMRealizeMessages.create(),
      clientConfiguration,
      machine
    );
  }

  @Override
  public WXMRealizationInstructions evaluate()
  {
    final var builder = WXMRealizationInstructions.builder();

    for (final var device : this.machine.devices()) {
      switch (device.kind()) {
        case WXM_HOSTBRIDGE:
        case WXM_VIRTIO_NETWORK:
        case WXM_AHCI_CD:
        case WXM_LPC:
        case WXM_PASSTHRU:
          continue;
        case WXM_VIRTIO_BLOCK:
          this.evaluateVirtioBlock(
            builder,
            (WXMDeviceVirtioBlockStorage) device
          );
          continue;
        case WXM_AHCI_HD:
          this.evaluateAHCIDisk(
            builder,
            (WXMDeviceAHCIDisk) device
          );
          continue;
      }
    }

    return builder.build();
  }

  private void evaluateAHCIDisk(
    final WXMRealizationInstructions.Builder builder,
    final WXMDeviceAHCIDisk device)
  {
    final var backend = device.backend();
    switch (backend.kind()) {
      case WXM_STORAGE_FILE: {
        this.evaluateFile(builder, device, (WXMStorageBackendFile) backend);
        break;
      }
      case WXM_STORAGE_ZFS_VOLUME: {
        this.evaluateZFSVolume(
          builder, device, (WXMStorageBackendZFSVolume) backend);
        break;
      }
      case WXM_SCSI:
        throw new UnimplementedCodeException();
    }
  }

  private void evaluateFile(
    final WXMRealizationInstructions.Builder builder,
    final WXMDeviceType device,
    final WXMStorageBackendFile file)
  {
    builder.addSteps(
      new WXMFileCheck(
        this.messages,
        device.deviceSlot(),
        this.machine.id(),
        file.file())
    );
  }

  private void evaluateVirtioBlock(
    final WXMRealizationInstructions.Builder builder,
    final WXMDeviceVirtioBlockStorage device)
  {
    final var backend = device.backend();
    switch (backend.kind()) {
      case WXM_STORAGE_FILE: {
        this.evaluateFile(builder, device, (WXMStorageBackendFile) backend);
        break;
      }
      case WXM_STORAGE_ZFS_VOLUME: {
        this.evaluateZFSVolume(
          builder, device, (WXMStorageBackendZFSVolume) backend);
        break;
      }
      case WXM_SCSI:
        throw new UnimplementedCodeException();
    }
  }

  private void evaluateZFSVolume(
    final WXMRealizationInstructions.Builder builder,
    final WXMDeviceType device,
    final WXMStorageBackendZFSVolume zfs)
  {
    builder.addSteps(
      new WXMZFSVolumeCheck(
        this.clientConfiguration,
        this.messages,
        this.processes,
        this.machine.id(),
        device.deviceSlot(),
        zfs
      )
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMRealizations 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
