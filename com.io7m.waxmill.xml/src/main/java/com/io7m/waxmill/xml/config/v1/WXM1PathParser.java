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

package com.io7m.waxmill.xml.config.v1;

import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.nio.file.FileSystem;
import java.util.Objects;

/**
 * "Path" parser.
 */

public final class WXM1PathParser
  implements BTElementHandlerType<Object, WXM1Path>
{
  private final WXM1Path.Builder builder;
  private final FileSystem fileSystem;

  /**
   * "Path" parser.
   *
   * @param inFileSystem The filesystem
   */

  public WXM1PathParser(
    final FileSystem inFileSystem)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.builder =
      WXM1Path.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setType(
        attributes.getValue("type"));
      this.builder.setPath(
        this.fileSystem.getPath(attributes.getValue("value"))
          .toAbsolutePath()
      );
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXM1Path onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
