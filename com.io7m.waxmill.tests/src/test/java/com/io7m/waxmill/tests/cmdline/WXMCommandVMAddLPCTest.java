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
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMTTYBackends.NMDMSide.NMDM_GUEST;
import static com.io7m.waxmill.machines.WXMTTYBackends.NMDMSide.NMDM_HOST;
import static com.io7m.waxmill.machines.WXMTTYBackends.nmdmPath;
import static com.io7m.waxmill.tests.cmdline.WXMParsing.parseFirst;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandVMAddLPCTest
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
  public void addLPCNonexistentVirtualMachine()
  {
    assertThrows(IOException.class, () -> {
      final var id = UUID.randomUUID();
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
          "stdio;com1"
        }
      );
    });
  }

  @Test
  public void addLPCAlreadyUsed()
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

    assertThrows(IOException.class, () -> {
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
          "stdio;com1",
          "--device-slot",
          "0:1:0"
        }
      );
    });
  }

  @Test
  public void addLPCDevicesNotUnique()
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

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-add-lpc-device",
          "--verbose",
          "trace",
          "--configuration",
          this.configFile.toString(),
          "--machine",
          id.toString(),
          "--device-slot",
          "0:1:0",
          "--add-backend",
          "stdio;com1",
          "--add-backend",
          "file;com1;/tmp/xyz"
        }
      );
    });
  }

  @Test
  public void addLPCOK()
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
        "vm-add-lpc-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--add-backend",
        "stdio;com1",
        "--add-backend",
        "file;com2;/tmp/xyz",
        "--device-slot",
        "0:1:0"
      }
    );

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var net =
      (WXMDeviceLPC) machine.devices().get(1);
    final var tty0 =
      (WXMTTYBackendStdio) net.backendMap().get("com1");
    final var tty1 =
      (WXMTTYBackendFile) net.backendMap().get("com2");

    assertEquals("com1", tty0.device());
    assertEquals("com2", tty1.device());
  }

  @Test
  public void addLPCOKMultiple()
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
        "vm-add-lpc-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--add-backend",
        "stdio;com1",
        "--add-backend",
        "stdio;com2",
        "--device-slot",
        "0:1:0"
      }
    );

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var net =
      (WXMDeviceLPC) machine.devices().get(1);
    final var tty0 =
      (WXMTTYBackendStdio) net.backendMap().get("com1");
    final var tty1 =
      (WXMTTYBackendStdio) net.backendMap().get("com2");

    assertEquals("com1", tty0.device());
    assertEquals("com2", tty1.device());
  }

  @Test
  public void addLPCOKNMDM()
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
        "vm-add-lpc-device",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--add-backend",
        "nmdm;com1",
        "--device-slot",
        "0:1:0"
      }
    );

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var net =
      (WXMDeviceLPC) machine.devices().get(1);
    final var tty0 =
      (WXMTTYBackendNMDM) net.backendMap().get("com1");

    assertEquals("com1", tty0.device());
    assertEquals(
      String.format("/dev/nmdm_%s_A", machine.id()),
      nmdmPath(FileSystems.getDefault(), machine.id(), NMDM_GUEST).toString());
    assertEquals(
      String.format("/dev/nmdm_%s_B", machine.id()),
      nmdmPath(FileSystems.getDefault(), machine.id(), NMDM_HOST).toString());
  }

  @Test
  public void addLPCBusNotZero()
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

    assertThrows(IOException.class, () -> {
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
          "nmdm;com1",
          "--device-slot",
          "1:1:0"
        }
      );
    });
  }
}
