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
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.io7m.waxmill.tests.WXMExceptions.assertThrowsLogged;

public final class WXMCommandVMConsoleTest
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
        )
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
    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-console"
        }
      );
    });
  }

  @Test
  public void runConfigurationFileMissing()
    throws IOException
  {
    Files.deleteIfExists(this.configFile);

    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-console",
          "--verbose",
          "trace",
          "--machine",
          UUID.randomUUID().toString(),
          "--configuration",
          this.configFile.toString()
        }
      );
    });
  }

  @Test
  public void runNoConsoleDevice()
    throws IOException
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

    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-console",
          "--verbose",
          "trace",
          "--machine",
          id.toString(),
          "--configuration",
          this.configFile.toString()
        }
      );
    });
  }

  @Test
  public void runNoConsoleDeviceDryRun()
    throws IOException
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
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--add-backend",
        "file;com1;/tmp/xyz",
        "--device-slot",
        "0:1:0"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-console",
        "--verbose",
        "trace",
        "--dry-run",
        "true",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }
}
