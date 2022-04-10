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
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import com.io7m.waxmill.xml.WXMVirtualMachineParsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.io7m.waxmill.tests.WXMExceptions.assertThrowsLogged;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class WXMCommandVMImportTest
{
  private Path directory;
  private Path configFile;
  private Path configFileTmp;
  private Path vmDirectory;
  private Path zfsDirectory;
  private WXMClientConfiguration configuration;
  private Path vm0;
  private Path vm0Imported;
  private Path vmBad0;
  private Path vmBad1;
  private Path vmBad2;
  private Path vmNonexistent;

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

    this.vm0Imported =
      this.vmDirectory.resolve("1a438a53-2fcd-498f-8cc2-0ff0456e3dc4.wvmx");

    this.vm0 =
      WXMTestDirectories.resourceOf(
        WXMCommandVMImportTest.class,
        this.directory,
        "vm0.xml"
      );

    this.vmBad0 =
      Files.write(this.directory.resolve("vmbad0"), "HELLO".getBytes());
    this.vmBad1 =
      Files.write(this.directory.resolve("vmbad1"), "HELLO".getBytes());
    this.vmBad2 =
      Files.write(this.directory.resolve("vmbad2"), "HELLO".getBytes());
    this.vmNonexistent =
      this.directory.resolve("nonexistent");

    Files.deleteIfExists(this.vmNonexistent);
  }

  @Test
  public void importOK()
    throws IOException, WXMException
  {
    MainExitless.main(
      new String[]{
        "vm-import",
        "--verbose", "trace",
        "--configuration", this.configFile.toString(),
        "--file", this.vm0.toString()
      }
    );

    final var machineSetOriginal =
      new WXMVirtualMachineParsers()
        .parse(this.vm0);
    final var machineSetImported =
      new WXMVirtualMachineParsers()
        .parse(this.vm0Imported);

    final var machineOriginal =
      machineSetOriginal.machines()
        .get(machineSetOriginal.machines().firstKey());
    final var machineImported =
      machineSetImported.machines()
        .get(machineSetImported.machines().firstKey());

    assertEquals(
      machineOriginal.withConfigurationFile(Optional.empty()),
      machineImported.withConfigurationFile(Optional.empty())
    );
  }

  @Test
  public void importUnparseable()
    throws IOException
  {
    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-import",
          "--verbose", "trace",
          "--configuration", this.configFile.toString(),
          "--file", this.vmBad0.toString(),
          "--file", this.vmBad1.toString(),
          "--file", this.vmBad2.toString()
        }
      );
    });

    assertEquals(
      0L,
      WXMParsing.listVMFiles(this.vmDirectory).count()
    );
  }

  @Test
  public void importNonexistent()
    throws IOException
  {
    assertThrowsLogged(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-import",
          "--verbose", "trace",
          "--configuration", this.configFile.toString(),
          "--file", this.vmNonexistent.toString()
        }
      );
    });

    assertEquals(
      0L,
      WXMParsing.listVMFiles(this.vmDirectory).count()
    );
  }
}
