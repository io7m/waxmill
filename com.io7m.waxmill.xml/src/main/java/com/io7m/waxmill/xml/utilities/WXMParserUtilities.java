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

package com.io7m.waxmill.xml.utilities;

import com.io7m.blackthorne.api.BTParseError;
import com.io7m.blackthorne.api.BTParseErrorType;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.parser.api.WXMParseErrorType;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.ERROR;
import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.WARNING;

/**
 * Parser utilities.
 */

public final class WXMParserUtilities
{
  private WXMParserUtilities()
  {

  }

  /**
   * Ensure an exception always has a message.
   *
   * @param e The exception
   *
   * @return The exception message
   */

  public static String safeMessage(
    final Exception e)
  {
    final var message = e.getMessage();
    if (message == null) {
      return e.getClass().getCanonicalName();
    }
    return message;
  }

  /**
   * Map blackthorne severity levels to WXM severity levels.
   *
   * @param severity The blackthorne severity
   *
   * @return The WXM severity
   */

  public static WXMParseErrorType.Severity mapSeverity(
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

  /**
   * Publish an error.
   *
   * @param error  The error
   * @param errors The error consumer
   * @param logger The logger
   */

  public static void publishError(
    final WXMParseError error,
    final Consumer<WXMParseError> errors,
    final Logger logger)
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
          logger.warn(
            "{}:{}:{}: {}",
            errorSource,
            Integer.valueOf(lexical.line()),
            Integer.valueOf(lexical.column()),
            error.message()
          );
          break;
        case ERROR:
          logger.error(
            "{}:{}:{}: {}",
            errorSource,
            Integer.valueOf(lexical.line()),
            Integer.valueOf(lexical.column()),
            error.message()
          );
          break;
      }

      errors.accept(Objects.requireNonNull(error, "error"));
    } catch (final Exception e) {
      logger.error("ignored exception raised by error consumer: ", e);
    }
  }

  /**
   * Map from blackthorne errors to WXM errors.
   *
   * @param btError The blackthorne error
   *
   * @return The WXM error
   */

  public static WXMParseError mapBlackthorneError(
    final BTParseError btError)
  {
    return WXMParseError.builder()
      .setLexical(btError.lexical())
      .setMessage(btError.message())
      .setException(btError.exception())
      .setSeverity(mapSeverity(btError.severity()))
      .build();
  }
}
