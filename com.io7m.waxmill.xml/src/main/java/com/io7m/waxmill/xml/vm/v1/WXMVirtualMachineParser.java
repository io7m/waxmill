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

package com.io7m.waxmill.xml.vm.v1;

import com.io7m.blackthorne.api.BTContentHandler;
import com.io7m.blackthorne.api.BTParseError;
import com.io7m.blackthorne.api.BTParseErrorType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import com.io7m.waxmill.client.api.WXMVirtualMachineSet;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity;
import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.ERROR;
import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.WARNING;
import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXMVirtualMachineParser
  implements WXMVirtualMachineParserType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMVirtualMachineParser.class);

  private final XMLReader reader;
  private final InputStream stream;
  private final Consumer<WXMParseError> errors;
  private final URI source;

  public WXMVirtualMachineParser(
    final Consumer<WXMParseError> inErrors,
    final URI inSource,
    final InputStream inStream,
    final XMLReader inReader)
  {
    this.errors =
      Objects.requireNonNull(inErrors, "inErrors");
    this.source =
      Objects.requireNonNull(inSource, "inSource");
    this.stream =
      Objects.requireNonNull(inStream, "inStream");
    this.reader =
      Objects.requireNonNull(inReader, "reader");
  }

  private static String safeMessage(
    final Exception e)
  {
    final var message = e.getMessage();
    if (message == null) {
      return e.getClass().getCanonicalName();
    }
    return message;
  }

  private static Severity mapSeverity(
    final BTParseErrorType.Severity severity)
  {
    switch (severity) {
      case WARNING:
        return WARNING;
      case ERROR:
        return ERROR;
      default:
        throw new UnreachableCodeException();
    }
  }

  @Override
  public Optional<WXMVirtualMachineSet> parse()
  {
    LOG.debug("parse: {}", this.source);

    final var contentHandler =
      new BTContentHandler<>(
        this.source,
        this::onError,
        Map.ofEntries(
          Map.entry(
            element("VirtualMachines"),
            c -> new WXM1VirtualMachineSetParser(this.source)
          ),
          Map.entry(
            element("VirtualMachine"),
            c -> new WXM1VirtualMachineParser(this.source)
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

      final var resultOpt = contentHandler.result();
      if (resultOpt.isEmpty()) {
        return Optional.empty();
      }

      final var resultObj = resultOpt.get();
      if (resultObj instanceof WXMVirtualMachineSet) {
        return Optional.of((WXMVirtualMachineSet) resultObj);
      }
      if (resultObj instanceof WXMVirtualMachine) {
        final var machine = (WXMVirtualMachine) resultObj;
        return Optional.of(
          WXMVirtualMachineSet.builder()
            .setMachines(new TreeMap<>(Map.of(machine.id(), machine)))
            .build()
        );
      }
      throw new UnreachableCodeException();
    } catch (final SAXParseException e) {
      LOG.error("error encountered during parsing: ", e);

      final var position =
        LexicalPosition.of(
          e.getLineNumber(),
          e.getColumnNumber(),
          Optional.of(this.source)
        );
      this.publishError(
        WXMParseError.builder()
          .setException(e)
          .setLexical(position)
          .setMessage(safeMessage(e))
          .setSeverity(ERROR)
          .build()
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
      this.publishError(
        WXMParseError.builder()
          .setException(e)
          .setLexical(position)
          .setMessage(safeMessage(e))
          .setSeverity(ERROR)
          .build()
      );
      return Optional.empty();
    }
  }

  private void publishError(
    final WXMParseError error)
  {
    try {
      final var lexical =
        error.lexical();
      final var errorSource =
        lexical.file()
          .map(URI::toString)
          .orElse("");

      switch (error.severity()) {
        case WARNING:
          LOG.warn(
            "{}:{}:{}: {}",
            errorSource,
            Integer.valueOf(lexical.line()),
            Integer.valueOf(lexical.column()),
            error.message()
          );
          break;
        case ERROR:
          LOG.error(
            "{}:{}:{}: {}",
            errorSource,
            Integer.valueOf(lexical.line()),
            Integer.valueOf(lexical.column()),
            error.message()
          );
          break;
      }

      this.errors.accept(Objects.requireNonNull(error, "error"));
    } catch (final Exception e) {
      LOG.error("ignored exception raised by error consumer: ", e);
    }
  }

  private void onError(
    final BTParseError btError)
  {
    this.publishError(
      WXMParseError.builder()
        .setLexical(btError.lexical())
        .setMessage(btError.message())
        .setException(btError.exception())
        .setSeverity(mapSeverity(btError.severity()))
        .build()
    );
  }

  @Override
  public void close()
    throws IOException
  {
    this.stream.close();
  }
}
