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
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.waxmill.client.api.WXMClientType;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceSlots;
import com.io7m.waxmill.machines.WXMDeviceType;
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

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendFileType.WXMOpenOption;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType;
import static com.io7m.waxmill.machines.WXMStorageBackends.determineZFSVolumePath;

@Parameters(commandDescription = "Add an AHCI disk to a virtual machine.")
public final class WXMCommandVMAddAHCIDisk extends
  WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddAHCIDisk.class);

  @Parameter(
    names = "--machine",
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
    names = "--optical",
    description = "The disk should be an optical disk (a CD drive)",
    required = false
  )
  private boolean optical;

  @Parameter(
    names = "--backend",
    description = "A specification of the AHCI storage device backend to add",
    required = true,
    converter = WXMStorageBackendConverter.class
  )
  private WXMStorageBackendType backend;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMAddAHCIDisk(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-add-ahci-disk";
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("storageBackendSpec");
  }

  private void showCreated(
    final WXMClientType client,
    final WXMVirtualMachine machine)
  {
    switch (this.backend.kind()) {
      case WXM_STORAGE_FILE: {
        this.info(
          "infoAddedDiskFile",
          this.optical ? "AHCI optical" : "AHCI",
          ((WXMStorageBackendFile) this.backend).file(),
          this.deviceSlot
        );
        break;
      }
      case WXM_STORAGE_ZFS_VOLUME: {
        this.info(
          "infoAddedDiskZFS",
          this.optical ? "AHCI optical" : "AHCI",
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

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
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

      final WXMDeviceType disk;
      if (this.optical) {
        disk = WXMDeviceAHCIOpticalDisk.builder()
          .setDeviceSlot(this.deviceSlot)
          .setBackend(this.backend)
          .setComment(Optional.ofNullable(this.comment).orElse(""))
          .build();
      } else {
        disk = WXMDeviceAHCIDisk.builder()
          .setDeviceSlot(this.deviceSlot)
          .setBackend(this.backend)
          .setComment(Optional.ofNullable(this.comment).orElse(""))
          .build();
      }

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
}
