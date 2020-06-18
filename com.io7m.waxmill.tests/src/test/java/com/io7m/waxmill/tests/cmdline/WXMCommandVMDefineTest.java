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
import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.cmdline.MainExitless;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import com.io7m.waxmill.xml.WXMVirtualMachineParsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandVMDefineTest
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
        .setZfsVirtualMachineDirectory(this.zfsDirectory)
        .build();

    new WXMClientConfigurationSerializers()
      .serialize(
        this.configFile,
        this.configFileTmp,
        this.configuration
      );
  }

  @Test
  public void defineOK()
    throws IOException, WXMException
  {
    final var id = UUID.randomUUID();
    MainExitless.main(
      new String[]{
        "vm-define",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--id",
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

    final var machineSet =
      new WXMVirtualMachineParsers()
        .parse(this.vmDirectory.resolve(id + ".wvmx"));
    final var machine =
      machineSet.machines().get(id);

    assertEquals(id, machine.id());
  }

  @Test
  public void defineAlreadyExists()
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
        "--id",
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
          "vm-define",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--id",
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
    });
  }

  @Test
  public void defineConfigurationNonexistent()
    throws IOException
  {
    Files.deleteIfExists(this.configFile);

    assertThrows(IOException.class, () -> {
      final var id = UUID.randomUUID();
      MainExitless.main(
        new String[]{
          "vm-define",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--id",
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
    });
  }

  @Test
  public void defineGeneratedIDOK()
    throws IOException, WXMException
  {
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
        "2"
      }
    );

    final var machineSet =
      new WXMVirtualMachineParsers()
        .parse(
          Files.list(this.vmDirectory)
            .findFirst()
            .orElseThrow()
        );

    assertEquals(1, machineSet.machines().size());
  }
}
