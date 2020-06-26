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

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jranges.RangeCheck;
import com.io7m.jranges.RangeInclusiveI;
import org.immutables.value.Value;

import java.util.Comparator;

/**
 * A device slot. A device is uniquely identified by the PCI bus, slot, and
 * function to which it is attached.
 */

@Value.Immutable
@ImmutablesStyleType
public abstract class WXMDeviceSlotType implements Comparable<WXMDeviceSlotType>
{
  /**
   * The inclusive range of valid PCI slot IDs.
   */

  public static final RangeInclusiveI VALID_SLOT_IDS =
    RangeInclusiveI.of(0, 31);

  /**
   * The inclusive range of valid PCI bus IDs.
   */

  public static final RangeInclusiveI VALID_BUS_IDS =
    RangeInclusiveI.of(0, 255);

  /**
   * The inclusive range of valid PCI function IDs.
   */

  public static final RangeInclusiveI VALID_FUNCTION_IDS =
    RangeInclusiveI.of(0, 7);

  /**
   * @return The PCI bus ID
   */

  abstract int busID();

  /**
   * @return The PCI slot ID
   */

  abstract int slotID();

  /**
   * @return The PCI function ID
   */

  abstract int functionID();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  final void checkPreconditions()
  {
    RangeCheck.checkIncludedInInteger(
      this.busID(),
      "PCI bus ID",
      VALID_BUS_IDS,
      "Valid PCI bus IDs"
    );
    RangeCheck.checkIncludedInInteger(
      this.slotID(),
      "PCI slot ID",
      VALID_SLOT_IDS,
      "Valid PCI slot IDs"
    );
    RangeCheck.checkIncludedInInteger(
      this.functionID(),
      "PCI function ID",
      VALID_FUNCTION_IDS,
      "Valid PCI function IDs"
    );
  }

  @Override
  public final int compareTo(
    final WXMDeviceSlotType other)
  {
    return Comparator.comparingInt(WXMDeviceSlotType::busID)
      .thenComparingInt(WXMDeviceSlotType::slotID)
      .thenComparingInt(WXMDeviceSlotType::functionID)
      .compare(this, other);
  }

  @Override
  public final String toString()
  {
    return String.format(
      "%d:%d:%d",
      Integer.valueOf(this.busID()),
      Integer.valueOf(this.slotID()),
      Integer.valueOf(this.functionID())
    );
  }
}
