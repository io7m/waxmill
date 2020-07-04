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

package com.io7m.waxmill.cmdline.internal;

import com.io7m.waxmill.client.api.WXMApplicationVersion;
import com.io7m.waxmill.client.api.WXMApplicationVersions;
import com.io7m.waxmill.client.api.WXMClientProviderType;
import com.io7m.waxmill.parser.api.WXMBootConfigurationParserProviderType;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserProviderType;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.serializer.api.WXMVirtualMachineSerializerProviderType;

import java.io.IOException;
import java.net.URL;
import java.util.ServiceLoader;

final class WXMServices
{
  private WXMServices()
  {

  }

  public static WXMClientProviderType clients()
  {
    return findService(WXMClientProviderType.class);
  }

  public static WXMVirtualMachineSerializerProviderType vmSerializers()
  {
    return findService(WXMVirtualMachineSerializerProviderType.class);
  }

  public static WXMVirtualMachineParserProviderType vmParsers()
  {
    return findService(WXMVirtualMachineParserProviderType.class);
  }

  public static WXMBootConfigurationParserProviderType bootConfigurationParsers()
  {
    return findService(WXMBootConfigurationParserProviderType.class);
  }

  public static WXMProcessesType processes()
  {
    return findService(WXMProcessesType.class);
  }

  private static <T> T findService(
    final Class<T> service)
  {
    return ServiceLoader.load(service)
      .findFirst()
      .orElseThrow(() -> missingService(service));
  }

  private static IllegalStateException missingService(
    final Class<?> clazz)
  {
    return new IllegalStateException(String.format(
      "No available implementations of service: %s",
      clazz.getCanonicalName()));
  }

  public static WXMApplicationVersion findApplicationVersion()
    throws IOException
  {
    final URL resource =
      WXMServices.class.getResource(
        "/com/io7m/waxmill/cmdline/internal/version.properties"
      );

    try (var stream = resource.openStream()) {
      return WXMApplicationVersions.ofStream(stream);
    }
  }
}
