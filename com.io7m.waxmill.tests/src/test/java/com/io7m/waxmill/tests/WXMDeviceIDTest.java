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

package com.io7m.waxmill.tests;

import com.io7m.waxmill.cmdline.internal.WXMDeviceSlotConverter;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.io7m.waxmill.machines.WXMDeviceSlotType.VALID_BUS_IDS;
import static com.io7m.waxmill.machines.WXMDeviceSlotType.VALID_FUNCTION_IDS;
import static com.io7m.waxmill.machines.WXMDeviceSlotType.VALID_SLOT_IDS;

public final class WXMDeviceIDTest
{
  public static WXMDeviceSlot convert(
    final String value)
  {
    return new WXMDeviceSlotConverter().convert(value);
  }

  @Test
  public void validValuesAreOK()
  {
    for (var bus = VALID_BUS_IDS.lower(); bus <= VALID_BUS_IDS.upper(); ++bus) {
      for (var slot = VALID_SLOT_IDS.lower(); slot <= VALID_SLOT_IDS.upper(); ++slot) {
        for (var func = VALID_FUNCTION_IDS.lower(); func <= VALID_FUNCTION_IDS.upper(); ++func) {
          final var ds =
            WXMDeviceSlot.builder()
              .setBusID(bus)
              .setSlotID(slot)
              .setFunctionID(func)
              .build();

          Assertions.assertEquals(0, ds.compareTo(ds));
        }
      }
    }
  }

  @Test
  public void invalidSlotValuesAreNotOK()
  {
    for (int id = -VALID_SLOT_IDS.lower(); id < VALID_SLOT_IDS.lower(); ++id) {
      final int finalId = id;
      Assertions.assertThrows(Exception.class, () -> {
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(finalId)
          .setFunctionID(0)
          .build();
      });
    }

    for (int id = VALID_SLOT_IDS.upper() + 1; id < VALID_SLOT_IDS.upper() + 100; ++id) {
      final int finalId = id;
      Assertions.assertThrows(Exception.class, () -> {
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(finalId)
          .setFunctionID(0)
          .build();
      });
    }
  }

  @Test
  public void invalidBusValuesAreNotOK()
  {
    for (int id = -VALID_BUS_IDS.lower(); id < VALID_BUS_IDS.lower(); ++id) {
      final int finalId = id;
      Assertions.assertThrows(Exception.class, () -> {
        WXMDeviceSlot.builder()
          .setBusID(finalId)
          .setSlotID(0)
          .setFunctionID(0)
          .build();
      });
    }

    for (int id = VALID_BUS_IDS.upper() + 1; id < VALID_BUS_IDS.upper() + 100; ++id) {
      final int finalId = id;
      Assertions.assertThrows(Exception.class, () -> {
        WXMDeviceSlot.builder()
          .setBusID(finalId)
          .setSlotID(0)
          .setFunctionID(0)
          .build();
      });
    }
  }

  @Test
  public void invalidFunctionValuesAreNotOK()
  {
    for (int id = -VALID_FUNCTION_IDS.lower(); id < VALID_FUNCTION_IDS.lower(); ++id) {
      final int finalId = id;
      Assertions.assertThrows(Exception.class, () -> {
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(0)
          .setFunctionID(finalId)
          .build();
      });
    }

    for (int id = VALID_FUNCTION_IDS.upper() + 1; id < VALID_FUNCTION_IDS.upper() + 100; ++id) {
      final int finalId = id;
      Assertions.assertThrows(Exception.class, () -> {
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(0)
          .setFunctionID(finalId)
          .build();
      });
    }
  }
}
