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
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMBootConfigurationsSerializers;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static com.io7m.waxmill.tests.WXMExceptions.assertThrowsLogged;
import static com.io7m.waxmill.tests.cmdline.WXMParsing.parseFirst;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMCommandVMDeleteDeviceTest
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
  public void addDeleteDiskOK()
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
        "vm-add-virtio-disk",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "file;/tmp/xyz",
        "--device-slot",
        "0:1:0"
      }
    );

    final var machineSet0 =
      parseFirst(this.vmDirectory);

    final var machineBefore =
      machineSet0.machines()
        .values()
        .iterator()
        .next();

    MainExitless.main(
      new String[]{
        "vm-delete-devices",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--device-slot",
        "0:1:0",
        "--device-slot",
        "0:0:0"
      }
    );

    final var machineSet1 =
      parseFirst(this.vmDirectory);

    final var machineAfter =
      machineSet1.machines()
        .values()
        .iterator()
        .next();

    assertNotEquals(machineBefore, machineAfter);
    assertTrue(machineAfter.deviceMap().isEmpty());
  }

  @Test
  public void deleteNonexistent()
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

    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-delete-devices",
          "--verbose", "trace",
          "--configuration", this.configFile.toString(),
          "--machine", id.toString(),
          "--device-slot", "0:1:0",
          "--device-slot", "0:0:0"
        }
      );
    });

    final var machineSet0 =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet0.machines()
        .values()
        .iterator()
        .next();

    assertTrue(
      machine.deviceMap()
        .containsKey(
          WXMDeviceSlot.builder()
            .setSlotID(0)
            .setBusID(0)
            .setFunctionID(0)
            .build())
    );
  }

  @Test
  public void deleteBootReferenced()
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
        "vm-add-ahci-disk",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "file;/tmp/xyz",
        "--device-slot",
        "0:1:0"
      }
    );

    final var bootConf =
      WXMBootConfigurationGRUBBhyve.builder()
        .setComment("A configuration")
        .setName(WXMBootConfigurationName.of("install"))
        .setKernelInstructions(
          WXMGRUBKernelOpenBSD.builder()
            .setKernelPath(Paths.get("/bsd"))
            .setPartition("openbsd1")
            .setBootDevice(
              WXMDeviceSlot.builder()
                .setBusID(0)
                .setSlotID(1)
                .setFunctionID(0)
                .build())
            .build()
        ).build();

    final List<WXMBootConfigurationType> bootConfs =
      List.of(bootConf);

    new WXMBootConfigurationsSerializers()
      .serialize(
        this.directory.resolve("boot.xml"),
        this.directory.resolve("boot.xml.tmp"),
        bootConfs
      );

    MainExitless.main(
      new String[]{
        "vm-update-boot-configurations",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--file",
        this.directory.resolve("boot.xml").toString()
      }
    );

    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-delete-devices",
          "--verbose", "trace",
          "--configuration", this.configFile.toString(),
          "--machine", id.toString(),
          "--device-slot", "0:1:0"
        }
      );
    });
  }
}
