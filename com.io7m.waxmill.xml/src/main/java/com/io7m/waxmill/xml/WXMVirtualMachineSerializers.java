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

import com.io7m.waxmill.client.api.WXMVirtualMachineSet;
import com.io7m.waxmill.serializer.api.WXMSerializerType;
import com.io7m.waxmill.serializer.api.WXMVirtualMachineSerializerProviderType;
import com.io7m.waxmill.xml.vm.v1.WXM1VirtualMachineSerializer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

/**
 * A provider of virtual machine serializers.
 */

public final class WXMVirtualMachineSerializers
  implements WXMVirtualMachineSerializerProviderType
{
  private final XMLOutputFactory serializers;
  private final TransformerFactory transformers;

  /**
   * Construct a provider.
   */

  public WXMVirtualMachineSerializers()
  {
    this.serializers = XMLOutputFactory.newFactory();
    this.transformers = TransformerFactory.newInstance();
    this.transformers.setAttribute("indent-number", Integer.valueOf(4));
  }

  @Override
  public WXMSerializerType create(
    final URI uri,
    final OutputStream stream,
    final WXMVirtualMachineSet value)
    throws IOException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(value, "value");

    try {
      final var transformer = this.transformers.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(
        "{http://xml.apache.org/xslt}indent-amount",
        "4");

      final var bufferedOutput =
        new ByteArrayOutputStream();
      final var writer =
        this.serializers.createXMLStreamWriter(bufferedOutput, "UTF-8");

      return new WXM1VirtualMachineSerializer(
        writer,
        bufferedOutput,
        stream,
        transformer,
        value
      );
    } catch (final XMLStreamException | TransformerConfigurationException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMVirtualMachineSerializers 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
