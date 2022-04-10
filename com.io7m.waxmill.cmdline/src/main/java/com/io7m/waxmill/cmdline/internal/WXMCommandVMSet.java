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
import com.io7m.waxmill.machines.WXMFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-set" command.
 */

@Parameters(commandDescription = "Set virtual machine configuration flags.")
public final class WXMCommandVMSet extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMSet.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--wire-guest-memory",
    description = "Enable/disable wiring of guest memory.",
    arity = 1
  )
  private Boolean wireGuestMemory;

  @Parameter(
    names = "--include-guest-memory-cores",
    description = "Include guest memory in core files.",
    arity = 1
  )
  private Boolean includeGuestMemoryInCoreFiles;

  @Parameter(
    names = "--yield-on-HLT",
    description = "Yield the virtual CPU thread when a HLT instruction is detected.",
    arity = 1
  )
  private Boolean yieldOnHLT;

  @Parameter(
    names = "--exit-on-PAUSE",
    description = "Force the guest virtual CPU to exit when a PAUSE instruction is detected.",
    arity = 1
  )
  private Boolean exitOnPAUSE;

  @Parameter(
    names = "--generate-acpi-tables",
    description = "Generate ACPI tables. Required for FreeBSD/amd64 guests.",
    arity = 1
  )
  private Boolean generateACPITables;

  @Parameter(
    names = "--disable-mptable-generation",
    description = "Disable MP table generation.",
    arity = 1
  )
  private Boolean disableMPTableGeneration;

  @Parameter(
    names = "--force-msi-interrupts",
    description = "Force virtio PCI device emulations to use MSI interrupts instead of MSI-X interrupts.",
    arity = 1
  )
  private Boolean forceMSIInterrupts;

  @Parameter(
    names = "--guest-apic-is-x2apic",
    description = "The guest's local APIC is configured in x2APIC mode.",
    arity = 1
  )
  private Boolean guestAPICIsX2APIC;

  @Parameter(
    names = "--rtc-is-utc",
    description = "RTC keeps UTC time.",
    arity = 1
  )
  private Boolean realTimeClockIsUTC;

  @Parameter(
    names = "--ignore-unimplemented-msr",
    description = "Ignore accesses to unimplemented Model Specific Registers.",
    arity = 1
  )
  private Boolean ignoreUnimplementedModelSpecificRegisters;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMSet(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmSetHelp");
  }

  @Override
  public String name()
  {
    return "vm-set";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);

      final var flagBuilder =
        WXMFlags.builder()
          .from(machine.flags());

      handleFlag(
        this.disableMPTableGeneration,
        flagBuilder::setDisableMPTableGeneration
      );
      handleFlag(
        this.exitOnPAUSE,
        flagBuilder::setExitOnPAUSE
      );
      handleFlag(
        this.forceMSIInterrupts,
        flagBuilder::setForceVirtualIOPCIToUseMSI
      );
      handleFlag(
        this.generateACPITables,
        flagBuilder::setGenerateACPITables
      );
      handleFlag(
        this.guestAPICIsX2APIC,
        flagBuilder::setGuestAPICIsX2APIC
      );
      handleFlag(
        this.ignoreUnimplementedModelSpecificRegisters,
        flagBuilder::setIgnoreUnimplementedModelSpecificRegisters
      );
      handleFlag(
        this.includeGuestMemoryInCoreFiles,
        flagBuilder::setIncludeGuestMemoryInCoreFiles
      );
      handleFlag(
        this.realTimeClockIsUTC,
        flagBuilder::setRealTimeClockIsUTC
      );
      handleFlag(
        this.wireGuestMemory,
        flagBuilder::setWireGuestMemory
      );
      handleFlag(
        this.yieldOnHLT,
        flagBuilder::setYieldCPUOnHLT
      );

      client.vmUpdate(machine.withFlags(flagBuilder.build()));
    }
    return SUCCESS;
  }

  interface FlagSetterType
  {
    void set(boolean flag);
  }

  private static void handleFlag(
    final Boolean flag,
    final FlagSetterType flagSetter)
  {
    if (flag != null) {
      flagSetter.set(flag.booleanValue());
    }
  }
}
