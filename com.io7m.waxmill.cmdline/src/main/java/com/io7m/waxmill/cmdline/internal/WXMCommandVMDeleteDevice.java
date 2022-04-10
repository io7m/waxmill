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

package com.io7m.waxmill.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-delete-devices" command.
 */

@Parameters(commandDescription = "Delete devices from virtual machines.")
public final class WXMCommandVMDeleteDevice
  extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMDeleteDevice.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--device-slot",
    description = "The devices to remove",
    converter = WXMDeviceSlotConverter.class
  )
  private List<WXMDeviceSlot> deviceSlots = new ArrayList<>();

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMDeleteDevice(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-delete-devices";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);

      final var machineBuilder =
        WXMVirtualMachine.builder()
          .from(machine);

      var failed = false;

      final var currentDevices =
        new HashMap<>(machine.deviceMap());

      for (final var deviceSlot : this.deviceSlots) {
        if (!currentDevices.containsKey(deviceSlot)) {
          this.error("errorDeviceNonexistent", deviceSlot);
          failed = true;
          continue;
        }

        final var referencing =
          WXMVirtualMachines.bootConfigurationsUsingDevice(
            machine,
            deviceSlot
          );

        if (!referencing.isEmpty()) {
          this.error(
            "errorDeviceUsedByBootConfigurations",
            deviceSlot,
            referencing.stream()
              .map(WXMBootConfigurationType::name)
              .map(WXMBootConfigurationName::value)
              .collect(Collectors.toList()));
          failed = true;
          continue;
        }

        currentDevices.remove(deviceSlot);
      }

      if (failed) {
        return FAILURE;
      }

      machineBuilder.setDevices(currentDevices.values());
      client.vmUpdate(machineBuilder.build());
    }

    for (final var deviceSlot : this.deviceSlots) {
      this.info("infoDeviceDeleted", deviceSlot);
    }
    return SUCCESS;
  }
}
