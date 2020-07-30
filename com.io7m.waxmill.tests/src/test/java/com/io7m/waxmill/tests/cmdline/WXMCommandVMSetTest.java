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

package com.io7m.waxmill.tests.cmdline;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.cmdline.MainExitless;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.io7m.waxmill.tests.cmdline.WXMParsing.parseFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMCommandVMSetTest
{
  private Path directory;
  private Path configFile;
  private Path configFileTmp;
  private Path vmDirectory;
  private Path zfsDirectory;
  private WXMClientConfiguration configuration;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory = WXMTestDirectories.createTempDirectory();
    this.configFile = this.directory.resolve("config.xml");
    this.configFileTmp = this.directory.resolve("config.xml.tmp");
    this.vmDirectory = this.directory.resolve("vmDirectory");
    this.zfsDirectory = this.directory.resolve("zfsDirectory");
    Files.createDirectories(this.vmDirectory);

    this.configuration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.vmDirectory)
        .setVirtualMachineRuntimeFilesystem(
          WXMZFSFilesystem.builder()
            .setMountPoint(this.zfsDirectory)
            .setName("storage/vm")
            .build()
        ).build();

    new WXMClientConfigurationSerializers()
      .serialize(
        this.configFile,
        this.configFileTmp,
        this.configuration
      );
  }

  @Test
  public void setWiredMemoryOK()
    throws Exception
  {
    final var id = UUID.randomUUID();

    MainExitless.main(
      new String[]{
        "vm-define",
        "--configuration",
        this.configFile.toString(),
        "--name",
        "com.io7m.example",
        "--memory-gigabytes",
        "1",
        "--memory-megabytes",
        "128",
        "--cpu-count",
        "2",
        "--machine",
        id.toString()
      }
    );

    final var flagsStart = this.getFlags();

    MainExitless.main(
      new String[]{
        "vm-set",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--wire-guest-memory",
        "true"
      }
    );

    final var flagsAfter = this.getFlags();
    assertTrue(flagsAfter.wireGuestMemory());
    final var flagsCheck = flagsAfter.withWireGuestMemory(false);
    assertEquals(flagsStart, flagsCheck);
  }

  @Test
  public void setAllTrue()
    throws Exception
  {
    final var id = UUID.randomUUID();

    MainExitless.main(
      new String[]{
        "vm-define",
        "--configuration",
        this.configFile.toString(),
        "--name",
        "com.io7m.example",
        "--memory-gigabytes",
        "1",
        "--memory-megabytes",
        "128",
        "--cpu-count",
        "2",
        "--machine",
        id.toString()
      }
    );

    MainExitless.main(
      new String[]{
        "vm-set",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--disable-mptable-generation", "true",
        "--exit-on-PAUSE", "true",
        "--force-msi-interrupts", "true",
        "--generate-acpi-tables", "true",
        "--guest-apic-is-x2apic", "true",
        "--ignore-unimplemented-msr", "true",
        "--include-guest-memory-cores", "true",
        "--rtc-is-utc", "true",
        "--wire-guest-memory", "true",
        "--yield-on-HLT", "true",
      }
    );

    final var flagsAfter = this.getFlags();
    assertTrue(flagsAfter.disableMPTableGeneration());
    assertTrue(flagsAfter.exitOnPAUSE());
    assertTrue(flagsAfter.forceVirtualIOPCIToUseMSI());
    assertTrue(flagsAfter.generateACPITables());
    assertTrue(flagsAfter.guestAPICIsX2APIC());
    assertTrue(flagsAfter.ignoreUnimplementedModelSpecificRegisters());
    assertTrue(flagsAfter.includeGuestMemoryInCoreFiles());
    assertTrue(flagsAfter.realTimeClockIsUTC());
    assertTrue(flagsAfter.wireGuestMemory());
    assertTrue(flagsAfter.yieldCPUOnHLT());
  }

  @Test
  public void setAllFalse()
    throws Exception
  {
    final var id = UUID.randomUUID();

    MainExitless.main(
      new String[]{
        "vm-define",
        "--configuration",
        this.configFile.toString(),
        "--name",
        "com.io7m.example",
        "--memory-gigabytes",
        "1",
        "--memory-megabytes",
        "128",
        "--cpu-count",
        "2",
        "--machine",
        id.toString()
      }
    );

    MainExitless.main(
      new String[]{
        "vm-set",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--disable-mptable-generation", "false",
        "--exit-on-PAUSE", "false",
        "--force-msi-interrupts", "false",
        "--generate-acpi-tables", "false",
        "--guest-apic-is-x2apic", "false",
        "--ignore-unimplemented-msr", "false",
        "--include-guest-memory-cores", "false",
        "--rtc-is-utc", "false",
        "--wire-guest-memory", "false",
        "--yield-on-HLT", "false",
      }
    );

    final var flagsAfter = this.getFlags();
    assertFalse(flagsAfter.disableMPTableGeneration());
    assertFalse(flagsAfter.exitOnPAUSE());
    assertFalse(flagsAfter.forceVirtualIOPCIToUseMSI());
    assertFalse(flagsAfter.generateACPITables());
    assertFalse(flagsAfter.guestAPICIsX2APIC());
    assertFalse(flagsAfter.ignoreUnimplementedModelSpecificRegisters());
    assertFalse(flagsAfter.includeGuestMemoryInCoreFiles());
    assertFalse(flagsAfter.realTimeClockIsUTC());
    assertFalse(flagsAfter.wireGuestMemory());
    assertFalse(flagsAfter.yieldCPUOnHLT());
  }

  private WXMFlags getFlags()
    throws IOException, WXMException
  {
    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    return machine.flags();
  }

  @Test
  public void setNonexistentVirtualMachine()
  {
    assertThrows(IOException.class, () -> {
      final var id = UUID.randomUUID();
      MainExitless.main(
        new String[]{
          "vm-set",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--machine",
          id.toString()
        }
      );
    });
  }
}
