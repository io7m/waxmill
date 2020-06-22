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
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceID;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMException;
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

@Parameters(commandDescription = "Add an AHCI disk to a virtual machine.")
public final class WXMCommandVMAddAHCIDisk extends WXMCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddAHCIDisk.class);

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
    description = "A specification of the device backend to add "
      + "(such as 'file:/tmp/xyz' or 'zfs-volume')",
    required = true,
    converter = WXMStorageBackendConverter.class
  )
  private WXMStorageBackendType backend;

  public WXMCommandVMAddAHCIDisk()
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
      final var deviceId =
        machine.findUnusedDeviceId()
          .orElseThrow(() -> new WXMException(
            "No slots left to add a device to the virtual machine"
          ));

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
          .setId(deviceId)
          .setBackend(this.backend)
          .setComment(Optional.ofNullable(this.comment).orElse(""))
          .build();
      } else {
        disk = WXMDeviceAHCIDisk.builder()
          .setId(deviceId)
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
      this.showCreated(client, machine, deviceId);
    }
    return SUCCESS;
  }

  private void showCreated(
    final WXMClientType client,
    final WXMVirtualMachine machine,
    final WXMDeviceID deviceId)
  {
    switch (this.backend.kind()) {
      case WXM_STORAGE_FILE: {
        LOG.info(
          "Added {} disk file {} @ slot {}",
          this.optical ? "AHCI optical" : "AHCI",
          ((WXMStorageBackendFile) this.backend).file(),
          Integer.valueOf(deviceId.value())
        );
        break;
      }
      case WXM_STORAGE_ZFS_VOLUME: {
        LOG.info(
          "Added {} disk zfs volume {} @ slot {}",
          this.optical ? "AHCI optical" : "AHCI",
          showZFSPath(client, machine, deviceId),
          Integer.valueOf(deviceId.value())
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
    final WXMDeviceID deviceId)
  {
    return determineZFSVolumePath(
      client.configuration().virtualMachineRuntimeDirectory(),
      machine.id(),
      deviceId
    ).toString();
  }
}
