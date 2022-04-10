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

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.nio.ByteOrder.BIG_ENDIAN;

/**
 * Functions to encode UUIDs using a shortened encoding that can be used
 * with Bhyve's virtual machine name limitations. Essentially, Bhyve limits
 * names to at most 31 characters on modern FreeBSD versions.
 */

public final class WXMShortIDs
{
  private static final Base64.Encoder ENCODER =
    Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER =
    Base64.getUrlDecoder();

  /**
   * The pattern that defines a valid short ID.
   */

  public static final Pattern SHORT_ID_PATTERN =
    Pattern.compile("[0-9a-zA-Z_\\-]{22}");

  private WXMShortIDs()
  {

  }

  /**
   * Produce a shortened encoding of the given UUID.
   *
   * @param uuid The uuid
   *
   * @return An encoded UUID
   */

  public static String encode(
    final UUID uuid)
  {
    Objects.requireNonNull(uuid, "uuid");

    final var buffer = ByteBuffer.allocate(16).order(BIG_ENDIAN);
    buffer.putLong(0, uuid.getMostSignificantBits());
    buffer.putLong(8, uuid.getLeastSignificantBits());
    return ENCODER.encodeToString(buffer.array());
  }

  /**
   * Decode a shortened ID to a UUID.
   *
   * @param shortId The short ID
   *
   * @return A UUID
   */

  public static UUID decode(
    final String shortId)
  {
    Objects.requireNonNull(shortId, "shortId");

    final var matcher = SHORT_ID_PATTERN.matcher(shortId);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(String.format(
        "Invalid short ID: Must match %s",
        SHORT_ID_PATTERN.pattern())
      );
    }

    final var buffer = ByteBuffer.allocate(16).order(BIG_ENDIAN);
    buffer.put(DECODER.decode(shortId));
    return new UUID(buffer.getLong(0), buffer.getLong(8));
  }
}
