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

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

import java.util.Objects;

import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.FAILURE;
import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.SUCCESS;

@Parameters(commandDescription = "Show a detailed help message describing all available commands.")
public final class WXMCommandHelp extends WXMCommandRoot
{
  private final JCommander commander;

  public WXMCommandHelp(
    final JCommander inCommander)
  {
    this.commander = Objects.requireNonNull(inCommander, "commander");
  }

  @Override
  public Status execute()
    throws Exception
  {
    if (super.execute() == FAILURE) {
      return FAILURE;
    }

    final var console = new WXMStringBuilderConsole();
    this.commander.setUsageFormatter(new DefaultUsageFormatter(this.commander));
    this.commander.setConsole(console);
    this.commander.usage();

    System.err.println(console.builder());
    return SUCCESS;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMCommandHelp 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
