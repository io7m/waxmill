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

package com.io7m.waxmill.parser.api;

import com.io7m.waxmill.exceptions.WXMException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import static com.io7m.waxmill.parser.api.WXMParseErrorType.Severity.ERROR;

public interface WXMParserProviderType<T>
{
  WXMParserType<T> create(
    FileSystem fileSystem,
    URI uri,
    InputStream stream,
    Consumer<WXMParseError> errors)
    throws IOException;

  /**
   * Convenience function to parse a file directly. Throws an exception if
   * there are any errors logged.
   *
   * @param path The file
   *
   * @return A parsed value
   *
   * @throws WXMException On errors
   */

  default T parse(
    final Path path)
    throws WXMException
  {
    Objects.requireNonNull(path, "path");

    try (var stream = Files.newInputStream(path)) {
      final var errors = new ArrayList<WXMParseError>();
      try (var parser = this.create(
        path.getFileSystem(),
        path.toUri(),
        stream,
        errors::add)) {
        final var result = parser.parse();
        if (errors.stream().anyMatch(e -> e.severity() == ERROR)) {
          throw new WXMParseException(
            "One or more parse errors encountered", errors
          );
        }
        return result.get();
      }
    } catch (final IOException e) {
      throw new WXMException(e);
    }
  }
}
