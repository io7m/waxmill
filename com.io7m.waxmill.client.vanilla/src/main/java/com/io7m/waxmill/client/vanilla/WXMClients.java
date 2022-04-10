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

package com.io7m.waxmill.client.vanilla;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.client.api.WXMClientProviderType;
import com.io7m.waxmill.client.api.WXMClientType;
import com.io7m.waxmill.client.vanilla.internal.WXMClient;
import com.io7m.waxmill.database.api.WXMDatabaseConfiguration;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseProviderType;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.parser.api.WXMClientConfigurationParserProviderType;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.serializer.api.WXMClientConfigurationSerializerProviderType;

import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * The default client provider.
 */

public final class WXMClients implements WXMClientProviderType
{
  private final WXMClientConfigurationParserProviderType clientConfigurationParsers;
  private final WXMVirtualMachineDatabaseProviderType databases;
  private final WXMProcessesType processes;

  /**
   * The default client provider.
   *
   * @param inClientConfigurationParsers     The provider of configuration parsers
   * @param inClientConfigurationSerializers The provider of configuration serializers
   * @param inDatabases                      The provider of databases
   * @param inProcesses                      The provider of processes
   */

  public WXMClients(
    final WXMClientConfigurationParserProviderType inClientConfigurationParsers,
    final WXMClientConfigurationSerializerProviderType inClientConfigurationSerializers,
    final WXMVirtualMachineDatabaseProviderType inDatabases,
    final WXMProcessesType inProcesses)
  {
    this.clientConfigurationParsers =
      Objects.requireNonNull(
        inClientConfigurationParsers,
        "clientConfigurationParsers");
    Objects.requireNonNull(
      inClientConfigurationSerializers,
      "inClientConfigurationSerializers");
    this.databases =
      Objects.requireNonNull(inDatabases, "inDatabases");
    this.processes =
      Objects.requireNonNull(inProcesses, "inProcesses");
  }

  /**
   * The default client provider. Dependencies are resolved from {@link ServiceLoader}.
   */

  public WXMClients()
  {
    this(
      findService(WXMClientConfigurationParserProviderType.class),
      findService(WXMClientConfigurationSerializerProviderType.class),
      findService(WXMVirtualMachineDatabaseProviderType.class),
      findService(WXMProcessesType.class)
    );
  }

  private static <T> T findService(
    final Class<T> service)
  {
    return ServiceLoader.load(service)
      .findFirst()
      .orElseThrow(() -> missingService(service));
  }

  private static <T> IllegalStateException missingService(
    final Class<T> service)
  {
    return new IllegalStateException(
      String.format(
        "No available services of type: %s",
        service.getCanonicalName())
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMClients 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }

  @Override
  public WXMClientType open(
    final WXMClientConfiguration configuration)
    throws WXMException
  {
    final var databaseConfiguration =
      WXMDatabaseConfiguration.builder()
        .setDatabaseDirectory(configuration.virtualMachineConfigurationDirectory())
        .build();

    return new WXMClient(
      configuration,
      this.databases.open(databaseConfiguration),
      this.processes
    );
  }

  @Override
  public WXMClientType open(
    final Path configurationFile)
    throws WXMException
  {
    Objects.requireNonNull(configurationFile, "configurationFile");

    final WXMClientConfiguration configuration =
      this.clientConfigurationParsers.parse(configurationFile);
    return this.open(configuration);
  }
}
