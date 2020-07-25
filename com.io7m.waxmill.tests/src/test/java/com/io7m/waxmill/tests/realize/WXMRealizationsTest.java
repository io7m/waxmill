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
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.realize.WXMRealizations;
import com.io7m.waxmill.tests.WXMTestDirectories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.List;
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

  private WXMProcessesType processes;
  private Path directory;
  private Path configs;
  private Path vms;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.processes =
      mock(WXMProcessesType.class);

    this.directory =
      WXMTestDirectories.createTempDirectory();
    this.configs =
      this.directory.resolve("configs");
    this.vms =
      this.directory.resolve("vms");
  }

  @Test
  public void realizeNothing()
    throws WXMException, IOException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .build();

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
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
    throws IOException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
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
        "%s/%s/disk-0_4_0",
        clientConfiguration.virtualMachineRuntimeDirectory(),
        machine.id()
      )));
      assertTrue(description.contains("128000"));

      final var processes = step.processes();
      assertEquals(1, processes.size());

      final WXMProcessDescription process = processes.get(0);
      assertEquals(clientConfiguration.zfsExecutable(), process.executable());
      final var arguments = process.arguments();
      assertEquals("create", arguments.get(0));
      assertEquals("-V", arguments.get(1));
      assertEquals("128000", arguments.get(2));
      assertEquals(String.format(
        "%s/%s/disk-0_4_0",
        clientConfiguration.virtualMachineRuntimeDirectory(),
        machine.id()
      ), arguments.get(3));
    }

    assertEquals(0, steps.size());
  }

  @Test
  public void realizeVirtioBlockZFSOK()
    throws IOException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
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
        "%s/%s/disk-0_4_0",
        clientConfiguration.virtualMachineRuntimeDirectory(),
        machine.id()
      )));
      assertTrue(description.contains("128000"));

      final var processes = step.processes();
      assertEquals(1, processes.size());

      final WXMProcessDescription process = processes.get(0);
      assertEquals(clientConfiguration.zfsExecutable(), process.executable());
      final var arguments = process.arguments();
      assertEquals("create", arguments.get(0));
      assertEquals("-V", arguments.get(1));
      assertEquals("128000", arguments.get(2));
      assertEquals(String.format(
        "%s/%s/disk-0_4_0",
        clientConfiguration.virtualMachineRuntimeDirectory(),
        machine.id()
      ), arguments.get(3));
    }

    assertEquals(0, steps.size());
  }

  @Test
  public void realizeVirtioBlockZFSFails()
    throws Exception
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    doThrow(new IOException("FAILED!"))
      .when(this.processes)
      .processStartAndWait(any());

    final var ex = assertThrows(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    Exception cause = (Exception) ex.getSuppressed()[0];
    cause = (Exception) cause.getCause();
    assertEquals(IOException.class, cause.getClass());
    assertEquals("FAILED!", cause.getMessage());

    verify(this.processes, new Times(1))
      .processStartAndWait(
        WXMProcessDescription.builder()
          .setExecutable(clientConfiguration.zfsExecutable())
          .addArguments("create")
          .addArguments("-V")
          .addArguments("128000")
          .addArguments(String.format(
            "%s/%s/disk-0_4_0",
            clientConfiguration.virtualMachineRuntimeDirectory(),
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
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_SIZED)
            .build())
        .build();

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    doThrow(new IOException("FAILED!"))
      .when(this.processes)
      .processStartAndWait(any());

    final var ex = assertThrows(WXMException.class, () -> {
      instructions.execute(EXECUTE);
    });

    Exception cause = (Exception) ex.getSuppressed()[0];
    cause = (Exception) cause.getCause();
    assertEquals(IOException.class, cause.getClass());
    assertEquals("FAILED!", cause.getMessage());

    verify(this.processes, new Times(1))
      .processStartAndWait(
        WXMProcessDescription.builder()
          .setExecutable(clientConfiguration.zfsExecutable())
          .addArguments("create")
          .addArguments("-V")
          .addArguments("128000")
          .addArguments(String.format(
            "%s/%s/disk-0_4_0",
            clientConfiguration.virtualMachineRuntimeDirectory(),
            machine.id()
          )).build()
      );
  }

  @Test
  public void realizeAHCIBlockZFSFailsUnsized()
    throws IOException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(DEVICE_SLOT_0)
            .setBackend(ZFS_VOLUME_UNSIZED)
            .build())
        .build();

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    final var ex = assertThrows(WXMException.class, () -> {
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
      this.directory.resolve(UUID.randomUUID().toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
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

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
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
      this.directory.resolve(UUID.randomUUID().toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
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

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    Files.deleteIfExists(file);

    final var ex = assertThrows(WXMException.class, () -> {
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
      this.directory.resolve(UUID.randomUUID().toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
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

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
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
      this.directory.resolve(UUID.randomUUID().toString());

    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
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

    Files.createDirectories(this.vms.resolve(machine.id().toString()));

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(this.vms)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
    final var instructions =
      realizations.evaluate();

    Files.deleteIfExists(file);

    final var ex = assertThrows(WXMException.class, () -> {
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
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .build();

    final var vmFilesystemProvider =
      mock(FileSystemProvider.class);
    final var vmFilesystem =
      mock(FileSystem.class);
    final var vmFileStore =
      mock(FileStore.class);
    final var attributes =
      mock(BasicFileAttributes.class);
    final var vmPath =
      mock(Path.class);

    when(Boolean.valueOf(attributes.isDirectory()))
      .thenReturn(Boolean.FALSE);
    when(vmFilesystemProvider.getFileStore(any()))
      .thenReturn(vmFileStore);
    when(vmFilesystem.provider())
      .thenReturn(vmFilesystemProvider);
    doThrow(IOException.class)
      .when(vmFilesystemProvider)
      .checkAccess(any(), any());
    when(vmFilesystemProvider.readAttributes(
      any(), eq(BasicFileAttributes.class), any()))
      .thenReturn(attributes);
    when(vmPath.resolve(anyString()))
      .thenReturn(vmPath);
    when(vmPath.getFileSystem())
      .thenReturn(vmFilesystem);
    when(Boolean.valueOf(vmPath.isAbsolute()))
      .thenReturn(Boolean.TRUE);
    when(vmPath.toString())
      .thenReturn("/storage/x/y/z");
    when(vmFileStore.type())
      .thenReturn("zfs");
    when(vmFileStore.name())
      .thenReturn("storage/x/y/z");

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeDirectory(vmPath)
        .build();

    final var realizations =
      WXMRealizations.create(this.processes, clientConfiguration, machine);
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
      assertEquals(clientConfiguration.zfsExecutable(), proc.executable());
      assertEquals("create", arguments.get(0));
      assertEquals(String.format("storage/x/y/z/%s", machine.id()), arguments.get(1));
      assertEquals(1, step.processes().size());
    }

    assertEquals(0, steps.size());
  }
}
