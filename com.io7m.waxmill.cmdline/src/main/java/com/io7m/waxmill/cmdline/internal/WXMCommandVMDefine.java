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
import com.io7m.waxmill.machines.WXMCPUTopology;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMMemory;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_UNSPECIFIED;

@Parameters(commandDescription = "Define a new virtual machine.")
public final class WXMCommandVMDefine extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMDefine.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the new virtual machine",
    required = false,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--name",
    description = "The name of the new virtual machine",
    required = true
  )
  private String name;

  @Parameter(
    names = "--memory-gigabytes",
    description = "The size in gigabytes of the virtual machine's memory (added to --memory-megabytes)",
    required = false
  )
  private long memoryGigabytes;

  @Parameter(
    names = "--memory-megabytes",
    description = "The size in megabytes of the virtual machine's memory (added to --memory-gigabytes)",
    required = false
  )
  private long memoryMegabytes = 250L;

  @Parameter(
    names = "--cpu-count",
    description = "The number of CPU cores in the virtual machine",
    required = false
  )
  private int cpuCores = 1;

  @Parameter(
    names = "--comment",
    description = "A comment describing the new virtual machine",
    required = false
  )
  private String comment;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMDefine(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-define";
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }

    final var machineName = WXMMachineName.of(this.name);
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var cpuTopology =
        WXMCPUTopology.builder()
          .setSockets(1)
          .setThreads(1)
          .setCores(this.cpuCores)
          .build();

      final var memory =
        WXMMemory.builder()
          .setGigabytes(BigInteger.valueOf(this.memoryGigabytes))
          .setMegabytes(BigInteger.valueOf(this.memoryMegabytes))
          .build();

      final var flags =
        WXMFlags.builder()
          .build();

      final var hostBridge =
        WXMDeviceHostBridge.builder()
          .setDeviceSlot(
            WXMDeviceSlot.builder()
              .setBusID(0)
              .setSlotID(0)
              .setFunctionID(0)
              .build())
          .setVendor(WXM_UNSPECIFIED)
          .build();

      final var machine =
        WXMVirtualMachine.builder()
          .setComment(Optional.ofNullable(this.comment).orElse(""))
          .setId(this.id)
          .setName(machineName)
          .setCpuTopology(cpuTopology)
          .setMemory(memory)
          .setFlags(flags)
          .addDevices(hostBridge)
          .build();

      client.vmDefine(machine);
    }
    return SUCCESS;
  }
}
