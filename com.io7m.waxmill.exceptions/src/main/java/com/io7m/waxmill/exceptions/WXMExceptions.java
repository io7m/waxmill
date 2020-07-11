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

package com.io7m.waxmill.exceptions;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for accumulating exceptions.
 */

public final class WXMExceptions
{
  private final List<Throwable> exceptions;

  /**
   * Construct an empty exception list.
   */

  public WXMExceptions()
  {
    this.exceptions = new ArrayList<>();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMExceptions 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }

  /**
   * Log an exception.
   *
   * @param e The exception
   */

  public void add(
    final Throwable e)
  {
    synchronized (this.exceptions) {
      this.exceptions.add(e);
    }
  }

  /**
   * Throw an exception encapsulating all of the previously logged exceptions
   * as suppressed exceptions, if any have been logged. Does nothing it {@link #add(Throwable)}
   * has never been called.
   *
   * @throws WXMException If any exceptions have been logged
   */

  public void throwIfRequired()
    throws WXMException
  {
    synchronized (this.exceptions) {
      if (!this.exceptions.isEmpty()) {
        final var exception =
          new WXMException("One or more errors were encountered.");
        for (final var ex : this.exceptions) {
          exception.addSuppressed(ex);
        }
        throw exception;
      }
    }
  }
}
