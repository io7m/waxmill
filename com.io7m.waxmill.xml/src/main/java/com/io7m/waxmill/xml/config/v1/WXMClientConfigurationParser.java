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

import com.io7m.blackthorne.api.BTContentHandler;
import com.io7m.blackthorne.api.BTParseError;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.parser.api.WXMClientConfigurationParserType;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.xml.utilities.WXMParserUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.ERROR;
import static com.io7m.waxmill.xml.config.v1.WXM1CNames.element;
import static com.io7m.waxmill.xml.utilities.WXMParserUtilities.publishError;
import static com.io7m.waxmill.xml.utilities.WXMParserUtilities.safeMessage;

/**
 * A client configuration parser.
 */

public final class WXMClientConfigurationParser
  implements WXMClientConfigurationParserType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMClientConfigurationParser.class);

  private final XMLReader reader;
  private final InputStream stream;
  private final Consumer<WXMParseError> errors;
  private final URI source;
  private final FileSystem fileSystem;

  /**
   * A client configuration parser.
   *
   * @param inFileSystem The filesystem
   * @param inErrors     The error consumer
   * @param inSource     The source URI
   * @param inStream     The source stream
   * @param inReader     The source XML reader
   */

  public WXMClientConfigurationParser(
    final FileSystem inFileSystem,
    final Consumer<WXMParseError> inErrors,
    final URI inSource,
    final InputStream inStream,
    final XMLReader inReader)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.errors =
      Objects.requireNonNull(inErrors, "inErrors");
    this.source =
      Objects.requireNonNull(inSource, "inSource");
    this.stream =
      Objects.requireNonNull(inStream, "inStream");
    this.reader =
      Objects.requireNonNull(inReader, "reader");
  }


  @Override
  public Optional<WXMClientConfiguration> parse()
  {
    LOG.debug("parse: {}", this.source);

    final var contentHandler =
      new BTContentHandler<>(
        this.source,
        this::onError,
        Map.ofEntries(
          Map.entry(
            element("Configuration"),
            c -> new WXM1ClientConfigurationParser(this.fileSystem)
          )
        )
      );

    this.reader.setContentHandler(contentHandler);
    this.reader.setErrorHandler(contentHandler);

    final var inputSource = new InputSource(this.stream);
    inputSource.setPublicId(this.source.toString());

    try {
      this.reader.parse(inputSource);
      LOG.debug("parsing completed");
      return contentHandler.result().map(WXMClientConfiguration.class::cast);
    } catch (final SAXParseException e) {
      LOG.error("error encountered during parsing: ", e);

      final var position =
        LexicalPosition.of(
          e.getLineNumber(),
          e.getColumnNumber(),
          Optional.of(this.source)
        );
      publishError(
        WXMParseError.builder()
          .setException(e)
          .setLexical(position)
          .setMessage(safeMessage(e))
          .setSeverity(ERROR)
          .build(),
        this.errors,
        LOG
      );
      return Optional.empty();
    } catch (final Exception e) {
      LOG.error("error encountered during parsing: ", e);

      final var position =
        LexicalPosition.of(
          -1,
          -1,
          Optional.of(this.source)
        );
      publishError(
        WXMParseError.builder()
          .setException(e)
          .setLexical(position)
          .setMessage(safeMessage(e))
          .setSeverity(ERROR)
          .build(),
        this.errors,
        LOG
      );
      return Optional.empty();
    }
  }

  private void onError(
    final BTParseError btError)
  {
    publishError(
      WXMParserUtilities.mapBlackthorneError(btError),
      this.errors,
      LOG
    );
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
