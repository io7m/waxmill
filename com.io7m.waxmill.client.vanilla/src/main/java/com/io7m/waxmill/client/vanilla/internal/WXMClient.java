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

package com.io7m.waxmill.client.vanilla.internal;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.client.api.WXMClientType;
import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import com.io7m.waxmill.client.api.WXMVirtualMachineSet;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseType;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WXMClient implements WXMClientType
{
  private final WXMClientConfiguration configuration;
  private final WXMVirtualMachineDatabaseType database;

  public WXMClient(
    final WXMClientConfiguration inConfiguration,
    final WXMVirtualMachineDatabaseType inDatabase)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");
  }

  @Override
  public void close()
    throws WXMException
  {
    this.database.close();
  }

  @Override
  public WXMVirtualMachineSet vmList()
    throws WXMException
  {
    return this.database.vmList();
  }

  @Override
  public WXMVirtualMachine vmFind(
    final UUID id)
    throws WXMException
  {
    Objects.requireNonNull(id, "id");

    final var existing = this.vmList();
    return Optional.ofNullable(existing.machines().get(id))
      .orElseThrow(() -> new WXMException(
        String.format("No such virtual machine: %s", id))
      );
  }

  @Override
  public void vmDefine(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    Objects.requireNonNull(machine, "machine");
    this.database.vmDefine(machine);
  }

  @Override
  public void vmUpdate(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    Objects.requireNonNull(machine, "machine");
    this.database.vmUpdate(machine);
  }

  @Override
  public WXMClientConfiguration configuration()
  {
    return this.configuration;
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMClient 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
