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

package com.io7m.waxmill.tests;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.client.vanilla.WXMClients;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.xml.WXMVirtualMachineParsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMBootConfigurationUEFIType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMClientTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMClientTest.class);

  private WXMClients clients;
  private WXMClientConfiguration configuration;
  private Path directory;
  private Path directoryEtc;
  private Path directoryRun;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.clients =
      new WXMClients();
    this.directory =
      WXMTestDirectories.createTempDirectory();
    this.directoryEtc =
      this.directory.resolve("etc");
    this.directoryRun =
      this.directory.resolve("run");

    this.configuration =
      WXMClientConfiguration.builder()
        .setVirtualMachineRuntimeFilesystem(
          WXMZFSFilesystem.builder()
            .setMountPoint(this.directoryRun)
            .setName("storage/vm")
            .build()
        )
        .setVirtualMachineConfigurationDirectory(this.directoryEtc)
        .build();
  }

  @Test
  public void findConsoleOK()
    throws Exception
  {
    final var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var consoleOpt = client.vmConsoleGet(machine);
      assertTrue(consoleOpt.isPresent());
      final var console = consoleOpt.get();
      assertTrue(console.isConsoleDevice());
    }
  }

  @Test
  public void findConsoleOKRun()
    throws Exception
  {
    final var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var execOpt = client.vmConsole(machine);
      assertTrue(execOpt.isPresent());
      final var exec = execOpt.get();
      assertEquals(exec.executable(), this.configuration.cuExecutable());
    }
  }

  @Test
  public void findConsoleOKNMDMRun()
    throws Exception
  {
    var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    machine = machine.withDevices(
      machine.devices()
        .stream()
        .map(device -> {
          if (device instanceof WXMDeviceLPC) {
            return toNMDM((WXMDeviceLPC) device);
          }
          return device;
        })
        .collect(Collectors.toList())
    );

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var execOpt = client.vmConsole(machine);
      assertTrue(execOpt.isPresent());
      final var exec = execOpt.get();
      assertEquals(exec.executable(), this.configuration.cuExecutable());
    }
  }

  @Test
  public void findConsoleNone()
    throws Exception
  {
    var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    machine = machine.withBootConfigurations(
      machine.bootConfigurations()
        .stream()
        .filter(boot -> !(boot instanceof WXMBootConfigurationUEFIType))
        .collect(Collectors.toList())
    );

    machine = machine.withDevices(
      machine.devices()
        .stream()
        .filter(dev -> !dev.isConsoleDevice())
        .collect(Collectors.toList())
    );

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var consoleOpt = client.vmConsoleGet(machine);
      assertFalse(consoleOpt.isPresent());
    }
  }

  @Test
  public void findConsoleNoneRun()
    throws Exception
  {
    var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    machine = machine.withBootConfigurations(
      machine.bootConfigurations()
        .stream()
        .filter(boot -> !(boot instanceof WXMBootConfigurationUEFIType))
        .collect(Collectors.toList())
    );

    machine = machine.withDevices(
      machine.devices()
        .stream()
        .filter(dev -> !dev.isConsoleDevice())
        .collect(Collectors.toList())
    );

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var execOpt = client.vmConsole(machine);
      assertFalse(execOpt.isPresent());
    }
  }

  @Test
  public void findConsoleStdioBasedRun()
    throws Exception
  {
    var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    machine = machine.withDevices(
      machine.devices()
        .stream()
        .map(device -> {
          if (device instanceof WXMDeviceLPC) {
            return toStdioLPC((WXMDeviceLPC) device);
          }
          return device;
        })
        .collect(Collectors.toList())
    );

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var execOpt = client.vmConsole(machine);
      assertFalse(execOpt.isPresent());
    }
  }

  @Test
  public void findConsoleNoCom1Run()
    throws Exception
  {
    var machine =
      this.parseResource("vm0.xml")
        .orElseThrow()
        .machines()
        .values()
        .iterator()
        .next();

    machine = machine.withDevices(
      machine.devices()
        .stream()
        .map(device -> {
          if (device instanceof WXMDeviceLPC) {
            return toNothing((WXMDeviceLPC) device);
          }
          return device;
        })
        .collect(Collectors.toList())
    );

    try (var client = this.clients.open(this.configuration)) {
      client.vmDefine(machine);
      final var execOpt = client.vmConsole(machine);
      assertFalse(execOpt.isPresent());
    }
  }

  private static WXMDeviceLPC toNMDM(
    final WXMDeviceLPC device)
  {
    return WXMDeviceLPC.builder()
      .from(device)
      .setBackends(List.of(
        WXMTTYBackendNMDM.builder()
          .setDevice("com1")
          .build()
      )).build();
  }

  private static WXMDeviceType toNothing(
    final WXMDeviceLPC device)
  {
    return WXMDeviceLPC.builder()
      .from(device)
      .setBackends(List.of(
        WXMTTYBackendNMDM.builder()
          .setDevice("bootrom")
          .build()
      )).build();
  }

  private static WXMDeviceType toStdioLPC(
    final WXMDeviceLPC device)
  {
    return WXMDeviceLPC.builder()
      .from(device)
      .setBackends(List.of(
        WXMTTYBackendStdio.builder()
          .setDevice("com1")
          .build()
      )).build();
  }

  private Optional<WXMVirtualMachineSet> parseResource(
    final String name)
    throws IOException
  {
    try (var stream = WXMTestDirectories.resourceStreamOf(
      WXMClientTest.class,
      this.directory,
      name)) {

      try (var parser = new WXMVirtualMachineParsers().create(
        FileSystems.getDefault(),
        URI.create("urn:unknown"),
        stream,
        WXMClientTest::logError)) {
        return parser.parse();
      }
    }
  }

  private static void logError(
    final WXMParseError error)
  {
    LOG.debug("error: {}", error);
  }
}
