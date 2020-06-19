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

package com.io7m.waxmill.boot.internal;

import com.io7m.waxmill.machines.WXMDeviceType;

import java.nio.file.Path;
import java.util.Objects;

public final class WXMGRUBDeviceAndPath
{
  private final int index;
  private final WXMDeviceType device;
  private final Path path;

  public WXMGRUBDeviceAndPath(
    final int inIndex,
    final WXMDeviceType inDevice,
    final Path inPath)
  {
    this.index =
      inIndex;
    this.device =
      Objects.requireNonNull(inDevice, "device");
    this.path =
      Objects.requireNonNull(inPath, "path");
  }

  public int index()
  {
    return this.index;
  }

  public WXMDeviceType device()
  {
    return this.device;
  }

  public Path path()
  {
    return this.path;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMGRUBDeviceAndPath 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
