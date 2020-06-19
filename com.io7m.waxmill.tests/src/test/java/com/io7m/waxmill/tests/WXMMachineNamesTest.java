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

import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMMachineNames;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

public final class WXMMachineNamesTest
{
  private static DynamicTest makeNameValid(
    final String s)
  {
    return DynamicTest.dynamicTest(
      String.format("testValid_%s", s),
      () -> {
        Assertions.assertTrue(WXMMachineNames.isValid(s));
        WXMMachineNames.checkValid(s);
        WXMMachineName.of(s);
      }
    );
  }

  private static DynamicTest makeNameInvalid(
    final String s)
  {
    return DynamicTest.dynamicTest(
      String.format("testInvalid_%s", s),
      () -> {
        Assertions.assertFalse(WXMMachineNames.isValid(s));
        Assertions.assertThrows(
          IllegalArgumentException.class, () -> WXMMachineNames.checkValid(s));
        Assertions.assertThrows(
          IllegalArgumentException.class, () -> WXMMachineName.of(s));
      }
    );
  }

  @TestFactory
  public Stream<DynamicTest> testNamesValid()
  {
    return Stream.of(
      "a",
      "0",
      "-",
      ".",
      "com.io7m.waxmill.tests.com.io7m.waxmill.tests.com.io7m.waxmill.t"
    ).map(WXMMachineNamesTest::makeNameValid);
  }

  @TestFactory
  public Stream<DynamicTest> testNamesInvalid()
  {
    return Stream.of(
      "",
      "@",
      "♯",
      "com.io7m.waxmill.tests.com.io7m.waxmill.tests.com.io7m.waxmill.te"
    ).map(WXMMachineNamesTest::makeNameInvalid);
  }
}
