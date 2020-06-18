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

package com.io7m.waxmill.database.api;

import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.client.api.WXMExceptionDuplicate;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import com.io7m.waxmill.client.api.WXMVirtualMachineSet;
import com.io7m.waxmill.client.api.WXMVirtualMachineSets;

import java.util.Optional;
import java.util.UUID;

/**
 * A virtual machine database.
 */

public interface WXMVirtualMachineDatabaseType extends WXMDatabaseType
{
  /**
   * Find an existing virtual machine with the given ID.
   *
   * @param machineId The virtual machine ID
   *
   * @return The virtual machine, if any
   *
   * @throws WXMException On errors
   */

  Optional<WXMVirtualMachine> vmGet(
    UUID machineId)
    throws WXMException;

  /**
   * Define a set of new virtual machines.
   *
   * @param machines The virtual machine
   *
   * @throws WXMExceptionDuplicate If one or more virtual machines already exist
   * @throws WXMException          On errors
   */

  void vmDefineAll(
    WXMVirtualMachineSet machines)
    throws WXMException, WXMExceptionDuplicate;

  /**
   * Define a new virtual machine.
   *
   * @param machine The virtual machine
   *
   * @throws WXMExceptionDuplicate If one or more virtual machines already exist
   * @throws WXMException          On errors
   */

  default void vmDefine(
    final WXMVirtualMachine machine)
    throws WXMException, WXMExceptionDuplicate
  {
    this.vmDefineAll(WXMVirtualMachineSets.one(machine));
  }

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

  WXMVirtualMachineSet vmList()
    throws WXMException;
}
