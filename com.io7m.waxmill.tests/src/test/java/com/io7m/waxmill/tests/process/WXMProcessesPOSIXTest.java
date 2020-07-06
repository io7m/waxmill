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

package com.io7m.waxmill.tests.process;

import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.posix.WXMProcessesPOSIX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public final class WXMProcessesPOSIXTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMProcessesPOSIXTest.class);

  @Test
  @Disabled("Cannot execute this test in the same JVM")
  public void testExecute()
    throws IOException
  {
    final var processes = WXMProcessesPOSIX.create();

    processes.processReplaceCurrent(
      WXMProcessDescription.builder()
        .setExecutable(Paths.get("/bin/ls"))
        .addArguments("-a")
        .addArguments("-l")
        .addArguments("-F")
        .build()
    );
  }

  @Test
  public void testExecuteLs()
    throws IOException, InterruptedException
  {
    final var processes = WXMProcessesPOSIX.create();

    final var process =
      processes.processStart(
        WXMProcessDescription.builder()
          .setExecutable(Paths.get("/bin/ls"))
          .addArguments("-a")
          .addArguments("-l")
          .addArguments("-F")
          .build()
      );

    final var buffer = new ByteArrayOutputStream();
    try (InputStream output = process.getInputStream()) {
      output.transferTo(buffer);
    }
    final var text = new String(buffer.toByteArray());
    process.waitFor(5L, TimeUnit.MINUTES);
    LOG.debug("text: {}", text);
    Assertions.assertTrue(text.length() > 5);
    Assertions.assertEquals(0, process.exitValue());
  }

  @Test
  public void testExecuteLsAndWait()
    throws IOException
  {
    final var processes = WXMProcessesPOSIX.create();

    processes.processStartAndWait(
      WXMProcessDescription.builder()
        .setExecutable(Paths.get("/bin/ls"))
        .addArguments("-a")
        .addArguments("-l")
        .addArguments("-F")
        .build()
    );
  }
}
