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

package com.io7m.waxmill.exceptions;

import java.util.Objects;

/**
 * An exception caused by some sort of nonexistent value.
 */

public class WXMExceptionNonexistent extends WXMException
{
  /**
   * Construct an exception.
   *
   * @param message The message
   */

  public WXMExceptionNonexistent(
    final String message)
  {
    super(Objects.requireNonNull(message, "message"));
  }

  /**
   * Construct an exception.
   *
   * @param cause   The cause
   * @param message The message
   */

  public WXMExceptionNonexistent(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }
}
