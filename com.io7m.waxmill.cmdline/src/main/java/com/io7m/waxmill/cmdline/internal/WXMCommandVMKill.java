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

package com.io7m.waxmill.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPCommandContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;
import static com.io7m.waxmill.machines.WXMDryRun.DRY_RUN;
import static com.io7m.waxmill.machines.WXMDryRun.EXECUTE;

/**
 * The "vm-kill" command.
 */

@Parameters(commandDescription = "Kill a virtual machine.")
public final class WXMCommandVMKill extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMKill.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--dry-run",
    description = "Show the commands that would be executed, but do not execute them.",
    required = false,
    arity = 1
  )
  private boolean dryRun;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMKill(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmKillHelp");
  }

  @Override
  public String name()
  {
    return "vm-kill";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);
      client.vmKill(
        machine,
        this.dryRun ? DRY_RUN : EXECUTE
      );
    }
    return SUCCESS;
  }
}
