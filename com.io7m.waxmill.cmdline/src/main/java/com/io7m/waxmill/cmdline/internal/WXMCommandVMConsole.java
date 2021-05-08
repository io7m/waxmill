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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.machines.WXMDeviceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-console" command.
 */

@Parameters(commandDescription = "Connect to the console of a virtual machine")
public final class WXMCommandVMConsole
  extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMConsole.class);

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

  public WXMCommandVMConsole(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmConsole");
  }

  @Override
  public String name()
  {
    return "vm-console";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    final var processes = WXMServices.processes();

    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);
      final var processOpt = client.vmConsole(machine);

      if (processOpt.isEmpty()) {
        this.error(
          "errorNoSingleConsole",
          this.id,
          machine.devices()
            .stream()
            .filter(WXMDeviceType::isConsoleDevice)
            .collect(Collectors.toList())
        );
        return FAILURE;
      }

      final var process = processOpt.get();
      if (this.dryRun) {
        System.out.printf(
          "%s %s%n",
          process.executable(),
          String.join(" ", process.arguments())
        );
        return SUCCESS;
      }

      processes.processReplaceCurrent(process);
      throw new UnreachableCodeException();
    }
  }
}
