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
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMShortIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-id-of" command.
 */

@Parameters(commandDescription = "Find the ID of a virtual machine")
public final class WXMCommandVMIDOf extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMIDOf.class);

  @Parameter(
    names = "--name",
    description = "The name of the virtual machine",
    converter = WXMMachineNameConverter.class,
    required = true
  )
  private WXMMachineName name;

  @Parameter(
    names = "--short",
    description = "Print the ID as a short ID",
    arity = 1,
    required = false
  )
  private boolean shortId;

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmIDOfHelp");
  }

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMIDOf(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-id-of";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machines = client.vmList();
      for (final var machine : machines.machines().values()) {
        if (Objects.equals(machine.name(), this.name)) {
          final var machineId = machine.id();
          if (this.shortId) {
            System.out.println(WXMShortIDs.encode(machineId));
          } else {
            System.out.println(machineId);
          }
          return SUCCESS;
        }
      }
    }

    LOG.error(
      "{}",
      this.messages().format("errorNoMachinesWithName", this.name.value())
    );
    return FAILURE;
  }
}
