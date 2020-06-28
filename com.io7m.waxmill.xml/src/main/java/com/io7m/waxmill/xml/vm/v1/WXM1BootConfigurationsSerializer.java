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

package com.io7m.waxmill.xml.vm.v1;

import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.serializer.api.WXMBootConfigurationsSerializerType;
import com.io7m.waxmill.xml.WXMSchemas;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class WXM1BootConfigurationsSerializer
  implements WXMBootConfigurationsSerializerType
{
  private static final byte[] XML_DECLARATION =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(UTF_8);

  private final XMLStreamWriter writer;
  private final ByteArrayOutputStream bufferedOutput;
  private final OutputStream stream;
  private final Transformer transformer;
  private final List<WXMBootConfigurationType> bootConfigurations;

  public WXM1BootConfigurationsSerializer(
    final XMLStreamWriter inWriter,
    final ByteArrayOutputStream inBufferedOutput,
    final OutputStream inStream,
    final Transformer inTransformer,
    final List<WXMBootConfigurationType> inBootConfigurations)
  {
    this.writer =
      Objects.requireNonNull(inWriter, "writer");
    this.bufferedOutput =
      Objects.requireNonNull(inBufferedOutput, "inBufferedOutput");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.transformer =
      Objects.requireNonNull(inTransformer, "transformer");
    this.bootConfigurations =
      Objects.requireNonNull(inBootConfigurations, "value");
  }

  @Override
  public void execute()
    throws IOException
  {
    try {
      this.start();
      WXM1BootConfigurations.serializeBootConfigurations(
        this.bootConfigurations, this.writer, true
      );
      this.finish();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  private void finish()
    throws XMLStreamException
  {
    this.writer.flush();
    this.writer.writeEndDocument();
  }

  private void start()
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartDocument("UTF-8", "1.0");
    this.writer.writeCharacters("\n");
    this.writer.setPrefix("wxm", namespaceURI);
  }

  @Override
  public void close()
    throws IOException
  {
    try {
      this.writer.flush();

      final var source =
        new StreamSource(
          new ByteArrayInputStream(this.bufferedOutput.toByteArray())
        );
      final var result =
        new StreamResult(this.stream);

      this.stream.write(XML_DECLARATION);
      this.stream.write(System.lineSeparator().getBytes(UTF_8));
      this.transformer.transform(source, result);
      this.stream.flush();
      this.stream.close();
    } catch (final XMLStreamException | TransformerException e) {
      throw new IOException(e);
    }
  }
}
