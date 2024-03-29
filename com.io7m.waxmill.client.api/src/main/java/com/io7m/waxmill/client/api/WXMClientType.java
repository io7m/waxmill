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

package com.io7m.waxmill.client.api;

import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.machines.WXMVirtualMachineSets;
import com.io7m.waxmill.process.api.WXMProcessDescription;

import java.util.Optional;
import java.util.UUID;

/**
 * A client.
 */

public interface WXMClientType extends AutoCloseable
{
  @Override
  void close()
    throws WXMException;

  /**
   * List the available virtual machines.
   *
   * @return A set of machines
   *
   * @throws WXMException On errors
   */

  WXMVirtualMachineSet vmList()
    throws WXMException;

  /**
   * Find a virtual machine with the given ID.
   *
   * @param id The ID of the machine
   *
   * @return A virtual machine
   *
   * @throws WXMException On errors
   */

  WXMVirtualMachine vmFind(
    UUID id)
    throws WXMException;

  /**
   * Find a virtual machine with the given ID.
   *
   * @param id The ID of the machine
   *
   * @return A virtual machine, if one exists
   *
   * @throws WXMException On errors
   */

  Optional<WXMVirtualMachine> vmFindOptional(
    UUID id)
    throws WXMException;

  /**
   * Update an existing virtual machine.
   *
   * @param machine The virtual machine
   *
   * @throws WXMException On errors
   */

  void vmUpdate(
    WXMVirtualMachine machine)
    throws WXMException;

  /**
   * @return The configuration used to open the client
   */

  WXMClientConfiguration configuration();

  /**
   * Define a set of new virtual machines.
   *
   * @param machines The virtual machine
   *
   * @throws WXMException On errors
   */

  void vmDefineAll(
    WXMVirtualMachineSet machines)
    throws WXMException;

  /**
   * Define a new virtual machine.
   *
   * @param machine The virtual machine
   *
   * @throws WXMException On errors
   */

  default void vmDefine(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    this.vmDefineAll(WXMVirtualMachineSets.one(machine));
  }

  /**
   * Start a virtual machine. If the startup succeeds, this method never
   * returns and the current process is replaced with that of the running
   * virtual machine.
   *
   * @param machine               The virtual machine
   * @param bootConfigurationName The boot configuration used
   * @param dryRun                Whether or not the operation is a dry run
   *
   * @throws WXMException         On errors
   * @throws InterruptedException If the operation was interrupted
   */

  void vmRun(
    WXMVirtualMachine machine,
    WXMBootConfigurationName bootConfigurationName,
    WXMDryRun dryRun)
    throws WXMException, InterruptedException;

  /**
   * Delete the configuration for a virtual machine.
   *
   * @param id The machine ID
   *
   * @throws WXMException On errors
   */

  void vmDelete(UUID id)
    throws WXMException;

  /**
   * Return the primary console device of the given virtual machine. The method
   * will return nothing if the machine has no console device, or if the
   * machine has more than one potential console.
   *
   * @param machine The virtual machine
   *
   * @return A process execution
   */

  Optional<WXMDeviceType> vmConsoleGet(
    WXMVirtualMachine machine);

  /**
   * Return a process description that, when executed, will give access to
   * the primary console of the given virtual machine. The method will return
   * nothing if the machine has no console device, or if the machine has more
   * than one potential console.
   *
   * @param machine The virtual machine
   *
   * @return A process execution
   */

  Optional<WXMProcessDescription> vmConsole(
    WXMVirtualMachine machine);

  /**
   * Realize a virtual machine.
   *
   * @param machine The virtual machine
   * @param dryRun  Whether or not the operation is a dry run
   *
   * @throws WXMException On errors
   */

  void vmRealize(
    WXMVirtualMachine machine,
    WXMDryRun dryRun)
    throws WXMException;

  /**
   * Kill a running virtual machine.
   *
   * @param dryRun  Whether or not the operation is a dry run
   * @param machine The virtual machine
   */

  void vmKill(
    WXMVirtualMachine machine,
    WXMDryRun dryRun)
    throws WXMException;
}
