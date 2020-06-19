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

import com.io7m.junreachable.UnreachableCodeException;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Functions over TTY backends.
 */

public final class WXMTTYBackends
{
  private WXMTTYBackends()
  {

  }

  /**
   * Derive the nmdm device path for the given machine ID and side.
   *
   * @param fileSystem The file system
   * @param machineId  The machine ID
   * @param side       The device side
   *
   * @return The device path
   */

  public static Path nmdmPath(
    final FileSystem fileSystem,
    final UUID machineId,
    final NMDMSide side)
  {
    Objects.requireNonNull(machineId, "machineId");
    Objects.requireNonNull(side, "side");

    switch (side) {
      case NMDM_HOST:
        return fileSystem.getPath(String.format("/dev/nmdm_%s_B", machineId));
      case NMDM_GUEST:
        return fileSystem.getPath(String.format("/dev/nmdm_%s_A", machineId));
    }
    throw new UnreachableCodeException();
  }

  /**
   * The "side" of an NMDM device. This is either the host-accessible side,
   * or the guest-accessible side.
   */

  public enum NMDMSide
  {
    /**
     * The host side.
     */

    NMDM_HOST,

    /**
     * The guest side.
     */

    NMDM_GUEST
  }
}
