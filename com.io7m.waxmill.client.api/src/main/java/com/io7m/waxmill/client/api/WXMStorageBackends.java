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

package com.io7m.waxmill.client.api;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Functions over storage backends.
 */

public final class WXMStorageBackends
{
  private WXMStorageBackends()
  {

  }

  /**
   * Derive the ZFS volume device path for the given machine and device IDs.
   *
   * @param machineId     The machine ID
   * @param configuration The client configuration
   * @param deviceID      The device ID
   *
   * @return The device path
   */

  public static Path zfsVolumePath(
    final WXMClientConfiguration configuration,
    final UUID machineId,
    final WXMDeviceID deviceID)
  {
    Objects.requireNonNull(configuration, "configuration");
    Objects.requireNonNull(machineId, "machineId");
    Objects.requireNonNull(deviceID, "deviceID");

    final var baseOpt = configuration.zfsVirtualMachineDirectory();
    if (baseOpt.isEmpty()) {
      throw new IllegalArgumentException(
        "A ZFS virtual machine directory must be specified in the client configuration"
      );
    }

    return baseOpt.get()
      .resolve(machineId.toString())
      .resolve(String.format("disk-%d", Integer.valueOf(deviceID.value())));
  }
}
