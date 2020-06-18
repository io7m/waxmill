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

package com.io7m.waxmill.tests;

import com.io7m.waxmill.client.api.WXMCPUTopology;
import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.client.api.WXMFlags;
import com.io7m.waxmill.client.api.WXMMachineName;
import com.io7m.waxmill.client.api.WXMMemory;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import com.io7m.waxmill.database.api.WXMDatabaseConfiguration;
import com.io7m.waxmill.database.vanilla.WXMVirtualMachineDatabases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class WXMVirtualMachineDatabasesTest
{
  private WXMVirtualMachine virtualMachine;
  private FileSystem filesystem;
  private FileSystemProvider provider;

  @BeforeEach
  public void setup()
  {
    this.virtualMachine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("test"))
        .setFlags(
          WXMFlags.builder()
            .build())
        .setMemory(
          WXMMemory.builder()
            .setGigabytes(BigInteger.ONE)
            .setMegabytes(BigInteger.TEN)
            .build())
        .setCpuTopology(
          WXMCPUTopology.builder()
            .build())
        .build();

    this.filesystem =
      mock(FileSystem.class);
    this.provider =
      mock(FileSystemProvider.class);
    when(this.filesystem.provider())
      .thenReturn(this.provider);
  }

  @Test
  public void faultyFilesystemCannotOpen()
    throws IOException
  {
    final var path = mock(Path.class);
    when(path.getParent()).thenReturn(null);
    when(path.toAbsolutePath()).thenReturn(path);
    when(path.resolve(anyString())).thenReturn(path);
    when(path.getFileSystem()).thenReturn(this.filesystem);

    doThrow(new FileAlreadyExistsException("FAILURE!"))
      .when(this.provider)
      .createDirectory(any(), any());

    when(this.provider.readAttributes(
      any(),
      eq(BasicFileAttributes.class),
      any()))
      .thenThrow(new IOException("Failed to read attributes!"));

    final var configuration =
      WXMDatabaseConfiguration.builder()
        .setDatabaseDirectory(path)
        .build();

    final var databases = new WXMVirtualMachineDatabases();

    final var ex = assertThrows(
      WXMException.class,
      () -> databases.open(configuration));
    final var cause = ex.getCause();
    assertEquals(FileAlreadyExistsException.class, cause.getClass());
    assertEquals("FAILURE!", cause.getMessage());
  }

  @Test
  public void faultyFilesystemCannotWrite()
    throws IOException, WXMException
  {
    final var machineFile = mock(Path.class);
    when(machineFile.getFileSystem()).thenReturn(this.filesystem);
    when(machineFile.toString()).thenReturn("MACHINE FILE");
    when(machineFile.toUri()).thenReturn(URI.create("urn:x"));

    final var machineFileTmp = mock(Path.class);
    when(machineFileTmp.getFileSystem()).thenReturn(this.filesystem);
    when(machineFileTmp.toString()).thenReturn("MACHINE FILE TEMP");
    when(machineFileTmp.toUri()).thenReturn(URI.create("urn:x"));

    final var lockFile = mock(Path.class);
    when(lockFile.toString()).thenReturn("LOCK FILE");
    when(lockFile.getFileSystem()).thenReturn(this.filesystem);

    final var path = mock(Path.class);
    when(path.getFileSystem()).thenReturn(this.filesystem);

    when(path.resolve("lock"))
      .thenReturn(lockFile);
    when(path.resolve(eq(this.virtualMachine.id() + ".wvmx")))
      .thenReturn(machineFile);
    when(path.resolve(eq(this.virtualMachine.id() + ".wvmx.tmp")))
      .thenReturn(machineFileTmp);

    final var lockStream = mock(OutputStream.class);
    final var lockChannel = mock(FileChannel.class);
    final var lockLock = mock(FileLock.class);

    when(lockChannel.lock())
      .thenReturn(lockLock);

    doThrow(new IOException("Unreadable!"))
      .when(this.provider)
      .checkAccess(eq(machineFile), any());

    when(this.provider.newOutputStream(eq(lockFile), eq(CREATE), eq(WRITE)))
      .thenReturn(lockStream);
    when(this.provider.newOutputStream(eq(lockFile), eq(CREATE)))
      .thenReturn(lockStream);
    when(this.provider.newFileChannel(eq(lockFile), any()))
      .thenReturn(lockChannel);
    when(this.provider.newOutputStream(eq(machineFileTmp), eq(CREATE_NEW)))
      .thenThrow(new IOException("Write failure!"));

    final var configuration =
      WXMDatabaseConfiguration.builder()
        .setDatabaseDirectory(path)
        .build();

    final var databases = new WXMVirtualMachineDatabases();
    final var database = databases.open(configuration);

    final var ex = assertThrows(WXMException.class, () -> {
      database.vmDefine(this.virtualMachine);
    });
    final var cause = ex.getCause();
    assertEquals(IOException.class, cause.getClass());
    assertEquals("Write failure!", cause.getMessage());
  }
}
