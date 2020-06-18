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
import com.beust.jcommander.JCommander.ProgramName;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

public final class WXMBriefUsageFormatter extends DefaultUsageFormatter
{
  private final JCommander commander;

  public WXMBriefUsageFormatter(
    final JCommander inCommander)
  {
    super(inCommander);
    this.commander =
      Objects.requireNonNull(inCommander, "commander");
  }

  @Override
  public void appendCommands(
    final StringBuilder out,
    final int indentCount,
    final int descriptionIndent,
    final String indent)
  {
    out.append('\n');
    out.append(indent + "  Commands:\n");

    final var rawCommands = this.commander.getRawCommands();
    final var commandNames = new ArrayList<>(rawCommands.keySet());
    commandNames.sort(Comparator.comparing(ProgramName::getDisplayName));

    for (final var commandName : commandNames) {
      final var commands = rawCommands.get(commandName);
      final Object arg = commands.getObjects().get(0);
      final Parameters p = arg.getClass().getAnnotation(Parameters.class);

      if (p == null || !p.hidden()) {
        final String description =
          String.format(
            "    %-32s %s",
            commandName.getDisplayName(),
            this.getCommandDescription(commandName.getName())
          );

        out.append(description);
        out.append('\n');
      }
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMBriefUsageFormatter 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
