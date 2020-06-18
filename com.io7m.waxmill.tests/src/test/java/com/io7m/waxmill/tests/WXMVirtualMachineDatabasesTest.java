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
import com.io7m.waxmill.client.api.WXMExceptionDuplicate;
import com.io7m.waxmill.client.api.WXMFlags;
import com.io7m.waxmill.client.api.WXMMachineName;
import com.io7m.waxmill.client.api.WXMMemory;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import com.io7m.waxmill.client.api.WXMVirtualMachineSet;
import com.io7m.waxmill.database.api.WXMDatabaseConfiguration;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseType;
import com.io7m.waxmill.database.vanilla.WXMVirtualMachineDatabases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class WXMVirtualMachineDatabasesTest
{
  private FileSystem filesystem;
  private FileSystemProvider provider;
  private Path directory;
  private WXMVirtualMachine virtualMachine0;
  private WXMVirtualMachineDatabaseType database;
  private Map<UUID, WXMVirtualMachine> virtualMachineOthers;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      WXMTestDirectories.createTempDirectory();
    this.database =
      new WXMVirtualMachineDatabases()
        .open(WXMDatabaseConfiguration.builder()
                .setDatabaseDirectory(this.directory)
                .build());

    this.virtualMachine0 =
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

    this.virtualMachineOthers =
      Stream.of(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID()
      ).map(uuid -> {
        return WXMVirtualMachine.builder()
          .setId(uuid)
          .setName(WXMMachineName.of(uuid.toString()))
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
      }).collect(Collectors.toMap(
        WXMVirtualMachine::id,
        Function.identity()
      ));
  }

  @Test
  public void defineExists()
    throws WXMException
  {
    this.database.vmDefine(this.virtualMachine0);

    assertEquals(
      this.virtualMachine0,
      this.database.vmGet(this.virtualMachine0.id())
        .orElseThrow()
        .withConfigurationFile(Optional.empty())
    );
  }

  @Test
  public void defineAllExists()
    throws WXMException
  {
    this.database.vmDefineAll(
      WXMVirtualMachineSet.builder()
        .setMachines(new TreeMap<>(this.virtualMachineOthers))
        .build()
    );

    for (final var machine : this.virtualMachineOthers.values()) {
      assertEquals(
        machine,
        this.database.vmGet(machine.id())
          .orElseThrow()
          .withConfigurationFile(Optional.empty())
      );
    }
  }

  @Test
  public void defineDuplicate()
    throws WXMException
  {
    this.database.vmDefine(this.virtualMachine0);

    assertThrows(WXMExceptionDuplicate.class, () -> {
      this.database.vmDefine(this.virtualMachine0);
    });
  }

  @Test
  public void updateExists()
    throws WXMException
  {
    this.database.vmUpdate(this.virtualMachine0);

    assertEquals(
      this.virtualMachine0,
      this.database.vmGet(this.virtualMachine0.id())
        .orElseThrow()
        .withConfigurationFile(Optional.empty())
    );
  }
}
