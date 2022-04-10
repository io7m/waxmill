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

package com.io7m.waxmill.machines;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Functions over TAP device names.
 */

public final class WXMTAPDeviceNames
{
  private static final Pattern VALID_NAME =
    Pattern.compile("tap[a-z_0-9]{1,13}");

  private WXMTAPDeviceNames()
  {

  }

  /**
   * @param name The raw TAP device name string
   *
   * @return {@code true} if the given string represents a valid TAP device name
   */

  public static boolean isValid(
    final String name)
  {
    return VALID_NAME.matcher(
      Objects.requireNonNull(name, "name")
    ).matches();
  }

  /**
   * Check that a string represents a valid TAP device name.
   *
   * @param name The raw TAP device name string
   *
   * @return {@code name}
   *
   * @throws IllegalArgumentException If the string is not a TAP device name
   */

  public static String checkValid(
    final String name)
  {
    if (isValid(name)) {
      return name;
    }
    throw new IllegalArgumentException(
      String.format("Invalid name '%s': Must match %s", name, VALID_NAME)
    );
  }
}
