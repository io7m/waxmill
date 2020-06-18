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

package com.io7m.waxmill.client.api;

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
   * Define a new virtual machine.
   *
   * @param machine The virtual machine
   *
   * @throws WXMException On errors
   */

  void vmDefine(
    WXMVirtualMachine machine)
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
}
