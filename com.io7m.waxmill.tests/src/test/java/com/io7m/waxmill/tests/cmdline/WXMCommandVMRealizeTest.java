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
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMStorageBackends;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.util.Locale.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMCommandVMRealizeTest
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
    Files.createDirectories(this.zfsDirectory);

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
  public void tooFewArguments()
  {
    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-realize"
        }
      );
    });
  }

  @Test
  public void configurationFileMissing()
    throws IOException
  {
    Files.deleteIfExists(this.configFile);

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-realize",
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
  public void realizeMissingFile()
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
        "vm-add-ahci-disk",
        "--verbose",
        "trace",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        String.format("file;%s", this.directory.resolve("nonexistent")),
        "--device-slot",
        "0:1:0"
      }
    );

    assertThrows(IOException.class, () -> {
      MainExitless.main(
        new String[]{
          "vm-realize",
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
  public void realizeMissingFileDryRun()
    throws Exception
  {
    final var id = UUID.randomUUID();
    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-ahci-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        String.format("file;%s", this.directory.resolve("nonexistent")),
        "--device-slot",
        "0:1:0"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--dry-run",
        "true",
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeVirtio()
    throws Exception
  {
    final var id = UUID.randomUUID();

    final Path path = this.zfsDirectory.resolve(id.toString());
    Files.createDirectories(path);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-virtio-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeVirtioDryRun()
    throws Exception
  {
    final var id = UUID.randomUUID();

    final Path path = this.zfsDirectory.resolve(id.toString());
    Files.createDirectories(path);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-virtio-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--dry-run",
        "true",
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeVirtioExists()
    throws Exception
  {
    final var id = UUID.randomUUID();
    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-virtio-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    final var path =
      WXMStorageBackends.determineZFSVolumePath(
        this.zfsDirectory,
        id,
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(1)
          .setFunctionID(0)
          .build()
      );
    Files.createDirectories(path.getParent());
    Files.write(path, new byte[128000]);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  private static void assumeZFSFilesystem(final Path path)
    throws IOException
  {
    Assumptions.assumeTrue(
      "ZFS".equals(Files.getFileStore(path).type().toUpperCase(ROOT)),
      String.format("%s is a ZFS filesystem", path)
    );
  }

  @Test
  public void realizeZFSVolumeVirtioExistsWrongSize()
    throws Exception
  {
    final var id = UUID.randomUUID();
    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-virtio-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    final var path =
      WXMStorageBackends.determineZFSVolumePath(
        this.zfsDirectory,
        id,
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(1)
          .setFunctionID(0)
          .build()
      );
    Files.createDirectories(path.getParent());
    Files.write(path, new byte[128001]);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeAHCI()
    throws Exception
  {
    final var id = UUID.randomUUID();

    final Path path = this.zfsDirectory.resolve(id.toString());
    Files.createDirectories(path);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-ahci-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeAHCIExists()
    throws Exception
  {
    final var id = UUID.randomUUID();
    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-ahci-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    final var path =
      WXMStorageBackends.determineZFSVolumePath(
        this.zfsDirectory,
        id,
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(1)
          .setFunctionID(0)
          .build()
      );
    Files.createDirectories(path.getParent());
    Files.write(path, new byte[128000]);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeAHCIExistsWrongSize()
    throws Exception
  {
    final var id = UUID.randomUUID();
    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-ahci-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    final var path =
      WXMStorageBackends.determineZFSVolumePath(
        this.zfsDirectory,
        id,
        WXMDeviceSlot.builder()
          .setBusID(0)
          .setSlotID(1)
          .setFunctionID(0)
          .build()
      );
    Files.createDirectories(path.getParent());
    Files.write(path, new byte[128001]);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--configuration",
        this.configFile.toString()
      }
    );
  }

  @Test
  public void realizeZFSVolumeAHCIDryRun()
    throws Exception
  {
    final var id = UUID.randomUUID();

    final Path path = this.zfsDirectory.resolve(id.toString());
    Files.createDirectories(path);
    assumeZFSFilesystem(path);

    MainExitless.main(
      new String[]{
        "vm-define",
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
        "vm-add-ahci-disk",
        "--configuration",
        this.configFile.toString(),
        "--machine",
        id.toString(),
        "--backend",
        "zfs-volume;128000",
        "--device-slot",
        "0:1:0"
      }
    );

    MainExitless.main(
      new String[]{
        "vm-realize",
        "--machine",
        id.toString(),
        "--dry-run",
        "true",
        "--configuration",
        this.configFile.toString()
      }
    );
  }
}
