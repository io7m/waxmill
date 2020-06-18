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

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public final class WXMMessages
{
  private final ResourceBundle resources;

  private WXMMessages(
    final ResourceBundle inResources)
  {
    this.resources = Objects.requireNonNull(inResources, "inResources");
  }

  public static WXMMessages create()
    throws IOException
  {
    try (var stream = WXMMessages.class.getResourceAsStream(
      "/com/io7m/waxmill/cmdline/internal/Messages.xml")) {
      return new WXMMessages(new XMLResourceBundle(stream));
    }
  }

  public String format(
    final String id,
    final Object... args)
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(args, "args");
    return MessageFormat.format(this.resources.getString(id), args);
  }

  private static final class XMLResourceBundle extends ResourceBundle
  {
    private final Properties props;

    XMLResourceBundle(
      final InputStream stream)
      throws IOException
    {
      this.props = new Properties();
      this.props.loadFromXML(
        Objects.requireNonNull(stream, "stream")
      );
    }

    @Override
    protected Object handleGetObject(
      final String key)
    {
      return this.props.getProperty(
        Objects.requireNonNull(key, "key")
      );
    }

    @Override
    public Enumeration<String> getKeys()
    {
      final Set<String> handleKeys = this.props.stringPropertyNames();
      return Collections.enumeration(handleKeys);
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMMessages 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
