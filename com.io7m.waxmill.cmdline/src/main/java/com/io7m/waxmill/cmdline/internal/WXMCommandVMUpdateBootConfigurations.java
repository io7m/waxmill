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
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

@Parameters(commandDescription = "Add/replace boot configurations in a virtual machine.")
public final class WXMCommandVMUpdateBootConfigurations
  extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMUpdateBootConfigurations.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--file",
    description = "A file containing boot configurations",
    required = true
  )
  private Path file;

  @Parameter(
    names = "--update",
    description = "Update existing boot configurations"
  )
  private boolean update;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMUpdateBootConfigurations(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-update-boot-configurations";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    final var parsers =
      WXMServices.bootConfigurationParsers();
    final var namesAdded =
      new HashSet<WXMBootConfigurationName>();
    final var namesUpdated =
      new HashSet<WXMBootConfigurationName>();

    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);
      final var bootConfigurations = parsers.parse(this.file);

      this.info(
        "infoBootConfigurationsParsed",
        Integer.valueOf(bootConfigurations.size()));

      final var machineBuilder =
        WXMVirtualMachine.builder()
          .from(machine);

      var failed = false;
      final var currentConfigurations =
        new HashMap<>(machine.bootConfigurationMap());

      for (final var bootConfiguration : bootConfigurations) {
        final var bootName = bootConfiguration.name();
        if (currentConfigurations.containsKey(bootName)) {
          namesUpdated.add(bootName);

          if (this.update) {
            currentConfigurations.put(bootName, bootConfiguration);
          } else {
            this.error("errorBootConfigurationAlreadyExists", bootName.value());
            failed = true;
          }
        } else {
          namesAdded.add(bootName);
          currentConfigurations.put(bootName, bootConfiguration);
        }
      }

      if (failed) {
        return FAILURE;
      }

      machineBuilder.setBootConfigurations(currentConfigurations.values());
      client.vmUpdate(machineBuilder.build());
    }

    for (final var nameUpdated : namesUpdated) {
      this.info("infoBootConfigurationUpdated", nameUpdated.value());
    }
    for (final var nameAdded : namesAdded) {
      this.info("infoBootConfigurationAdded", nameAdded.value());
    }
    return SUCCESS;
  }
}
