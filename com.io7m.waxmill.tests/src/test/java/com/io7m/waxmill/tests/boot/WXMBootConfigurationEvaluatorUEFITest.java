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
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceFramebuffer;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMSectorSizes;
import com.io7m.waxmill.machines.WXMShortIDs;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceFramebufferType.WXMVGAConfiguration.OFF;
import static com.io7m.waxmill.machines.WXMOpenOption.NO_CACHE;
import static com.io7m.waxmill.machines.WXMOpenOption.READ_ONLY;
import static com.io7m.waxmill.machines.WXMOpenOption.SYNCHRONOUS;
import static com.io7m.waxmill.tests.WXMDeviceIDTest.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class WXMBootConfigurationEvaluatorUEFITest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMBootConfigurationEvaluatorUEFITest.class);

  private Path directory;
  private Path configs;
  private Path vms;
  private WXMClientConfiguration clientConfiguration;

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

    this.clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeFilesystem(
          WXMZFSFilesystem.builder()
            .setMountPoint(this.vms)
            .setName("storage/vm")
            .build())
        .build();
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


    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
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
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals(
      "0:0:0,ahci-hd,/tmp/file,nocache,direct,ro,sectorsize=2048/4096",
      lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,lpc", lastArgs.remove(0));
    assertEquals("-l", lastArgs.remove(0));
    assertEquals("com1,stdio", lastArgs.remove(0));
    assertEquals("-l", lastArgs.remove(0));
    assertEquals("bootrom,/tmp/firmware", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,ahci-hd,/tmp/file,nocache,direct,ro,sectorsize=2048/4096 -s 0:1:0,lpc -l com1,stdio -l bootrom,/tmp/firmware %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
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
            .setListenAddress(Inet6Address.getByAddress(new byte[]{
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

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
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
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,lpc", lastArgs.remove(0));
    assertEquals("-l", lastArgs.remove(0));
    assertEquals("com1,stdio", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals(
      "0:2:0,fbuf,tcp=[0:0:0:0:0:0:0:1]:5901,w=1200,h=1400,vga=off,wait",
      lastArgs.remove(0));
    assertEquals("-l", lastArgs.remove(0));
    assertEquals("bootrom,/tmp/firmware", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:1:0,lpc -l com1,stdio -s 0:2:0,fbuf,tcp=[0:0:0:0:0:0:0:1]:5901,w=1200,h=1400,vga=off,wait -l bootrom,/tmp/firmware %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
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
            .setListenAddress(Inet4Address.getByAddress(new byte[]{
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

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
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
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,lpc", lastArgs.remove(0));
    assertEquals("-l", lastArgs.remove(0));
    assertEquals("com1,stdio", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals(
      "0:2:0,fbuf,tcp=127.0.0.1:5901,w=1200,h=1400,vga=off,wait",
      lastArgs.remove(0));
    assertEquals("-l", lastArgs.remove(0));
    assertEquals("bootrom,/tmp/firmware", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:1:0,lpc -l com1,stdio -s 0:2:0,fbuf,tcp=127.0.0.1:5901,w=1200,h=1400,vga=off,wait -l bootrom,/tmp/firmware %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }
}
