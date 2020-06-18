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

package com.io7m.waxmill.database.vanilla.internal;

import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.client.api.WXMExceptionDuplicate;
import com.io7m.waxmill.client.api.WXMExceptions;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import com.io7m.waxmill.client.api.WXMVirtualMachineSet;
import com.io7m.waxmill.client.api.WXMVirtualMachineSets;
import com.io7m.waxmill.database.api.WXMDatabaseConfiguration;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseType;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserProviderType;
import com.io7m.waxmill.serializer.api.WXMVirtualMachineSerializerProviderType;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public final class WXMVirtualMachineDatabase
  implements WXMVirtualMachineDatabaseType
{
  private final WXMDatabaseConfiguration configuration;
  private final Path lockFile;
  private final WXMVirtualMachineParserProviderType parsers;
  private final WXMVirtualMachineSerializerProviderType serializers;

  private WXMVirtualMachineDatabase(
    final WXMVirtualMachineParserProviderType inParsers,
    final WXMVirtualMachineSerializerProviderType inSerializers,
    final WXMDatabaseConfiguration inConfiguration,
    final Path inLockFile)
  {
    this.parsers =
      Objects.requireNonNull(inParsers, "inParsers");
    this.serializers =
      Objects.requireNonNull(inSerializers, "inSerializers");
    this.configuration =
      Objects.requireNonNull(inConfiguration, "inConfiguration");
    this.lockFile =
      Objects.requireNonNull(inLockFile, "lockFile");
  }

  public static WXMVirtualMachineDatabaseType open(
    final WXMVirtualMachineParserProviderType inParsers,
    final WXMVirtualMachineSerializerProviderType inSerializers,
    final WXMDatabaseConfiguration configuration)
    throws WXMException
  {
    Objects.requireNonNull(inParsers, "inParsers");
    Objects.requireNonNull(inSerializers, "inSerializers");
    Objects.requireNonNull(configuration, "configuration");

    try {
      final Path databaseDirectory = configuration.databaseDirectory();
      Files.createDirectories(databaseDirectory);

      final var lockFile = databaseDirectory.resolve("lock");
      Files.write(lockFile, "lock".getBytes(UTF_8), CREATE);
      return new WXMVirtualMachineDatabase(
        inParsers,
        inSerializers,
        configuration,
        lockFile
      );
    } catch (final IOException e) {
      throw new WXMException(e);
    }
  }

  private DatabaseWriteLock acquireWriteLock()
    throws WXMException
  {
    try {
      return DatabaseWriteLock.acquire(this.lockFile);
    } catch (final IOException e) {
      throw new WXMException(e);
    }
  }

  @Override
  public Optional<WXMVirtualMachine> vmGet(
    final UUID machineId)
    throws WXMException
  {
    Objects.requireNonNull(machineId, "machineId");

    final var file =
      this.configuration.databaseDirectory()
        .resolve(machineId + ".wvmx");

    if (Files.exists(file)) {
      final var set = this.parsers.parse(file);
      final var machine = set.machines().get(machineId);
      return Optional.ofNullable(machine);
    }

    return Optional.empty();
  }

  @Override
  public void vmDefine(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    final var machineId =
      machine.id();
    final Path base =
      this.configuration.databaseDirectory();
    final var file =
      base.resolve(machineId + ".wvmx");
    final var fileTmp =
      base.resolve(machineId + ".wvmx.tmp");

    try (var ignored = this.acquireWriteLock()) {
      final var existing = this.vmGet(machineId);
      if (existing.isPresent()) {
        throw new WXMExceptionDuplicate(
          String.format(
            "A virtual machine already exists with ID %s", machineId
          )
        );
      }
      this.serializers.serialize(
        file,
        fileTmp,
        WXMVirtualMachineSets.one(machine)
      );
    } catch (final IOException e) {
      throw new WXMException(e);
    }
  }

  @Override
  public void vmUpdate(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    final var machineId =
      machine.id();
    final Path base =
      this.configuration.databaseDirectory();
    final var file =
      base.resolve(machineId + ".wvmx");
    final var fileTmp =
      base.resolve(machineId + ".wvmx.tmp");

    try (var ignored = this.acquireWriteLock()) {
      this.serializers.serialize(
        file,
        fileTmp,
        WXMVirtualMachineSets.one(machine)
      );
    } catch (final IOException e) {
      throw new WXMException(e);
    }
  }

  private static boolean appearsToBeVirtualMachine(
    final Path path)
  {
    return path.toString().toUpperCase(Locale.ROOT).endsWith(".WVMX");
  }

  @Override
  public WXMVirtualMachineSet vmList()
    throws WXMException
  {
    final var exceptions = new WXMExceptions();

    final var sets = new ArrayList<WXMVirtualMachineSet>();
    try (var stream = Files.list(this.configuration.databaseDirectory())) {
      final var list =
        stream.map(Path::toAbsolutePath)
          .filter(WXMVirtualMachineDatabase::appearsToBeVirtualMachine)
          .sorted()
          .collect(Collectors.toList());
      for (final var file : list) {
        try {
          sets.add(this.parsers.parse(file));
        } catch (final WXMException e) {
          exceptions.add(e);
        }
      }
    } catch (final IOException e) {
      exceptions.add(e);
    }

    exceptions.throwIfRequired();
    return WXMVirtualMachineSets.merge(sets);
  }

  @Override
  public void close()
  {

  }

  private static final class DatabaseWriteLock implements AutoCloseable
  {
    private final FileChannel channel;
    private final FileLock lock;

    private DatabaseWriteLock(
      final FileChannel inChannel,
      final FileLock inLock)
    {
      this.channel =
        Objects.requireNonNull(inChannel, "channel");
      this.lock =
        Objects.requireNonNull(inLock, "lock");
    }

    public static DatabaseWriteLock acquire(
      final Path lockFile)
      throws IOException
    {
      final var channel = FileChannel.open(lockFile, CREATE, WRITE);
      try {
        final var lock = channel.lock();
        return new DatabaseWriteLock(channel, lock);
      } catch (final IOException e) {
        try {
          channel.close();
        } catch (final IOException ex) {
          e.addSuppressed(ex);
        }
        throw e;
      }
    }

    @Override
    public void close()
      throws WXMException
    {
      final var exceptions = new WXMExceptions();

      try {
        this.lock.close();
      } catch (final IOException e) {
        exceptions.add(e);
      }

      try {
        this.channel.close();
      } catch (final IOException e) {
        exceptions.add(e);
      }

      exceptions.throwIfRequired();
    }

    @Override
    public String toString()
    {
      return String.format(
        "[DatabaseWriteLock 0x%s]",
        Long.toUnsignedString(
          System.identityHashCode(this),
          16)
      );
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMVirtualMachineDatabase 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
