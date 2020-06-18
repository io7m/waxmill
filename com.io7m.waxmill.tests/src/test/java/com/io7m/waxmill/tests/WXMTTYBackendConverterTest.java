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

import com.io7m.waxmill.client.api.WXMTTYBackendFile;
import com.io7m.waxmill.client.api.WXMTTYBackendNMDM;
import com.io7m.waxmill.client.api.WXMTTYBackendStdio;
import com.io7m.waxmill.cmdline.internal.WXMTTYBackendConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMTTYBackendConverterTest
{
  @Test
  public void stdioIsOK()
  {
    final var result =
      (WXMTTYBackendStdio) new WXMTTYBackendConverter()
        .convert("stdio;com1");

    assertEquals("com1", result.device());
  }

  @Test
  public void fileIsOK()
  {
    final var result =
      (WXMTTYBackendFile) new WXMTTYBackendConverter()
        .convert("file;com1;/tmp/xyz");

    assertEquals("com1", result.device());
    assertEquals("/tmp/xyz", result.path().toString());
  }

  @Test
  public void nmdmIsOK()
  {
    final var result =
      (WXMTTYBackendNMDM) new WXMTTYBackendConverter()
        .convert("nmdm;com1");

    assertEquals("com1", result.device());
  }

  @Test
  public void syntaxError0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("");
    });
  }

  @Test
  public void syntaxError1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("what;is;this");
    });
  }

  @Test
  public void syntaxErrorStdio0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("stdio");
    });
  }

  @Test
  public void syntaxErrorStdio1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("stdio;zyx");
    });
  }

  @Test
  public void syntaxErrorFile0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("file");
    });
  }

  @Test
  public void syntaxErrorFile1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("file;xyz");
    });
  }

  @Test
  public void syntaxErrorFile2()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("file;xyz;xyz");
    });
  }

  @Test
  public void syntaxErrorNMDM0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("nmdm");
    });
  }

  @Test
  public void syntaxErrorNMDM1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMTTYBackendConverter()
        .convert("nmdm;zyx");
    });
  }
}
