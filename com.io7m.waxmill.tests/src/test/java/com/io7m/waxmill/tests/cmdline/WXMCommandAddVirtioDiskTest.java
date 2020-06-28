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
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMStorageBackends;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandAddVirtioDiskTest
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
  public void addVirtioDiskOK()
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

    final var machineSet =
      parseFirst(this.vmDirectory);

    final var machine =
      machineSet.machines()
        .values()
        .iterator()
        .next();

    final var disk =
      (WXMDeviceVirtioBlockStorage) machine.devices().get(1);
    final var storage =
      (WXMStorageBackendFile) disk.backend();

    assertEquals("/tmp/xyz", storage.file().toString());
  }

  @Test
  public void addVirtioDiskZFSVolumeOK()
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
        "zfs-volume",
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

    final var disk =
      (WXMDeviceVirtioBlockStorage) machine.devices().get(1);
    final var storage =
      (WXMStorageBackendZFSVolume) disk.backend();

    assertEquals(
      this.zfsDirectory.resolve(id.toString())
        .resolve(String.format(
          "disk-%d_%d_%d",
          Integer.valueOf(disk.deviceSlot().busID()),
          Integer.valueOf(disk.deviceSlot().slotID()),
          Integer.valueOf(disk.deviceSlot().functionID())
        )),
      WXMStorageBackends.determineZFSVolumePath(
        this.configuration.virtualMachineRuntimeDirectory(),
        id,
        disk.deviceSlot())
    );
  }

  @Test
  public void addVirtioDiskNonexistentVirtualMachine()
  {
    assertThrows(IOException.class, () -> {
      final var id = UUID.randomUUID();
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
        }
      );
    });
  }

  @Test
  public void addVirtioDiskAlreadyUsed()
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

    assertThrows(IOException.class, () -> {
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
    });
  }
}
