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
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserType;
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
import java.util.TreeMap;
import java.util.function.Consumer;

import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.ERROR;
import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

/**
 * Virtual machine parser.
 */

public final class WXMVirtualMachineParser
  implements WXMVirtualMachineParserType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMVirtualMachineParser.class);

  private final XMLReader reader;
  private final InputStream stream;
  private final FileSystem fileSystem;
  private final Consumer<WXMParseError> errors;
  private final URI source;

  /**
   * Virtual machine parser.
   *
   * @param inFileSystem The file system
   * @param inErrors     The error consumer
   * @param inSource     The source URI
   * @param inStream     The source stream
   * @param inReader     The source reader
   */

  public WXMVirtualMachineParser(
    final FileSystem inFileSystem,
    final Consumer<WXMParseError> inErrors,
    final URI inSource,
    final InputStream inStream,
    final XMLReader inReader)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "inFileSystem");
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
            c -> new WXM1VirtualMachineSetParser(this.fileSystem, this.source)
          ),
          Map.entry(
            element("VirtualMachine"),
            c -> new WXM1VirtualMachineParser(this.fileSystem, this.source)
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
      WXMParserUtilities.publishError(
        WXMParseError.builder()
          .setException(e)
          .setLexical(position)
          .setMessage(WXMParserUtilities.safeMessage(e))
          .setSeverity(ERROR)
          .build(), this.errors, LOG
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
      WXMParserUtilities.publishError(
        WXMParseError.builder()
          .setException(e)
          .setLexical(position)
          .setMessage(WXMParserUtilities.safeMessage(e))
          .setSeverity(ERROR)
          .build(), this.errors, LOG
      );
      return Optional.empty();
    }
  }

  private void onError(
    final BTParseError btError)
  {
    WXMParserUtilities.publishError(
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
