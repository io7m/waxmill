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

package com.io7m.waxmill.tests.cmdline;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.cmdline.MainExitless;
import com.io7m.waxmill.exceptions.WXMExceptionDuplicate;
import com.io7m.waxmill.exceptions.WXMExceptionNonexistent;
import com.io7m.waxmill.machines.WXMDevicePassthru;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.io7m.waxmill.tests.WXMExceptions.assertThrowsCauseLogged;
import static com.io7m.waxmill.tests.cmdline.WXMParsing.parseFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class WXMCommandVMAddPassthruTest
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
  public void addPassthruOK()
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
        "--wire-guest-memory",
        "true"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-add-passthru-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--device-slot",
        "0:1:0",
        "--host-device-slot",
        "1:2:3"
      }
    );

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var passthru =
      (WXMDevicePassthru) machine.devices().get(1);

    assertEquals("0:1:0", passthru.deviceSlot().toString());
    assertEquals("1:2:3", passthru.hostPCISlot().toString());
  }

  @Test
  public void addPassthruNonexistentVirtualMachine()
  {
    assertThrowsCauseLogged(IOException.class, WXMExceptionNonexistent.class, () -> {
      final var id = UUID.randomUUID();
      MainExitless.main(
        new String[]{
          "vm-add-passthru-device",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--machine",
          id.toString(),
          "--device-slot",
          "0:1:0",
          "--host-device-slot",
          "1:2:3"
        }
      );
    });
  }

  @Test
  public void addPassthruAlreadyUsed()
    throws Exception
  {
    final var id = UUID.randomUUID();

    MainExitless.main(
      new String[]{
        "vm-define",
        "--verbose",
        "trace",
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
        "--wire-guest-memory",
        "true"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-add-passthru-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--device-slot",
        "0:1:0",
        "--host-device-slot",
        "1:2:3"
      }
    );

    assertThrowsCauseLogged(IOException.class, WXMExceptionDuplicate.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-add-passthru-device",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--machine",
          id.toString(),
          "--device-slot",
          "0:1:0",
          "--host-device-slot",
          "1:2:3"
        }
      );
    });
  }

  @Test
  public void addPassthruAlreadyUsedReplace()
    throws Exception
  {
    final var id = UUID.randomUUID();

    MainExitless.main(
      new String[]{
        "vm-define",
        "--verbose",
        "trace",
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
        "--wire-guest-memory",
        "true"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-add-passthru-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--device-slot",
        "0:1:0",
        "--host-device-slot",
        "1:2:3"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-add-passthru-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--device-slot",
        "0:1:0",
        "--host-device-slot",
        "1:2:3",
        "--replace",
        "true"
      }
    );
  }
}
