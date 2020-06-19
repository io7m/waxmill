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

package com.io7m.waxmill.machines;

import java.util.Collection;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Functions over sets of virtual machines.
 */

public final class WXMVirtualMachineSets
{
  private WXMVirtualMachineSets()
  {

  }

  /**
   * Create a set consisting of a single machine.
   *
   * @param machine The machine
   *
   * @return A single-machine set
   */

  public static WXMVirtualMachineSet one(
    final WXMVirtualMachine machine)
  {
    Objects.requireNonNull(machine, "machine");

    final var machines = new TreeMap<UUID, WXMVirtualMachine>();
    machines.put(machine.id(), machine);
    return WXMVirtualMachineSet.builder()
      .setMachines(machines)
      .build();
  }

  /**
   * Create a set consisting of the union of all of the given sets.
   *
   * @param machineSets The sets of machines
   *
   * @return A set consisting of all of the machines in all sets
   *
   * @throws WXMException If two machines have the same ID
   */

  public static WXMVirtualMachineSet merge(
    final Collection<WXMVirtualMachineSet> machineSets)
    throws WXMException
  {
    Objects.requireNonNull(machineSets, "machineSets");

    final var exceptions = new WXMExceptions();
    final var machines = new TreeMap<UUID, WXMVirtualMachine>();
    for (final var machineSet : machineSets) {
      for (final var machine : machineSet.machines().values()) {
        final var machineId = machine.id();
        if (!machines.containsKey(machineId)) {
          machines.put(machineId, machine);
          continue;
        }

        final var existing = machines.get(machineId);
        exceptions.add(
          new WXMException(errorMachineConflict(machine, existing))
        );
      }
    }

    exceptions.throwIfRequired();
    return WXMVirtualMachineSet.builder()
      .setMachines(machines)
      .build();
  }

  private static String errorMachineConflict(
    final WXMVirtualMachine machine0,
    final WXMVirtualMachine machine1)
  {
    final var lineSeparator = System.lineSeparator();
    final var builder = new StringBuilder(128);
    builder
      .append("One or more virtual machines encountered with the same ID.")
      .append(lineSeparator);

    builder.append("  ID: ")
      .append(machine0.id())
      .append(lineSeparator);

    builder.append("  Machine: ")
      .append(machine0.name().value())
      .append(lineSeparator);

    machine0.configurationFile().ifPresent(source -> {
      builder.append("  Source: ")
        .append(source)
        .append(lineSeparator);
    });

    builder.append("  Machine: ")
      .append(machine1.name().value())
      .append(lineSeparator);

    machine1.configurationFile().ifPresent(source -> {
      builder.append("  Source: ")
        .append(source)
        .append(lineSeparator);
    });

    return builder.toString();
  }
}
