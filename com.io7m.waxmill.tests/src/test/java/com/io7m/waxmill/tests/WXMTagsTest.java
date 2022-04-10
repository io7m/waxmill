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

import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMTag;
import com.io7m.waxmill.machines.WXMTags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

public final class WXMTagsTest
{
  private static DynamicTest makeNameValid(
    final String s)
  {
    return DynamicTest.dynamicTest(
      String.format("testValid_%s", s),
      () -> {
        Assertions.assertTrue(WXMTags.isValid(s));
        WXMTags.checkValid(s);
        WXMTag.of(s);
      }
    );
  }

  private static DynamicTest makeNameInvalid(
    final String s)
  {
    return DynamicTest.dynamicTest(
      String.format("testInvalid_%s", s),
      () -> {
        Assertions.assertFalse(WXMTags.isValid(s));
        Assertions.assertThrows(
          IllegalArgumentException.class, () -> WXMTags.checkValid(s));
        Assertions.assertThrows(
          IllegalArgumentException.class, () -> WXMTag.of(s));
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
      "com_io7m_waxmill_tests_com_io7m_waxmill_tests_com_io7m_waxmill_t"
    ).map(WXMTagsTest::makeNameValid);
  }

  @TestFactory
  public Stream<DynamicTest> testNamesInvalid()
  {
    return Stream.of(
      "",
      "@",
      "♯",
      ".",
      "com.io7m.waxmill.tests.com.io7m.waxmill.tests.com.io7m.waxmill.te"
    ).map(WXMTagsTest::makeNameInvalid);
  }
}
