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
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMBootConfigurationUEFI;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandVMRunTest
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
        .setZfsExecutable(Paths.get("/bin/echo"))
        .setGrubBhyveExecutable(Paths.get("/bin/echo"))
        .setBhyveExecutable(Paths.get("/bin/echo"))
        .build();

    new WXMClientConfigurationSerializers()
      .serialize(
        this.configFile,
        this.configFileTmp,
        this.configuration
      );
  }

  @Test
  public void runTooFewArguments()
  {
    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-run"
        }
      );
    });
  }

  @Test
  public void runConfigurationFileMissing()
    throws IOException
  {
    Files.deleteIfExists(this.configFile);

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-run",
          "--verbose",
          "trace",
          "--boot-configuration",
          "run",
          "--machine",
          UUID.randomUUID().toString(),
          "--configuration",
          this.configFile.toString()
        }
      );
    });
  }

  @Test
  public void runDryRun()
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
        "--machine",
        id.toString(),
        "--name",
        "com.io7m.example",
        "--memory-gigabytes",
        "1",
        "--memory-megabytes",
        "128",
        "--cpu-count",
        "2"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-add-lpc-device",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--device-slot",
        "0:2:0",
        "--add-backend",
        "stdio;com1"
      }
    );

    Files.write(this.directory.resolve("firmware"), "FIRMWARE".getBytes(UTF_8));

    final var bootConf =
      WXMBootConfigurationUEFI.builder()
        .setComment("A configuration")
        .setName(WXMBootConfigurationName.of("run"))
        .setFirmware(this.directory.resolve("firmware"))
        .build();

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

    MainExitless.main(
      new String[]{
        "vm-run",
        "--verbose",
        "trace",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString(),
        "--dry-run",
        "true",
        "--boot-configuration",
        "run"
      }
    );
  }

  @Test
  public void runMissingBoot()
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
        "--machine",
        id.toString(),
        "--name",
        "com.io7m.example",
        "--memory-gigabytes",
        "1",
        "--memory-megabytes",
        "128",
        "--cpu-count",
        "2"
      }
    );

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-run",
          "--verbose",
          "trace",
          "--machine",
          id.toString(),
          "--configuration",
          this.configFile.toString(),
          "--dry-run",
          "true",
          "--boot-configuration",
          "run"
        }
      );
    });
  }

  @Test
  public void runMissingMachine()
  {
    final var id = UUID.randomUUID();

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-kill",
          "--verbose",
          "trace",
          "--machine",
          id.toString(),
          "--configuration",
          this.configFile.toString(),
          "--dry-run",
          "true"
        }
      );
    });
  }
}
