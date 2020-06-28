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

package com.io7m.waxmill.serializer.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public interface WXMSerializerProviderType<T>
{
  WXMSerializerType create(
    URI uri,
    OutputStream stream,
    T value)
    throws IOException;

  default void serialize(
    final Path output,
    final Path outputTmp,
    final T value)
    throws IOException
  {
    Objects.requireNonNull(output, "output");
    Objects.requireNonNull(outputTmp, "outputTmp");
    Objects.requireNonNull(value, "value");

    try (var stream = Files.newOutputStream(outputTmp, CREATE, TRUNCATE_EXISTING)) {
      try (var serializer = this.create(outputTmp.toUri(), stream, value)) {
        serializer.execute();
      }
      Files.move(outputTmp, output, ATOMIC_MOVE, REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(outputTmp);
    }
  }
}
