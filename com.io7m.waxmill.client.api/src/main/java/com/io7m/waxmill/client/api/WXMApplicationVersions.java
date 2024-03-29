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

package com.io7m.waxmill.client.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Functions relating to application versions.
 */

public final class WXMApplicationVersions
{
  private WXMApplicationVersions()
  {

  }

  /**
   * Load application version information from the given stream.
   *
   * @param stream The stream
   *
   * @return Application version information
   *
   * @throws IOException On errors
   */

  public static WXMApplicationVersion ofStream(
    final InputStream stream)
    throws IOException
  {
    Objects.requireNonNull(stream, "stream");

    final Properties properties = new Properties();
    properties.load(stream);
    return ofProperties(properties);
  }

  /**
   * Load application version information from the given stream.
   *
   * @param properties The properties
   *
   * @return Application version information
   */

  public static WXMApplicationVersion ofProperties(
    final Properties properties)
  {
    Objects.requireNonNull(properties, "properties");

    final String name =
      properties.getProperty("applicationName");
    final String version =
      properties.getProperty("applicationVersion");
    final String build =
      properties.getProperty("applicationBuild");

    if (name != null && version != null && build != null) {
      return WXMApplicationVersion.builder()
        .setApplicationName(name)
        .setApplicationVersion(version)
        .setApplicationBuild(build)
        .build();
    }

    throw new IllegalArgumentException(
      "Must specify applicationName, applicationVersion, applicationBuild fields"
    );
  }
}
