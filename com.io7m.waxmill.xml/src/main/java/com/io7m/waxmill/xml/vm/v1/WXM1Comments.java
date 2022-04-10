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

package com.io7m.waxmill.xml.vm.v1;

import com.io7m.waxmill.xml.WXMSchemas;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Functions over comments.
 */

public final class WXM1Comments
{
  private WXM1Comments()
  {

  }

  /**
   * Serialize a comment.
   *
   * @param comment The comment
   * @param writer  The XML writer
   *
   * @throws XMLStreamException On errors
   */

  public static void serializeComment(
    final String comment,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    if (!comment.isBlank()) {
      final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
      writer.writeStartElement(namespaceURI, "Comment");
      writer.writeCharacters(comment);
      writer.writeEndElement();
    }
  }
}
