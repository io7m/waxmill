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

package com.io7m.waxmill.tests.cmdline;

import com.beust.jcommander.Parameter;
import com.io7m.claypot.core.CLPCommandType;
import com.io7m.waxmill.cmdline.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class WXMDocumentationGeneration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMDocumentationGeneration.class);

  private WXMDocumentationGeneration()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var command =
      new Main(args)
        .commands()
        .get("vm-add-virtio-network-device");

    final var fields = new TreeSet<ParameterField>();
    collectFields(command, command.getClass(), fields);

    final var xmlOutputs = XMLOutputFactory.newFactory();
    final var writer = xmlOutputs.createXMLStreamWriter(System.out);

    writer.writeStartDocument();
    writer.writeStartElement("Table");
    writer.writeAttribute("type", "parametersTable");
    writer.writeCharacters(System.lineSeparator());

    writeColumns(writer);
    writer.writeCharacters(System.lineSeparator());

    for (final var field : fields) {
      writeRow(writer, field);
      writer.writeCharacters(System.lineSeparator());
    }

    writer.writeEndElement();
    writer.writeEndDocument();

    writer.flush();
    System.out.flush();
  }

  private static void writeRow(
    final XMLStreamWriter writer,
    final ParameterField field)
    throws Exception
  {
    writer.writeStartElement("Row");

    writer.writeStartElement("Cell");
    writer.writeStartElement("Term");
    writer.writeAttribute("type", "parameter");
    writer.writeCharacters(field.parameter.names()[0]);
    writer.writeEndElement();
    writer.writeEndElement();

    writer.writeStartElement("Cell");
    writer.writeStartElement("Term");
    writer.writeAttribute("type", "parameterType");
    writer.writeCharacters(field.field.getGenericType().getTypeName());
    writer.writeEndElement();
    writer.writeEndElement();

    writer.writeStartElement("Cell");
    writer.writeStartElement("Term");
    writer.writeAttribute("type", "constant");
    writer.writeCharacters(String.valueOf(field.parameter.required()));
    writer.writeEndElement();
    writer.writeEndElement();

    writer.writeStartElement("Cell");
    writer.writeCharacters(field.parameter.description());
    writer.writeEndElement();

    writer.writeEndElement();
  }

  private static void writeColumns(
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    writer.writeStartElement("Columns");
    writer.writeStartElement("Column");
    writer.writeCharacters("Parameter");
    writer.writeEndElement();
    writer.writeStartElement("Column");
    writer.writeCharacters("Type");
    writer.writeEndElement();
    writer.writeStartElement("Column");
    writer.writeCharacters("Required");
    writer.writeEndElement();
    writer.writeStartElement("Column");
    writer.writeCharacters("Description");
    writer.writeEndElement();
    writer.writeEndElement();
  }

  private static final class ParameterField implements Comparable<ParameterField>
  {
    private final CLPCommandType command;
    private final Field field;
    private final Parameter parameter;

    ParameterField(
      final CLPCommandType inCommand,
      final Field inField,
      final Parameter inParameter)
    {
      this.command =
        Objects.requireNonNull(inCommand, "command");
      this.field =
        Objects.requireNonNull(inField, "field");
      this.parameter =
        Objects.requireNonNull(inParameter, "parameter");
    }

    @Override
    public int compareTo(
      final ParameterField other)
    {
      return this.parameter.names()[0].compareTo(other.parameter.names()[0]);
    }
  }

  private static void collectFields(
    final CLPCommandType command,
    final Class<?> aClass,
    final Set<ParameterField> fields)
  {
    for (final var field : aClass.getDeclaredFields()) {
      collectField(command, field, fields);
    }

    if (!Objects.equals(aClass.getSuperclass(), Object.class)) {
      collectFields(command, aClass.getSuperclass(), fields);
    }
  }

  private static void collectField(
    final CLPCommandType command,
    final Field field,
    final Set<ParameterField> fields)
  {
    final var annotated = field.getAnnotation(Parameter.class);
    if (annotated == null) {
      return;
    }
    fields.add(new ParameterField(command, field, annotated));
  }
}
