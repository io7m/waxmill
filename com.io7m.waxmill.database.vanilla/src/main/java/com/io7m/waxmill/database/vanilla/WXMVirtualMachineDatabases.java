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

package com.io7m.waxmill.database.vanilla;

import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.database.api.WXMDatabaseConfiguration;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseProviderType;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseType;
import com.io7m.waxmill.database.vanilla.internal.WXMVirtualMachineDatabase;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserProviderType;
import com.io7m.waxmill.serializer.api.WXMVirtualMachineSerializerProviderType;

import java.util.Objects;
import java.util.ServiceLoader;

public final class WXMVirtualMachineDatabases
  implements WXMVirtualMachineDatabaseProviderType
{
  private final WXMVirtualMachineParserProviderType parsers;
  private final WXMVirtualMachineSerializerProviderType serializers;

  public WXMVirtualMachineDatabases(
    final WXMVirtualMachineParserProviderType inParsers,
    final WXMVirtualMachineSerializerProviderType inSerializers)
  {
    this.parsers =
      Objects.requireNonNull(inParsers, "inParsers");
    this.serializers =
      Objects.requireNonNull(inSerializers, "inSerializers");
  }

  public WXMVirtualMachineDatabases()
  {
    this(
      requireService(WXMVirtualMachineParserProviderType.class),
      requireService(WXMVirtualMachineSerializerProviderType.class)
    );
  }

  private static <T> T requireService(
    final Class<T> clazz)
  {
    return ServiceLoader.load(clazz)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException(
        String.format(
          "No available services of type %s", clazz.getCanonicalName()
        ))
      );
  }

  @Override
  public WXMVirtualMachineDatabaseType open(
    final WXMDatabaseConfiguration configuration)
    throws WXMException
  {
    Objects.requireNonNull(configuration, "configuration");
    return WXMVirtualMachineDatabase.open(
      this.parsers,
      this.serializers,
      configuration
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMVirtualMachineDatabases 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
