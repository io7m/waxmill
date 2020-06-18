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

import com.io7m.waxmill.client.api.WXMTap;
import com.io7m.waxmill.client.api.WXMVMNet;
import com.io7m.waxmill.cmdline.internal.WXMVirtioNetworkBackendConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMVirtioNetworkBackendConverterTest
{
  @Test
  public void tapIsOK()
  {
    final var result =
      (WXMTap) new WXMVirtioNetworkBackendConverter()
        .convert("tap;tap23;f8:e1:e1:79:c9:7e");

    assertEquals("tap23", result.name().value());
    assertEquals("f8:e1:e1:79:c9:7e", result.address().value());
  }

  @Test
  public void vmnetIsOK()
  {
    final var result =
      (WXMVMNet) new WXMVirtioNetworkBackendConverter()
        .convert("vmnet;vmnet23;f8:e1:e1:79:c9:7e");

    assertEquals("vmnet23", result.name().value());
    assertEquals("f8:e1:e1:79:c9:7e", result.address().value());
  }

  @Test
  public void syntaxError0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMVirtioNetworkBackendConverter()
        .convert("");
    });
  }

  @Test
  public void syntaxError1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMVirtioNetworkBackendConverter()
        .convert("what;is;this");
    });
  }

  @Test
  public void syntaxErrorTap0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMVirtioNetworkBackendConverter()
        .convert("tap");
    });
  }

  @Test
  public void syntaxErrorTap1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMVirtioNetworkBackendConverter()
        .convert("tap;tap23");
    });
  }

  @Test
  public void syntaxErrorVMNet0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMVirtioNetworkBackendConverter()
        .convert("vmnet");
    });
  }

  @Test
  public void syntaxErrorVMNet1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMVirtioNetworkBackendConverter()
        .convert("vmnet;vmnet23");
    });
  }
}
