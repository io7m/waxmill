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
import com.io7m.waxmill.machines.WXMMachineMessages;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.machines.WXMVirtualMachineSets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-import" command.
 */

@Parameters(commandDescription = "Import virtual machine descriptions.")
public final class WXMCommandVMImport extends
  WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMImport.class);

  @Parameter(
    names = "--file",
    description = "Files containing virtual machine descriptions",
    required = false
  )
  private List<Path> files = List.of();

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMImport(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-import";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    final var parsers = WXMServices.vmParsers();
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machineSets = new ArrayList<WXMVirtualMachineSet>();
      for (final var path : this.files) {
        machineSets.add(parsers.parse(path));
      }
      final var set =
        WXMVirtualMachineSets.merge(
          WXMMachineMessages.create(),
          machineSets
        );
      client.vmDefineAll(set);

      this.info(
        "infoImportedMachines",
        Integer.valueOf(set.machines().size()));
    }
    return SUCCESS;
  }
}
