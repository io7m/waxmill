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
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
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

import static com.io7m.waxmill.tests.cmdline.WXMParsing.parseFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandVMUpdateBootConfigurationsTest
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
        .setVirtualMachineRuntimeDirectory(this.zfsDirectory)
        .build();

    new WXMClientConfigurationSerializers()
      .serialize(
        this.configFile,
        this.configFileTmp,
        this.configuration
      );
  }

  @Test
  public void addOK()
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
        "--id",
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
        "--id",
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
            .setBootDevice(
              WXMDeviceSlot.builder()
                .setBusID(0)
                .setFunctionID(0)
                .setSlotID(1)
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
        "--id",
        id.toString(),
        "--file",
        this.directory.resolve("boot.xml").toString()
      }
    );

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var bootReceived =
      (WXMBootConfigurationGRUBBhyve) machine.bootConfigurations().get(0);

    assertEquals(bootConf, bootReceived);
  }

  @Test
  public void addConflict()
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
        "--id",
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
        "--id",
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
            .setBootDevice(
              WXMDeviceSlot.builder()
                .setBusID(0)
                .setFunctionID(0)
                .setSlotID(1)
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
        "--id",
        id.toString(),
        "--file",
        this.directory.resolve("boot.xml").toString()
      }
    );

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-update-boot-configurations",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--id",
          id.toString(),
          "--file",
          this.directory.resolve("boot.xml").toString()
        }
      );
    });
  }

  @Test
  public void replaceOK()
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
        "--id",
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
        "--id",
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
            .setBootDevice(
              WXMDeviceSlot.builder()
                .setBusID(0)
                .setFunctionID(0)
                .setSlotID(1)
                .build())
            .build()
        ).build();

    new WXMBootConfigurationsSerializers()
      .serialize(
        this.directory.resolve("boot.xml"),
        this.directory.resolve("boot.xml.tmp"),
        List.of(bootConf)
      );

    MainExitless.main(
      new String[]{
        "vm-update-boot-configurations",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--id",
        id.toString(),
        "--file",
        this.directory.resolve("boot.xml").toString()
      }
    );

    final var bootConfNew = bootConf.withComment("Updated!");

    new WXMBootConfigurationsSerializers()
      .serialize(
        this.directory.resolve("boot.xml"),
        this.directory.resolve("boot.xml.tmp"),
        List.of(bootConfNew)
      );

    MainExitless.main(
      new String[]{
        "vm-update-boot-configurations",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--id",
        id.toString(),
        "--file",
        this.directory.resolve("boot.xml").toString(),
        "--update"
      }
    );

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var bootReceived =
      (WXMBootConfigurationGRUBBhyve) machine.bootConfigurations().get(0);

    assertEquals(bootConfNew, bootReceived);
    assertNotEquals(bootConf, bootReceived);
  }
}
