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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_LPC;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMLPCTTYNames.WXM_COM1;

/**
 * Functions over consoles.
 */

public final class WXMConsoles
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMConsoles.class);

  private WXMConsoles()
  {

  }

  /**
   * Find the default console for the given machine.
   *
   * @param machine The machine
   *
   * @return The default console, if exactly one exists
   */

  public static Optional<WXMDeviceType> findDefaultConsole(
    final WXMVirtualMachine machine)
  {
    Objects.requireNonNull(machine, "machine");

    final var consoleDevices =
      machine.devices()
        .stream()
        .filter(dev -> dev.kind() == WXM_LPC)
        .map(WXMDeviceLPC.class::cast)
        .filter(lpc -> lpc.backendMap().containsKey(WXM_COM1.deviceName()))
        .collect(Collectors.toList());

    final var deviceCount = consoleDevices.size();
    LOG.debug(
      "found {} console devices in machine {}",
      Integer.valueOf(deviceCount),
      machine.id()
    );

    if (deviceCount == 1) {
      return Optional.of(consoleDevices.get(0));
    }
    return Optional.empty();
  }
}
