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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WXMClientConfigurationParserContract
{
  private Path directory;
  private ArrayList<WXMParseError> errors;

  protected abstract Logger logger();

  protected abstract WXMClientConfigurationParserProviderType parsers();

  @BeforeEach
  public void testSetup()
    throws Exception
  {
    this.directory = WXMTestDirectories.createTempDirectory();
    this.errors = new ArrayList<>();
  }

  @Test
  public void exampleParses()
    throws Exception
  {
    final var configOpt = this.parseResource("config0.xml");
    assertTrue(configOpt.isPresent());
    final var config = configOpt.get();
    assertEquals(
      "/etc/waxmill/vm",
      config.virtualMachineConfigurationDirectory().toString());
  }

  private Optional<WXMClientConfiguration> parseResource(
    final String name)
    throws IOException
  {
    try (var stream = WXMTestDirectories.resourceStreamOf(
      WXMClientConfigurationParserContract.class,
      this.directory,
      name)) {

      try (var parser = this.parsers().create(
        FileSystems.getDefault(),
        URI.create("urn:unknown"),
        stream,
        this::logError)) {
        return parser.parse();
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
