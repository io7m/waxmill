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
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

@Parameters(commandDescription = "List the virtual machines with the given name.")
public final class WXMCommandVMListWithName extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMListWithName.class);

  @Parameter(
    names = "--name",
    description = "The name of the virtual machine",
    required = true,
    converter = WXMMachineNameConverter.class
  )
  private WXMMachineName name;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMListWithName(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-list-with-name";
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmListWithNameHelp");
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    var found = false;
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machineSet = client.vmList();
      found = this.showMachines(machineSet);
    }

    if (!found) {
      this.error("errorNoMachinesWithName", this.name.value());
      return FAILURE;
    }
    return SUCCESS;
  }

  private boolean showMachines(
    final WXMVirtualMachineSet machineSet)
  {
    var found = false;
    for (final var entry : machineSet.machines().entrySet()) {
      final var id = entry.getKey();
      final var machine = entry.getValue();
      if (Objects.equals(machine.name(), this.name)) {
        System.out.printf("%s%n", id);
        found = true;
      }
    }
    return found;
  }
}
