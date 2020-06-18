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
import com.io7m.waxmill.client.api.WXMCPUTopology;
import com.io7m.waxmill.client.api.WXMDeviceHostBridge;
import com.io7m.waxmill.client.api.WXMDeviceID;
import com.io7m.waxmill.client.api.WXMFlags;
import com.io7m.waxmill.client.api.WXMMachineName;
import com.io7m.waxmill.client.api.WXMMemory;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.waxmill.client.api.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_UNSPECIFIED;
import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.FAILURE;
import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.SUCCESS;
import static com.io7m.waxmill.cmdline.internal.WXMEnvironment.checkConfigurationPath;

@Parameters(commandDescription = "Define a new virtual machine.")
public final class WXMCommandVMDefine extends WXMCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMDefine.class);

  @Parameter(
    names = "--configuration",
    description = "The path to the configuration file (environment variable: $WAXMILL_CONFIGURATION_FILE)",
    required = false
  )
  private Path configurationFile = WXMEnvironment.configurationFile();

  @Parameter(
    names = "--id",
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

  public WXMCommandVMDefine()
  {

  }

  @Override
  public Status execute()
    throws Exception
  {
    if (super.execute() == FAILURE) {
      return FAILURE;
    }
    if (!checkConfigurationPath(LOG, this.configurationFile)) {
      return FAILURE;
    }

    if (this.id == null) {
      this.id = UUID.randomUUID();
    }

    final var machineName = WXMMachineName.of(this.name);
    try (var client = WXMServices.clients().open(this.configurationFile)) {
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
          .setId(WXMDeviceID.of(0))
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
