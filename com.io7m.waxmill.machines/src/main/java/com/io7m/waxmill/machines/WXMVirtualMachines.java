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

package com.io7m.waxmill.machines;

import com.io7m.waxmill.exceptions.WXMExceptionDuplicate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Functions over virtual machines.
 */

public final class WXMVirtualMachines
{
  private WXMVirtualMachines()
  {

  }

  /**
   * Return the set of boot configurations that are using the given device.
   *
   * @param machine The machine
   * @param device  The device
   *
   * @return The boot configurations, if any
   */

  public static Set<WXMBootConfigurationType> bootConfigurationsUsingDevice(
    final WXMVirtualMachine machine,
    final WXMDeviceSlot device)
  {
    Objects.requireNonNull(machine, "machine");
    Objects.requireNonNull(device, "device");

    final var referencing = new HashSet<WXMBootConfigurationType>();
    final var bootConfigurations = machine.bootConfigurations();
    for (final var bootConfiguration : bootConfigurations) {
      if (bootConfiguration.requiredDevices().contains(device)) {
        referencing.add(bootConfiguration);
      }
    }
    return Set.copyOf(referencing);
  }

  /**
   * Update the virtual machine with the given device.
   *
   * @param messages The string resources
   * @param machine  The input machine
   * @param device   The device
   * @param replace  {@code true} if existing devices should be replaced
   *
   * @return A machine with the device added
   *
   * @throws WXMExceptionDuplicate If {@code replace} is {@code false} and a device already exists
   */

  public static WXMVirtualMachine updateWithDevice(
    final WXMMachineMessages messages,
    final WXMVirtualMachine machine,
    final WXMDeviceType device,
    final boolean replace)
    throws WXMExceptionDuplicate
  {
    Objects.requireNonNull(machine, "machine");
    Objects.requireNonNull(device, "device");

    final var deviceMap = new HashMap<>(machine.deviceMap());
    final var existing = deviceMap.get(device.deviceSlot());
    if (existing != null) {
      if (!replace) {
        throw new WXMExceptionDuplicate(
          messages.format(
            "errorDeviceSlotAlreadyUsed",
            machine.id(),
            existing.deviceSlot(),
            existing.kind()
          )
        );
      }
    }

    deviceMap.put(device.deviceSlot(), device);
    return WXMVirtualMachine.builder()
      .from(machine)
      .setDevices(deviceMap.values())
      .build();
  }
}
