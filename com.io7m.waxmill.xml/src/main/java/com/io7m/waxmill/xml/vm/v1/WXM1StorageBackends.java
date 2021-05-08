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

import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMOpenOption;
import com.io7m.waxmill.machines.WXMSectorSizes;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.xml.WXMSchemas;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Functions over storage backends.
 */

public final class WXM1StorageBackends
{
  private WXM1StorageBackends()
  {

  }

  /**
   * Serialize a storage backend.
   *
   * @param backend The backend
   * @param writer  An XML writer
   *
   * @throws XMLStreamException On errors
   */

  public static void serializeStorageBackend(
    final WXMDeviceType.WXMStorageBackendType backend,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    switch (backend.kind()) {
      case WXM_STORAGE_FILE:
        serializeStorageBackendFile(
          (WXMStorageBackendFile) backend, writer);
        break;
      case WXM_STORAGE_ZFS_VOLUME:
        serializeStorageBackendZFSVolume(
          (WXMStorageBackendZFSVolume) backend, writer);
        break;
      case WXM_SCSI:
        serializeSCSI(backend);
        break;
    }
  }

  private static void serializeStorageBackendZFSVolume(
    final WXMStorageBackendZFSVolume backend,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "StorageBackendZFSVolume");

    final Optional<BigInteger> expectedSizeOpt = backend.expectedSize();
    if (expectedSizeOpt.isPresent()) {
      final var expectedSize = expectedSizeOpt.get();
      writer.writeAttribute("expectedSize", expectedSize.toString());
    }

    WXM1Comments.serializeComment(backend.comment(), writer);
    writer.writeEndElement();
  }

  private static void serializeSCSI(
    final Object backend)
  {
    Objects.requireNonNull(backend, "backend");
    throw new UnimplementedCodeException();
  }

  private static void serializeStorageBackendFile(
    final WXMStorageBackendFile backend,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "StorageBackendFile");
    writer.writeAttribute("path", backend.file().toString());
    WXM1Comments.serializeComment(backend.comment(), writer);
    serializeOpenOptions(backend.options(), writer);
    serializeSectorSizes(backend.sectorSizes(), writer);
    writer.writeEndElement();
  }

  private static void serializeSectorSizes(
    final Optional<WXMSectorSizes> sectorSizesOpt,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    if (sectorSizesOpt.isPresent()) {
      final var sectorSizes = sectorSizesOpt.get();
      final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
      writer.writeStartElement(namespaceURI, "SectorSizes");
      writer.writeAttribute("logical", sectorSizes.logical().toString());
      writer.writeAttribute("physical", sectorSizes.physical().toString());
      writer.writeEndElement();
    }
  }

  private static void serializeOpenOptions(
    final Set<WXMOpenOption> options,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    if (!options.isEmpty()) {
      final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
      writer.writeStartElement(namespaceURI, "OpenOptions");
      for (final var option : options) {
        writer.writeStartElement(namespaceURI, "OpenOption");
        writer.writeAttribute("value", option.name());
        writer.writeEndElement();
      }
      writer.writeEndElement();
    }
  }
}
