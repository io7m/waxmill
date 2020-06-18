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

package com.io7m.waxmill.cmdline.internal;

import org.slf4j.Logger;

import java.nio.file.Path;

public final class WXMEnvironment
{
  private WXMEnvironment()
  {

  }

  public static Path configurationFile()
  {
    final var path = System.getenv("WAXMILL_CONFIGURATION_FILE");
    if (path != null) {
      return Path.of(path).toAbsolutePath();
    }
    return null;
  }

  public static boolean checkConfigurationPath(
    final Logger logger,
    final Path configurationFile)
  {
    if (configurationFile == null) {
      logger.error(
        "A configuration file must be specified, either with --configuration "
          + "or the $WAXMILL_CONFIGURATION_FILE environment variable.");
      return false;
    }
    return true;
  }
}
