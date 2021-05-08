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

package com.io7m.waxmill.process.posix;

import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;

import java.io.IOException;

/**
 * A JNA POSIX implementation of the process creator.
 */

public final class WXMProcessesPOSIXService implements WXMProcessesType
{
  private final WXMProcessesType delegate;

  /**
   * A JNA POSIX implementation of the process creator.
   */

  public WXMProcessesPOSIXService()
  {
    this.delegate = WXMProcessesPOSIX.create();
  }

  @Override
  public void processReplaceCurrent(
    final WXMProcessDescription description)
    throws IOException
  {
    this.delegate.processReplaceCurrent(description);
  }

  @Override
  public Process processStart(
    final WXMProcessDescription description)
    throws IOException
  {
    return this.delegate.processStart(description);
  }

  @Override
  public void processStartAndWait(
    final WXMProcessDescription description)
    throws IOException, InterruptedException
  {
    this.delegate.processStartAndWait(description);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMProcessesPOSIXService 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
