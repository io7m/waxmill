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

package com.io7m.waxmill.tests.boot;

import com.io7m.waxmill.boot.WXMBootConfigurationEvaluator;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptionUnsatisfiedRequirement;
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceFramebuffer;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceFramebufferType.WXMVGAConfiguration;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMSectorSizes;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.tests.WXMTestDirectories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceFramebufferType.WXMVGAConfiguration.*;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_AMD;
import static com.io7m.waxmill.machines.WXMOpenOption.NO_CACHE;
import static com.io7m.waxmill.machines.WXMOpenOption.READ_ONLY;
import static com.io7m.waxmill.machines.WXMOpenOption.SYNCHRONOUS;
import static com.io7m.waxmill.tests.WXMDeviceIDTest.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMBootConfigurationEvaluatorUEFITest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMBootConfigurationEvaluatorUEFITest.class);

  private Path directory;
  private Path configs;
  private Path vms;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      WXMTestDirectories.createTempDirectory();
    this.configs =
      this.directory.resolve("configs");
    this.vms =
      this.directory.resolve("vms");
  }

  @Test
  public void openbsdSimpleAHCIHD()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationUEFI.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setFirmware(Paths.get("/tmp/firmware"))
            .build()
        )
        .addDevices(
          WXMDeviceLPC.builder()
            .setDeviceSlot(convert("0:1:0"))
            .addBackends(
              WXMTTYBackendStdio.builder()
                .setDevice("com1")
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .setOptions(Set.of(NO_CACHE, READ_ONLY, SYNCHRONOUS))
                .setSectorSizes(
                  WXMSectorSizes.builder()
                    .setLogical(BigInteger.valueOf(2048L))
                    .setPhysical(BigInteger.valueOf(4096L))
                    .build())
                .build()
            ).build()
        ).build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationUEFI) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    assertEquals(0, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -P -A -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,ahci-hd,/tmp/file,nocache,direct,ro,sectorsize=2048/4096 -s 0:1:0,lpc -l com1,stdio -l bootrom,/tmp/firmware %s",
        machine.id()),
      lastExec.toString()
    );
  }

  @Test
  public void openbsdVNC6()
    throws WXMException, UnknownHostException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationUEFI.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setFirmware(Paths.get("/tmp/firmware"))
            .build()
        )
        .addDevices(
          WXMDeviceFramebuffer.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setHeight(1400)
            .setListenAddress(Inet6Address.getByAddress(new byte[] {
              0, 0, 0, 0,
              0, 0, 0, 0,
              0, 0, 0, 0,
              0, 0, 0, 1
            }))
            .setListenPort(5901)
            .setVgaConfiguration(OFF)
            .setWaitForVNC(true)
            .setWidth(1200)
            .build()
        )
        .addDevices(
          WXMDeviceLPC.builder()
            .setDeviceSlot(convert("0:1:0"))
            .addBackends(
              WXMTTYBackendStdio.builder()
                .setDevice("com1")
                .build())
            .build()
        ).build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationUEFI) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    assertEquals(0, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -P -A -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:1:0,lpc -l com1,stdio -s 0:2:0,fbuf,tcp=[0:0:0:0:0:0:0:1]:5901,w=1200,h=1400,vga=off,wait -l bootrom,/tmp/firmware %s",
        machine.id()),
      lastExec.toString()
    );
  }

  @Test
  public void openbsdVNC4()
    throws WXMException, UnknownHostException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationUEFI.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setFirmware(Paths.get("/tmp/firmware"))
            .build()
        )
        .addDevices(
          WXMDeviceFramebuffer.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setHeight(1400)
            .setListenAddress(Inet4Address.getByAddress(new byte[] {
              0x7f, 0, 0, 1
            }))
            .setListenPort(5901)
            .setVgaConfiguration(OFF)
            .setWaitForVNC(true)
            .setWidth(1200)
            .build()
        )
        .addDevices(
          WXMDeviceLPC.builder()
            .setDeviceSlot(convert("0:1:0"))
            .addBackends(
              WXMTTYBackendStdio.builder()
                .setDevice("com1")
                .build())
            .build()
        ).build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationUEFI) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    assertEquals(0, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -P -A -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:1:0,lpc -l com1,stdio -s 0:2:0,fbuf,tcp=127.0.0.1:5901,w=1200,h=1400,vga=off,wait -l bootrom,/tmp/firmware %s",
        machine.id()),
      lastExec.toString()
    );
  }

  private static <T extends Throwable> T assertThrowsLogged(
    final Class<T> expectedType,
    final Executable executable)
  {
    final var ex = assertThrows(expectedType, executable);
    LOG.debug("", ex);
    return ex;
  }
}
