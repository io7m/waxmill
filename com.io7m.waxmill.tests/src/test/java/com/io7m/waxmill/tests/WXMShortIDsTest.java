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

import com.io7m.waxmill.machines.WXMShortIDs;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.waxmill.machines.WXMShortIDs.SHORT_ID_PATTERN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMShortIDsTest
{
  @Test
  public void testOK()
  {
    System.out.println(WXMShortIDs.encode(UUID.fromString("538a90e4-d50d-4511-8643-ae418279bac4")));
  }

  @TestFactory
  public Stream<DynamicTest> identityExhaustive()
  {
    return Stream.generate(UUID::randomUUID)
      .limit(9_000L)
      .map(WXMShortIDsTest::identityTestFor);
  }

  @TestFactory
  public Stream<DynamicTest> notValid()
  {
    return Stream.generate(UUID::randomUUID)
      .limit(9_000L)
      .map(WXMShortIDsTest::notValidTestFor);
  }

  private static DynamicTest notValidTestFor(
    final UUID uuid)
  {
    return DynamicTest.dynamicTest(
      String.format("notValidFor_%s", uuid),
      () -> notValidFor(uuid)
    );
  }

  private static void notValidFor(
    final UUID uuid)
  {
    assertThrows(
      IllegalArgumentException.class,
      () -> WXMShortIDs.decode(uuid.toString())
    );
  }

  private static DynamicTest identityTestFor(
    final UUID uuid)
  {
    return DynamicTest.dynamicTest(
      String.format("identityExhaustiveFor_%s", uuid),
      () -> identityFor(uuid)
    );
  }

  private static void identityFor(
    final UUID uuid)
  {
    final var encoded = WXMShortIDs.encode(uuid);
    final var matcher = SHORT_ID_PATTERN.matcher(encoded);
    assertTrue(matcher.matches(), String.format("%s is valid", encoded));
    final var decoded = WXMShortIDs.decode(encoded);
    assertEquals(uuid, decoded);
  }
}
