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
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

@Parameters(commandDescription = "Export virtual machine descriptions.")
public final class WXMCommandVMExport extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMExport.class);

  @Parameter(
    names = "--machine",
    description = "The ID(s) of the virtual machine",
    converter = WXMUUIDConverter.class
  )
  private List<UUID> ids = List.of();

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMExport(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-export";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machines = new TreeMap<UUID, WXMVirtualMachine>();
      for (final var id : this.ids) {
        final var virtualMachine = client.vmFind(id);
        machines.put(virtualMachine.id(), virtualMachine);
      }
      final var set =
        WXMVirtualMachineSet.builder()
          .setMachines(machines)
          .build();

      final var serializers = WXMServices.vmSerializers();

      try (var outputStream = new CloseShieldOutputStream(System.out)) {
        try (var serializer = serializers.create(
          URI.create("urn:stdout"), outputStream, set)) {
          serializer.execute();
        }
      }
    }
    return SUCCESS;
  }
}
