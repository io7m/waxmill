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

package com.io7m.waxmill.xml.config.v1;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.serializer.api.WXMSerializerType;
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
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * "Configuration" serializer.
 */

public final class WXM1ClientConfigurationSerializer
  implements WXMSerializerType
{
  private static final byte[] XML_DECLARATION =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(UTF_8);

  private final XMLStreamWriter writer;
  private final ByteArrayOutputStream bufferedOutput;
  private final OutputStream stream;
  private final Transformer transformer;
  private final WXMClientConfiguration clientConfiguration;

  /**
   * "Configuration" serializer.
   *
   * @param inWriter         The stream writer
   * @param inBufferedOutput The buffered output stream
   * @param inStream         The output stream
   * @param inTransformer    The XML transformer
   * @param inValue          The configuration value to serialize
   */

  public WXM1ClientConfigurationSerializer(
    final XMLStreamWriter inWriter,
    final ByteArrayOutputStream inBufferedOutput,
    final OutputStream inStream,
    final Transformer inTransformer,
    final WXMClientConfiguration inValue)
  {
    this.writer =
      Objects.requireNonNull(inWriter, "writer");
    this.bufferedOutput =
      Objects.requireNonNull(inBufferedOutput, "inBufferedOutput");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.transformer =
      Objects.requireNonNull(inTransformer, "transformer");
    this.clientConfiguration =
      Objects.requireNonNull(inValue, "value");
  }

  @Override
  public void execute()
    throws IOException
  {
    try {
      this.start();
      this.serializePaths();
      this.serializeZFSFilesystems();
      this.finish();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  private void serializeZFSFilesystems()
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.configSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "ZFSFilesystems");
    this.serializeZFSFilesystem(
      "VirtualMachineRuntimeFilesystem",
      this.clientConfiguration.virtualMachineRuntimeFilesystem()
    );
    this.writer.writeEndElement();
  }

  private void serializeZFSFilesystem(
    final String type,
    final WXMZFSFilesystem value)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.configSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "ZFSFilesystem");
    this.writer.writeAttribute("type", type);
    this.writer.writeAttribute("name", value.name());
    this.writer.writeAttribute("mountPoint", value.mountPoint().toString());
    this.writer.writeEndElement();
  }

  private void serializePaths()
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.configSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "Paths");
    this.serializePath(
      "VirtualMachineConfigurationDirectory",
      this.clientConfiguration.virtualMachineConfigurationDirectory()
    );
    this.serializePath(
      "BhyveExecutable",
      this.clientConfiguration.bhyveExecutable()
    );
    this.serializePath(
      "BhyveCtlExecutable",
      this.clientConfiguration.bhyveCtlExecutable()
    );
    this.serializePath(
      "GRUBBhyveExecutable",
      this.clientConfiguration.grubBhyveExecutable()
    );
    this.serializePath(
      "ZFSExecutable",
      this.clientConfiguration.zfsExecutable()
    );
    this.serializePath(
      "IfconfigExecutable",
      this.clientConfiguration.ifconfigExecutable()
    );
    this.serializePath(
      "CuExecutable",
      this.clientConfiguration.cuExecutable()
    );
    this.writer.writeEndElement();
  }

  private void serializePath(
    final String type,
    final Path value)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.configSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "Path");
    this.writer.writeAttribute("type", type);
    this.writer.writeAttribute("value", value.toString());
    this.writer.writeEndElement();
  }

  private void finish()
    throws XMLStreamException
  {
    this.writer.flush();
    this.writer.writeEndElement();
    this.writer.writeEndDocument();
  }

  private void start()
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.configSchemaV1p0NamespaceText();
    this.writer.writeStartDocument("UTF-8", "1.0");
    this.writer.writeCharacters("\n");
    this.writer.setPrefix("wxmc", namespaceURI);
    this.writer.writeStartElement(namespaceURI, "Configuration");
    this.writer.writeNamespace("wxmc", namespaceURI);
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
