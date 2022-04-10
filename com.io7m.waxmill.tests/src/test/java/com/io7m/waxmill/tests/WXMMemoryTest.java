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

package com.io7m.waxmill.tests;

import com.io7m.waxmill.machines.WXMMemory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public final class WXMMemoryTest
{
  @Test
  public void totalSize()
  {
    final var memory =
      WXMMemory.builder()
        .setMegabytes(BigInteger.TEN)
        .setGigabytes(BigInteger.TEN)
        .build();

    Assertions.assertEquals(
      BigInteger.valueOf(10_000_000_000L + 10_000_000L),
      memory.totalBytes()
    );
  }

  @Test
  public void invalidMegabytes()
  {
    Assertions.assertThrows(Exception.class, () -> {
      WXMMemory.builder()
        .setMegabytes(BigInteger.TEN.negate())
        .setGigabytes(BigInteger.TEN)
        .build();
    });
  }

  @Test
  public void invalidGigabytes()
  {
    Assertions.assertThrows(Exception.class, () -> {
      WXMMemory.builder()
        .setMegabytes(BigInteger.TEN)
        .setGigabytes(BigInteger.TEN.negate())
        .build();
    });
  }
}
