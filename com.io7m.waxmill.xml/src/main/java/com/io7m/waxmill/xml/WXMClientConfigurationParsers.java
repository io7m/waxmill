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

package com.io7m.waxmill.xml;

import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.jxe.core.JXEXInclude;
import com.io7m.waxmill.parser.api.WXMClientConfigurationParserProviderType;
import com.io7m.waxmill.parser.api.WXMClientConfigurationParserType;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.xml.config.v1.WXMClientConfigurationParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A provider of client configuration parsers.
 */

public final class WXMClientConfigurationParsers
  implements WXMClientConfigurationParserProviderType
{
  private final JXEHardenedSAXParsers parsers;

  /**
   * Construct a provider.
   */

  public WXMClientConfigurationParsers()
  {
    this.parsers = new JXEHardenedSAXParsers();
  }

  @Override
  public WXMClientConfigurationParserType create(
    final FileSystem fileSystem,
    final URI uri,
    final InputStream stream,
    final Consumer<WXMParseError> errors)
    throws IOException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(errors, "errors");

    try {
      final var reader =
        this.parsers.createXMLReader(
          Optional.empty(),
          JXEXInclude.XINCLUDE_DISABLED,
          WXMSchemas.schemas()
        );
      return new WXMClientConfigurationParser(
        fileSystem,
        errors,
        uri,
        stream,
        reader
      );
    } catch (final ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMClientConfigurationParsers 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
