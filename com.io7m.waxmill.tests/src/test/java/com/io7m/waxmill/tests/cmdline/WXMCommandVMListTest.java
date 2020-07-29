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
import com.io7m.waxmill.machines.WXMCPUTopology;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMMemory;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSets;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import com.io7m.waxmill.xml.WXMVirtualMachineSerializers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandVMListTest
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
  public void listNoConfiguration()
  {
    Assumptions.assumeFalse(
      System.getenv("WAXMILL_CONFIGURATION_FILE") == null,
      "WAXMILL_CONFIGURATION_FILE environment variable must be undefined"
    );

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-list"
        }
      );
    });
  }

  @Test
  public void listConfigurationFileMissing()
    throws IOException
  {
    Files.deleteIfExists(this.configFile);

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-list",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString()
        }
      );
    });
  }

  @Test
  public void listIsEmpty()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "vm-list",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void listNotEmpty()
    throws IOException
  {
    final var id = UUID.randomUUID();

    new WXMVirtualMachineSerializers()
      .serialize(
        this.vmDirectory.resolve(id + ".wvmx"),
        this.vmDirectory.resolve(id + ".wvmx.tmp"),
        WXMVirtualMachineSets.one(
          WXMVirtualMachine.builder()
            .setId(id)
            .setName(WXMMachineName.of("abcd"))
            .setCpuTopology(WXMCPUTopology.builder().build())
            .setMemory(WXMMemory.builder()
                         .setMegabytes(BigInteger.TEN)
                         .setGigabytes(BigInteger.ONE)
                         .build())
            .setFlags(WXMFlags.builder().build())
            .build()
        )
      );

    MainExitless.main(
      new String[]{
        "vm-list",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString()
      }
    );
  }
}
