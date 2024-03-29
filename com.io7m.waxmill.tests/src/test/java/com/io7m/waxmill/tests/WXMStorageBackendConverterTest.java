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

import com.io7m.jaffirm.core.PreconditionViolationException;
import com.io7m.waxmill.cmdline.internal.WXMStorageBackendConverter;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMStorageBackendConverterTest
{
  @Test
  public void fileIsOK()
  {
    final var result =
      (WXMStorageBackendFile) new WXMStorageBackendConverter()
        .convert("file;/tmp/xyz");

    assertEquals("/tmp/xyz", result.file().toString());
  }

  @Test
  public void zfsVolumeIsOK()
  {
    final var result =
      (WXMStorageBackendZFSVolume) new WXMStorageBackendConverter()
        .convert("zfs-volume");
  }

  @Test
  public void zfsVolumeSizeIsOK()
  {
    final var result =
      (WXMStorageBackendZFSVolume) new WXMStorageBackendConverter()
        .convert("zfs-volume;128000000");

    assertEquals(new BigInteger("128000000"), result.expectedSize().get());
  }

  @Test
  public void zfsVolumeSizeIsNotOK0()
  {
    assertThrows(PreconditionViolationException.class, () -> {
      new WXMStorageBackendConverter().convert("zfs-volume;100000000");
    });
  }

  @Test
  public void zfsVolumeSizeIsNotOK1()
  {
    assertThrows(NumberFormatException.class, () -> {
      new WXMStorageBackendConverter().convert("zfs-volume;z");
    });
  }

  @Test
  public void syntaxError0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMStorageBackendConverter()
        .convert("");
    });
  }

  @Test
  public void syntaxError1()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMStorageBackendConverter()
        .convert("what;is;this");
    });
  }

  @Test
  public void syntaxErrorFile0()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new WXMStorageBackendConverter()
        .convert("file");
    });
  }
}
