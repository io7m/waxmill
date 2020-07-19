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

import com.io7m.waxmill.cmdline.internal.WXMNetworkBackendConverter;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMNetworkBackendConverterTest
{
  @Test
  public void tapIsOK0()
  {
    final var result =
      (WXMTap) new WXMNetworkBackendConverter()
        .convert("tap;tap23;f8:e1:e1:79:c9:7e");

    assertEquals("tap23", result.name().value());
    assertEquals("f8:e1:e1:79:c9:7e", result.address().value());
  }

  @Test
  public void tapIsOK1()
  {
    final var result =
      (WXMTap) new WXMNetworkBackendConverter()
        .convert("tap;tap23;f8:e1:e1:79:c9:7e;x,y,z");

    assertEquals("tap23", result.name().value());
    assertEquals("f8:e1:e1:79:c9:7e", result.address().value());
    assertEquals("x", result.groups().get(0).value());
    assertEquals("y", result.groups().get(1).value());
    assertEquals("z", result.groups().get(2).value());
  }

  @Test
  public void vmnetIsOK0()
  {
    final var result =
      (WXMVMNet) new WXMNetworkBackendConverter()
        .convert("vmnet;vmnet23;f8:e1:e1:79:c9:7e");

    assertEquals("vmnet23", result.name().value());
    assertEquals("f8:e1:e1:79:c9:7e", result.address().value());
  }

  @Test
  public void vmnetIsOK1()
  {
    final var result =
      (WXMVMNet) new WXMNetworkBackendConverter()
        .convert("vmnet;vmnet23;f8:e1:e1:79:c9:7e;x,y,z");

    assertEquals("vmnet23", result.name().value());
    assertEquals("f8:e1:e1:79:c9:7e", result.address().value());
    assertEquals("x", result.groups().get(0).value());
    assertEquals("y", result.groups().get(1).value());
    assertEquals("z", result.groups().get(2).value());
  }

  @Test
  public void syntaxError0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("");
    });
  }

  @Test
  public void syntaxError1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("what;is;this");
    });
  }

  @Test
  public void syntaxErrorTap0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("tap");
    });
  }

  @Test
  public void syntaxErrorTap1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("tap;tap23");
    });
  }

  @Test
  public void syntaxErrorTap2()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("tap;tap23;f8:e1:e1:79:c9:7e;w0");
    });
  }

  @Test
  public void syntaxErrorTap3()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("tap;tap23;f8:e1:e1:79:c9:7e;w;x");
    });
  }

  @Test
  public void syntaxErrorVMNet0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("vmnet");
    });
  }

  @Test
  public void syntaxErrorVMNet1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("vmnet;vmnet23");
    });
  }

  @Test
  public void syntaxErrorVMNet2()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("vmnet;vmnet23;f8:e1:e1:79:c9:7e;w0");
    });
  }

  @Test
  public void syntaxErrorVMNet3()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMNetworkBackendConverter()
        .convert("vmnet;vmnet23;f8:e1:e1:79:c9:7e;w;x");
    });
  }
}
