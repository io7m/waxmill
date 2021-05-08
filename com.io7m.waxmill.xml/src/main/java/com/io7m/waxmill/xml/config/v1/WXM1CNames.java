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

package com.io7m.waxmill.xml.config.v1;

import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.waxmill.xml.WXMSchemas;

import java.util.Objects;

/**
 * Functions to create element names.
 */

public final class WXM1CNames
{
  private WXM1CNames()
  {

  }

  /**
   * Create a qualified name for the local name.
   *
   * @param localName The local name
   *
   * @return A qualified name
   */

  public static BTQualifiedName element(
    final String localName)
  {
    return BTQualifiedName.of(
      WXMSchemas.configSchemaV1p0NamespaceText(),
      Objects.requireNonNull(localName, "localName")
    );
  }
}
