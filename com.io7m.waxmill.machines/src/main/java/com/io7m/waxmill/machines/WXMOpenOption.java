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

package com.io7m.waxmill.machines;

import com.io7m.junreachable.UnreachableCodeException;

/**
 * Options used when opening files.
 */

public enum WXMOpenOption
{
  /**
   * Do not use caching (O_DIRECT).
   */

  NO_CACHE,

  /**
   * Writes to the file are synchronous (O_SYNC).
   */

  SYNCHRONOUS,

  /**
   * The file is opened read-only.
   */

  READ_ONLY;

  /**
   * @return The external name of the option as it would appear on a BHyve
   * command line
   */

  public String externalName()
  {
    switch (this) {
      case NO_CACHE:
        return "nocache";
      case SYNCHRONOUS:
        return "direct";
      case READ_ONLY:
        return "ro";
    }
    throw new UnreachableCodeException();
  }
}
