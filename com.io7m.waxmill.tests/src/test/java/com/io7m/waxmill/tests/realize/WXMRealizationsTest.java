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

package com.io7m.waxmill.tests.realize;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptionUnsatisfiedRequirement;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.realize.WXMRealizations;
import com.io7m.waxmill.tests.WXMTestDirectories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.internal.verification.Times;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDryRun.EXECUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class WXMRealizationsTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMRealizationsTest.class);

  private static final WXMDeviceSlot DEVICE_SLOT_0 =
    WXMDeviceSlot.builder()
      .setBusID(0)
      .setSlotID(4)
      .setFunctionID(0)
      .build();

  private static final WXMStorageBackendZFSVolume ZFS_VOLUME_SIZED =
    WXMStorageBackendZFSVolume.builder()
      .setExpectedSize(BigInteger.valueOf(128000L))
      .build();

  private static final WXMStorageBackendZFSVolume ZFS_VOLUME_UNSIZED =
    WXMStorageBackendZFSVolume.builder()
      .setExpectedSize(Optional.empty())
      .build();

  private BasicFileAttributes vmSpecificAttributes;
  private BasicFileAttributes vmSpecificDeviceAttributes;
  private FileStore vmBaseFileStore;
  private FileStore vmSpecificDeviceFileStore;
  private FileStore vmSpecificFileStore;
  private FileSystem vmFilesystem;
  private FileSystemProvider vmFilesystemProvider;
  private Path configs;
  private Path directory;
  private Path vmBasePath;
  private Path vmDevNodePath;
  private Path vmSpecificDevicePath;
  private Path vmSpecificPath;
  private Path vms;
  private UUID machineId;
  private WXMClientConfiguration clientConfiguration;
  private WXMProcessesType processes;
  private BasicFileAttributes vmDevNodeAttributes;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.machineId = UUID.randomUUID();

    this.processes =
      mock(WXMProcessesType.class);

    this.directory =
      WXMTestDirectories.createTempDirectory();
    this.configs =
      this.directory.resolve("configs");
    this.vms =
      this.directory.resolve("vms");

    this.vmFilesystemProvider =
      mock(FileSystemProvider.class);
    this.vmFilesystem =
      mock(FileSystem.class);
    this.vmBaseFileStore =
      mock(FileStore.class);
    this.vmSpecificFileStore =
      mock(FileStore.class);
    this.vmSpecificDeviceFileStore =
      mock(FileStore.class);
    this.vmBasePath =
      mock(Path.class);
    this.vmSpecificPath =
      mock(Path.class);
    this.vmSpecificDevicePath =
      mock(Path.class);
    this.vmDevNodePath =
      mock(Path.class);
    this.vmSpecificDeviceAttributes =
      mock(BasicFileAttributes.class);
    this.vmSpecificAttributes =
      mock(BasicFileAttributes.class);
    this.vmDevNodeAttributes =
      mock(BasicFileAttributes.class);

    when(Boolean.valueOf(this.vmBasePath.isAbsolute()))
      .thenReturn(Boolean.TRUE);
    when(this.vmBasePath.resolve(anyString()))
      .thenReturn(this.vmSpecificPath);
    when(this.vmBasePath.toString())
      .thenReturn("/storage/vm");
    when(this.vmBasePath.getFileSystem())
      .thenReturn(this.vmFilesystem);

    when(Boolean.valueOf(this.vmSpecificPath.isAbsolute()))
      .thenReturn(Boolean.TRUE);
    when(this.vmSpecificPath.getFileSystem())
      .thenReturn(this.vmFilesystem);
    when(this.vmSpecificPath.toString())
      .thenReturn(String.format("/storage/vm/%s", this.machineId));
    when(this.vmSpecificPath.resolve(anyString()))
      .thenReturn(this.vmSpecificDevicePath);

    when(Boolean.valueOf(this.vmSpecificDevicePath.isAbsolute()))
      .thenReturn(Boolean.TRUE);
    when(this.vmSpecificDevicePath.getFileSystem())
      .thenReturn(this.vmFilesystem);
    when(this.vmSpecificDevicePath.toString())
      .thenReturn(String.format("/storage/vm/%s/EXTRA", this.machineId));
    when(this.vmSpecificDevicePath.resolve(anyString()))
      .thenReturn(this.vmSpecificDevicePath);

    final String deviceNodeName =
      String.format("/dev/zvol/storage/vm/%s/disk-0_4_0", this.machineId);

    when(Boolean.valueOf(this.vmDevNodePath.isAbsolute()))
      .thenReturn(Boolean.TRUE);
    when(this.vmDevNodePath.getFileSystem())
      .thenReturn(this.vmFilesystem);
    when(this.vmDevNodePath.toString())
      .thenReturn(deviceNodeName);

    when(this.vmFilesystem.getPath(deviceNodeName))
      .thenReturn(this.vmDevNodePath);

    when(this.vmFilesystem.provider())
      .thenReturn(this.vmFilesystemProvider);
    when(this.vmFilesystemProvider.getFileStore(this.vmBasePath))
      .thenReturn(this.vmBaseFileStore);
    when(this.vmFilesystemProvider.getFileStore(this.vmSpecificPath))
      .thenReturn(this.vmSpecificFileStore);
    when(this.vmFilesystemProvider.getFileStore(this.vmSpecificDevicePath))
      .thenReturn(this.vmSpecificDeviceFileStore);

    when(this.vmBaseFileStore.type())
      .thenReturn("zfs");
    when(this.vmBaseFileStore.name())
      .thenReturn("storage/vm");

    when(this.vmSpecificFileStore.type())
      .thenReturn("zfs");
    when(this.vmSpecificFileStore.name())
      .thenReturn(String.format("storage/vm/%s", this.machineId));

    when(this.vmSpecificDeviceFileStore.type())
      .thenReturn("zfs");

    this.clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeFilesystem(
          WXMZFSFilesystem.builder()
            .setMountPoint(this.vmBasePath)
            .setName("storage/vm")
            .build()
        ).build();
  }

  @Test
  public void realizeNothing()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var steps = new ArrayList<>(instructions.steps());

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
    }

    assertEquals(0, steps.size());
    instructions.execute(EXECUTE);
  }

  @Test
  public void realizeAHCIZFSOK()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var steps = new ArrayList<>(instructions.steps());

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
    }

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
      assertTrue(description.contains(DEVICE_SLOT_0.toString()));
      assertTrue(description.contains(String.format(
        "storage/vm/%s/disk-0_4_0",
        machine.id()
      )));
      assertTrue(description.contains("128000"));

      final var processes = step.processes();
      assertEquals(1, processes.size());

      final WXMProcessDescription process = processes.get(0);
      assertEquals(
        this.clientConfiguration.zfsExecutable(),
        process.executable());
      final var arguments = process.arguments();
      assertEquals("create", arguments.get(0));
      assertEquals("-V", arguments.get(1));
      assertEquals("128000", arguments.get(2));
      assertEquals(String.format(
        "storage/vm/%s/disk-0_4_0",
        machine.id()
      ), arguments.get(3));
    }

    assertEquals(0, steps.size());
  }

  @Test
  public void realizeVirtioBlockZFSOK()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var steps = new ArrayList<>(instructions.steps());

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
    }

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
      assertTrue(description.contains(DEVICE_SLOT_0.toString()));
      assertTrue(description.contains(String.format(
        "storage/vm/%s/disk-0_4_0",
        machine.id()
      )));
      assertTrue(description.contains("128000"));

      final var processes = step.processes();
      assertEquals(1, processes.size());

      final WXMProcessDescription process = processes.get(0);
      assertEquals(
        this.clientConfiguration.zfsExecutable(),
        process.executable());
      final var arguments = process.arguments();
      assertEquals("create", arguments.get(0));
      assertEquals("-V", arguments.get(1));
      assertEquals("128000", arguments.get(2));
      assertEquals(String.format(
        "storage/vm/%s/disk-0_4_0",
        machine.id()
      ), arguments.get(3));
    }

    assertEquals(0, steps.size());
  }

  @Test
  public void realizeVirtioBlockZFSWrongSize()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    when(this.vmFilesystemProvider.readAttributes(
      eq(this.vmDevNodePath), eq(BasicFileAttributes.class), any()))
      .thenReturn(this.vmDevNodeAttributes);

    when(Long.valueOf(this.vmDevNodeAttributes.size()))
      .thenReturn(Long.valueOf(23L));

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    instructions.execute(EXECUTE);
  }

  @Test
  public void realizeVirtioBlockZFSFails()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmDevNodePath);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    doThrow(new IOException("FAILED!"))
      .when(this.processes)
      .processStartAndWait(any());

    final var ex = assertThrowsLogged(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    final Exception cause = (Exception) ex.getSuppressed()[0];
    assertEquals(IOException.class, cause.getClass());
    assertEquals("FAILED!", cause.getMessage());

    verify(this.processes, new Times(1))
      .processStartAndWait(
        WXMProcessDescription.builder()
          .setExecutable(this.clientConfiguration.zfsExecutable())
          .addArguments("create")
          .addArguments("-V")
          .addArguments("128000")
          .addArguments(String.format(
            "storage/vm/%s/disk-0_4_0",
            machine.id()
          )).build()
      );
  }

  @Test
  public void realizeAHCIBlockZFSFails()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    when(this.vmFilesystemProvider.readAttributes(
      eq(this.vmSpecificPath), eq(BasicFileAttributes.class), any()))
      .thenReturn(this.vmSpecificAttributes);

    when (Boolean.valueOf(this.vmSpecificAttributes.isDirectory()))
      .thenReturn(Boolean.TRUE);

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmDevNodePath);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    doThrow(new IOException("FAILED!"))
      .when(this.processes)
      .processStartAndWait(any());

    final var ex = assertThrowsLogged(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    Exception cause = (Exception) ex.getSuppressed()[0];
    cause = (Exception) cause.getCause();
    assertEquals(IOException.class, cause.getClass());
    assertEquals("FAILED!", cause.getMessage());

    verify(this.processes, new Times(1))
      .processStartAndWait(
        WXMProcessDescription.builder()
          .setExecutable(this.clientConfiguration.zfsExecutable())
          .addArguments("create")
          .addArguments("-V")
          .addArguments("128000")
          .addArguments(String.format(
            "storage/vm/%s/disk-0_4_0",
            machine.id()
          )).build()
      );
  }

  @Test
  public void realizeAHCIBlockZFSFailsUnsized()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_UNSIZED)
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmDevNodePath);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var ex = assertThrowsLogged(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    final var exc = (Exception) ex.getSuppressed()[0];
    assertTrue(exc.getMessage().contains("no expected size"));
  }

  @Test
  public void realizeAHCIFileOK()
    throws Exception
  {
    final var file =
      this.directory.resolve(this.machineId.toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(file)
                .build())
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var steps = new ArrayList<>(instructions.steps());

    Files.write(file, new byte[100]);
    instructions.execute(EXECUTE);

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
    }

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
      assertTrue(description.contains(DEVICE_SLOT_0.toString()));
      assertTrue(description.contains(file.toString()));

      final var processes = step.processes();
      assertEquals(0, processes.size());
    }

    assertEquals(0, steps.size());
  }

  @Test
  public void realizeAHCIFileFails()
    throws Exception
  {
    final var file =
      this.directory.resolve(this.machineId.toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(file)
                .build())
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    Files.deleteIfExists(file);

    final var ex = assertThrowsLogged(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    final var exc = (Exception) ex.getSuppressed()[0];
    assertTrue(exc.getMessage().contains(file.toString()));
  }


  @Test
  public void realizeVirtioBlockStorageFileOK()
    throws Exception
  {
    final var file =
      this.directory.resolve(this.machineId.toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(file)
                .build())
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    Files.write(file, new byte[100]);
    instructions.execute(EXECUTE);

    final var steps = new ArrayList<>(instructions.steps());

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
    }

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);
      assertTrue(description.contains(DEVICE_SLOT_0.toString()));
      assertTrue(description.contains(file.toString()));

      final var processes = step.processes();
      assertEquals(0, processes.size());
    }
  }

  @Test
  public void realizeVirtioBlockStorageFileFails()
    throws Exception
  {
    final var file =
      this.directory.resolve(this.machineId.toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(file)
                .build())
            .build())
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    Files.deleteIfExists(file);

    final var ex = assertThrowsLogged(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    final var exc = (Exception) ex.getSuppressed()[0];
    assertTrue(exc.getMessage().contains(file.toString()));
  }

  @Test
  public void realizeZFSCreate()
    throws WXMException, IOException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .build();

    doThrow(new IOException())
      .when(this.vmFilesystemProvider)
      .checkAccess(this.vmSpecificPath);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var steps = new ArrayList<>(instructions.steps());
    instructions.execute(EXECUTE);

    {
      final var step = steps.remove(0);
      final var description = step.description();
      LOG.debug("{}", description);

      final var proc = step.processes().get(0);
      final var arguments = proc.arguments();
      assertEquals(this.clientConfiguration.zfsExecutable(), proc.executable());
      assertEquals("create", arguments.get(0));
      assertEquals(
        String.format("storage/vm/%s", machine.id()),
        arguments.get(1));
      assertEquals(1, step.processes().size());
    }

    assertEquals(0, steps.size());
  }

  @Test
  public void realizeZFSCreateNotADirectory()
    throws WXMException, IOException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(this.machineId)
        .setName(WXMMachineName.of("vm"))
        .build();

    when(this.vmFilesystemProvider.readAttributes(
      eq(this.vmSpecificPath), eq(BasicFileAttributes.class), any()))
      .thenReturn(this.vmSpecificAttributes);

    when (Boolean.valueOf(this.vmSpecificAttributes.isDirectory()))
      .thenReturn(Boolean.FALSE);

    final var realizations =
      WXMRealizations.create(this.processes, this.clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var steps = new ArrayList<>(instructions.steps());

    final var ex = assertThrowsLogged(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    final var exc = (Exception) ex.getSuppressed()[0];
    assertTrue(exc.getMessage().contains("not a directory"));
  }

  private static <T extends Throwable> T assertThrowsLogged(
    final Class<T> expectedType,
    final Executable executable)
  {
    final var ex = assertThrows(expectedType, executable);
    LOG.debug("", ex);
    return ex;
  }
}
