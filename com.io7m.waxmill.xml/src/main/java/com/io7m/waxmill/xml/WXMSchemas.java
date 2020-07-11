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

package com.io7m.waxmill.xml;

import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;

import java.net.URI;

/**
 * Functions to access XML schemas.
 */

public final class WXMSchemas
{
  private static final JXESchemaDefinition VM_SCHEMA_M1P0 =
    JXESchemaDefinition.of(
      URI.create("urn:com.io7m.waxmill.vm:1:0"),
      "vm-1.0.xsd",
      WXMSchemas.class.getResource("/com/io7m/waxmill/xml/vm/v1/vm-1.0.xsd")
    );

  private static final JXESchemaDefinition CONFIG_SCHEMA_M1P0 =
    JXESchemaDefinition.of(
      URI.create("urn:com.io7m.waxmill.config:1:0"),
      "config-1.0.xsd",
      WXMSchemas.class.getResource(
        "/com/io7m/waxmill/xml/config/v1/config-1.0.xsd")
    );

  private static final JXESchemaResolutionMappings SCHEMAS =
    JXESchemaResolutionMappings.builder()
      .putMappings(URI.create("urn:com.io7m.waxmill.vm:1:0"), VM_SCHEMA_M1P0)
      .putMappings(
        URI.create("urn:com.io7m.waxmill.config:1:0"),
        CONFIG_SCHEMA_M1P0)
      .build();

  private WXMSchemas()
  {

  }

  /**
   * @return The VM version 1.0 XML namespace
   */

  public static String vmSchemaV1p0NamespaceText()
  {
    return vmSchemaV1p0Namespace().toString();
  }

  /**
   * @return The VM version 1.0 XML namespace
   */

  public static URI vmSchemaV1p0Namespace()
  {
    return vmSchemaV1p0().namespace();
  }

  /**
   * @return The VM version 1.0 schema
   */

  public static JXESchemaDefinition vmSchemaV1p0()
  {
    return VM_SCHEMA_M1P0;
  }

  /**
   * @return The Configuration version 1.0 XML namespace
   */

  public static String configSchemaV1p0NamespaceText()
  {
    return configSchemaV1p0Namespace().toString();
  }

  /**
   * @return The Configuration version 1.0 XML namespace
   */

  public static URI configSchemaV1p0Namespace()
  {
    return configSchemaV1p0().namespace();
  }

  /**
   * @return The Configuration version 1.0 schema
   */

  public static JXESchemaDefinition configSchemaV1p0()
  {
    return CONFIG_SCHEMA_M1P0;
  }

  /**
   * @return The collection of supported schemas
   */

  public static JXESchemaResolutionMappings schemas()
  {
    return SCHEMAS;
  }
}
