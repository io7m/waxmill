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

package com.io7m.waxmill.tests;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.parser.api.WXMClientConfigurationParserProviderType;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.serializer.api.WXMClientConfigurationSerializerProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public abstract class WXMClientConfigurationSerializerContract
{
  private Path directory;
  private ArrayList<WXMParseError> errors;

  protected abstract Logger logger();

  protected abstract WXMClientConfigurationParserProviderType parsers();

  protected abstract WXMClientConfigurationSerializerProviderType serializers();

  @BeforeEach
  public void testSetup()
    throws Exception
  {
    this.directory = WXMTestDirectories.createTempDirectory();
    this.errors = new ArrayList<>();
  }

  @Test
  public void exampleRoundTrip()
    throws Exception
  {
    final var cf0 =
      this.parseResource("config0.xml");
    final var output =
      Files.newOutputStream(this.directory.resolve("output.xml"));

    try (var serializer = this.serializers()
      .create(URI.create("urn:unknown"), output, cf0)) {
      serializer.execute();
    }

    final var cf1 = this.parseTempFile("output.xml");
    Assertions.assertEquals(cf0, cf1);
  }

  private WXMClientConfiguration parseTempFile(
    final String name)
    throws IOException
  {
    try (var stream = Files.newInputStream(this.directory.resolve(name))) {
      try (var parser = this.parsers().create(
        FileSystems.getDefault(),
        URI.create("urn:unknown"),
        stream, this::logError)) {
        final var result = parser.parse();
        return result.get();
      }
    }
  }

  private WXMClientConfiguration parseResource(
    final String name)
    throws IOException
  {
    try (var stream = WXMTestDirectories.resourceStreamOf(
      WXMClientConfigurationSerializerContract.class,
      this.directory,
      name)) {

      try (var parser = this.parsers().create(
        FileSystems.getDefault(),
        URI.create("urn:unknown"),
        stream, this::logError)) {
        final var result = parser.parse();
        return result.get();
      }
    }
  }

  private void logError(
    final WXMParseError error)
  {
    this.logger().debug("error: {}", error);
    this.errors.add(error);
  }
}
