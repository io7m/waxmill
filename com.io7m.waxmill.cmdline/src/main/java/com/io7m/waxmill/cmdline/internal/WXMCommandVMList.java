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

import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.waxmill.machines.WXMTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-list" command.
 */

@Parameters(commandDescription = "List the available virtual machines.")
public final class WXMCommandVMList extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMList.class);

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMList(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-list";
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmList");
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machineSet = client.vmList();
      if (machineSet.machines().isEmpty()) {
        return SUCCESS;
      }

      System.out.printf("# %-40s %-16s %-16s%n", "ID", "Name", "Tags");
      for (final var entry : machineSet.machines().entrySet()) {
        final var id = entry.getKey();
        final var machine = entry.getValue();
        final var machineName = machine.name().value();
        final var machineTags =
          machine.tags()
            .stream()
            .map(WXMTag::value)
            .collect(Collectors.joining(",")
            );

        System.out.printf("%-40s %-16s %s%n", id, machineName, machineTags);
      }
    }
    return SUCCESS;
  }
}
