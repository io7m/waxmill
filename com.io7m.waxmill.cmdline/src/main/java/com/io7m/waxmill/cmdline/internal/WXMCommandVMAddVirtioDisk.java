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

package com.io7m.waxmill.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.waxmill.client.api.WXMClientType;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceSlots;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMMachineMessages;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.FAILURE;
import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.SUCCESS;
import static com.io7m.waxmill.cmdline.internal.WXMEnvironment.checkConfigurationPath;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendFileType.WXMOpenOption;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType;
import static com.io7m.waxmill.machines.WXMStorageBackends.determineZFSVolumePath;

@Parameters(commandDescription = "Add a virtio disk to a virtual machine.")
public final class WXMCommandVMAddVirtioDisk extends WXMCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddVirtioDisk.class);

  @Parameter(
    names = "--configuration",
    description = "The path to the configuration file (environment variable: $WAXMILL_CONFIGURATION_FILE)",
    required = false
  )
  private Path configurationFile = WXMEnvironment.configurationFile();

  @Parameter(
    names = "--id",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--comment",
    description = "A comment describing the new disk",
    required = false
  )
  private String comment = "";

  @Parameter(
    names = "--device-slot",
    description = "The slot to which the device will be attached.",
    required = true,
    converter = WXMDeviceSlotConverter.class
  )
  private WXMDeviceSlot deviceSlot;

  @Parameter(
    names = "--open-option",
    description = "The options that will be used when opening the storage device.",
    required = false
  )
  private List<WXMOpenOption> openOptions = List.of();

  @Parameter(
    names = "--backend",
    description = "A specification of the device backend to add "
      + "(such as 'file:/tmp/xyz' or 'zfs-volume')",
    required = true,
    converter = WXMStorageBackendConverter.class
  )
  private WXMStorageBackendType backend;

  public WXMCommandVMAddVirtioDisk()
  {

  }

  @Override
  public Status execute()
    throws Exception
  {
    if (super.execute() == FAILURE) {
      return FAILURE;
    }
    if (!checkConfigurationPath(LOG, this.configurationFile)) {
      return FAILURE;
    }

    try (var client = WXMServices.clients().open(this.configurationFile)) {
      final var machine = client.vmFind(this.id);
      this.deviceSlot =
        WXMDeviceSlots.checkDeviceSlotNotUsed(
          WXMMachineMessages.create(),
          machine,
          this.deviceSlot
        );

      switch (this.backend.kind()) {
        case WXM_STORAGE_FILE:
          this.backend =
            WXMStorageBackendFile.builder()
              .from(this.backend)
              .setOptions(this.openOptions)
              .setComment(this.comment)
              .build();
          break;
        case WXM_STORAGE_ZFS_VOLUME:
          this.backend =
            WXMStorageBackendZFSVolume.builder()
              .from(this.backend)
              .setComment(this.comment)
              .build();
          break;
        case WXM_SCSI:
          throw new UnimplementedCodeException();
      }

      final WXMDeviceType disk =
        WXMDeviceVirtioBlockStorage.builder()
          .setDeviceSlot(this.deviceSlot)
          .setBackend(this.backend)
          .setComment(Optional.ofNullable(this.comment).orElse(""))
          .build();

      final var updatedMachine =
        WXMVirtualMachine.builder()
          .from(machine)
          .addDevices(disk)
          .build();

      client.vmUpdate(updatedMachine);
      this.showCreated(client, machine);
    }
    return SUCCESS;
  }

  private void showCreated(
    final WXMClientType client,
    final WXMVirtualMachine machine)
  {
    switch (this.backend.kind()) {
      case WXM_STORAGE_FILE: {
        LOG.info(
          "Added virtio disk file {} @ slot {}",
          ((WXMStorageBackendFile) this.backend).file(),
          this.deviceSlot
        );
        break;
      }
      case WXM_STORAGE_ZFS_VOLUME: {
        LOG.info(
          "Added virtio disk zfs volume {} @ slot {}",
          showZFSPath(client, machine, this.deviceSlot),
          this.deviceSlot
        );
        break;
      }
      case WXM_SCSI: {
        break;
      }
    }
  }

  private static String showZFSPath(
    final WXMClientType client,
    final WXMVirtualMachine machine,
    final WXMDeviceSlot deviceId)
  {
    return determineZFSVolumePath(
      client.configuration().virtualMachineRuntimeDirectory(),
      machine.id(),
      deviceId
    ).toString();
  }
}
