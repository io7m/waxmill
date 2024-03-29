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

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import org.immutables.value.Value;

import java.math.BigInteger;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;

/**
 * The specification of the size of a virtual machine's memory. This
 * is specified as a number of megabytes {@code m} plus a number of gigabytes
 * {@code g}, where {@code m >= 0 ∧ g >= 0}.
 */

@ImmutablesStyleType
@Value.Immutable
public interface WXMMemoryType
{
  /**
   * @return The number of megabytes
   */

  @Value.Default
  default BigInteger megabytes()
  {
    return valueOf(512L);
  }

  /**
   * @return The number of gigabytes
   */

  @Value.Default
  default BigInteger gigabytes()
  {
    return valueOf(0L);
  }

  /**
   * @return A descriptive comment
   */

  @Value.Default
  default String comment()
  {
    return "";
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.megabytes(),
      this.megabytes().compareTo(ZERO) >= 0,
      x -> "Megabytes must be >= 0"
    );
    Preconditions.checkPrecondition(
      this.gigabytes(),
      this.gigabytes().compareTo(ZERO) >= 0,
      x -> "Gigabytes must be >= 0"
    );
  }

  /**
   * @return The total size of the memory in bytes
   */

  default BigInteger totalBytes()
  {
    final var megabytesToBytes =
      valueOf(1_000_000L);
    final var gigabytesToBytes =
      valueOf(1_000_000_000L);
    final var mbb =
      this.megabytes().multiply(megabytesToBytes);
    final var gbb =
      this.gigabytes().multiply(gigabytesToBytes);
    return mbb.add(gbb);
  }

  /**
   * @return The total size of the memory in megabytes
   */

  default BigInteger totalMegabytes()
  {
    final var gigabytesToMegabytes =
      valueOf(1_000L);
    final var gmb =
      this.gigabytes().multiply(gigabytesToMegabytes);
    return this.megabytes().add(gmb);
  }
}
