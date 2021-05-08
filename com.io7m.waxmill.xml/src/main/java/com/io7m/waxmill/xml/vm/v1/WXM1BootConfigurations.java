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

import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMBootDiskAttachment;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.xml.WXMSchemas;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static com.io7m.waxmill.machines.WXMSide.GUEST;

/**
 * Functions over boot configurations.
 */

public final class WXM1BootConfigurations
{
  private WXM1BootConfigurations()
  {

  }

  /**
   * Serialize boot configurations.
   *
   * @param bootConfigurations The boot configurations
   * @param writer             The XML writer
   * @param root               {@code true} if this is the root element
   *
   * @throws XMLStreamException On errors
   */

  public static void serializeBootConfigurations(
    final List<WXMBootConfigurationType> bootConfigurations,
    final XMLStreamWriter writer,
    final boolean root)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "BootConfigurations");

    if (root) {
      writer.writeNamespace("wxm", namespaceURI);
    }

    final var iter =
      bootConfigurations.stream()
        .sorted(Comparator.comparing(WXMBootConfigurationType::name))
        .iterator();

    while (iter.hasNext()) {
      final var configuration = iter.next();
      switch (configuration.kind()) {
        case GRUB_BHYVE:
          serializeBootConfigurationGRUBBhyve(
            (WXMBootConfigurationGRUBBhyve) configuration, writer);
          break;
        case UEFI:
          serializeBootConfigurationUEFI(
            (WXMBootConfigurationUEFI) configuration, writer);
          break;
      }
    }

    writer.writeEndElement();
  }

  private static void serializeBootConfigurationUEFI(
    final WXMBootConfigurationUEFI configuration,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "BootConfigurationUEFI");
    writer.writeAttribute("name", configuration.name().value());
    writer.writeAttribute("firmware", configuration.firmware().toString());

    WXM1Comments.serializeComment(configuration.comment(), writer);
    serializeBootDiskAttachments(configuration.diskAttachments(), writer);
    writer.writeEndElement();
  }

  private static void serializeBootConfigurationGRUBBhyve(
    final WXMBootConfigurationGRUBBhyve configuration,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "BootConfigurationGRUBBhyve");
    writer.writeAttribute("name", configuration.name().value());
    WXM1Comments.serializeComment(configuration.comment(), writer);
    serializeBootDiskAttachments(configuration.diskAttachments(), writer);

    final var instructions = configuration.kernelInstructions();
    switch (instructions.kind()) {
      case KERNEL_OPENBSD:
        serializeGRUBKernelOpenBSD((WXMGRUBKernelOpenBSD) instructions, writer);
        break;
      case KERNEL_LINUX:
        serializeGRUBKernelLinux((WXMGRUBKernelLinux) instructions, writer);
        break;
    }

    writer.writeEndElement();
  }

  private static void serializeBootDiskAttachments(
    final List<WXMBootDiskAttachment> diskAttachments,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    if (!diskAttachments.isEmpty()) {
      final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
      writer.writeStartElement(namespaceURI, "BootDiskAttachments");
      for (final var attachment : diskAttachments) {
        serializeBootDiskAttachment(attachment, writer);
      }
      writer.writeEndElement();
    }
  }

  private static void serializeBootDiskAttachment(
    final WXMBootDiskAttachment attachment,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "BootDiskAttachment");
    WXM1DeviceSlots.serializeDeviceSlot(attachment.device(), GUEST, writer);
    WXM1StorageBackends.serializeStorageBackend(attachment.backend(), writer);
    writer.writeEndElement();
  }

  private static void serializeGRUBKernelOpenBSD(
    final WXMGRUBKernelOpenBSD kernel,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "GRUBBhyveKernelOpenBSD");
    serializeBSDBootDevice(
      kernel.kernelPath(),
      kernel.partition(),
      kernel.bootDevice(),
      writer
    );
    writer.writeEndElement();
  }

  private static void serializeBSDBootDevice(
    final Path kernelPath,
    final String partition,
    final WXMDeviceSlot bootDevice,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "BSDBootDevice");
    writer.writeAttribute("kernelPath", kernelPath.toString());
    writer.writeAttribute("partition", partition);
    WXM1DeviceSlots.serializeDeviceSlot(bootDevice, GUEST, writer);
    writer.writeEndElement();
  }

  private static void serializeGRUBKernelLinux(
    final WXMGRUBKernelLinux kernel,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "GRUBBhyveKernelLinux");
    serializeLinuxKernelDevice(kernel, writer);
    for (final var argument : kernel.kernelArguments()) {
      writer.writeStartElement(namespaceURI, "LinuxKernelArgument");
      writer.writeAttribute("value", argument);
      writer.writeEndElement();
    }
    serializeLinuxInitRDDevice(kernel, writer);
    writer.writeEndElement();
  }

  private static void serializeLinuxInitRDDevice(
    final WXMGRUBKernelLinux kernel,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "LinuxInitRDDevice");
    writer.writeAttribute("initRDPath", kernel.initRDPath().toString());
    WXM1DeviceSlots.serializeDeviceSlot(kernel.initRDDevice(), GUEST, writer);
    writer.writeEndElement();
  }

  private static void serializeLinuxKernelDevice(
    final WXMGRUBKernelLinux kernel,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    writer.writeStartElement(namespaceURI, "LinuxKernelDevice");
    writer.writeAttribute("kernelPath", kernel.kernelPath().toString());
    WXM1DeviceSlots.serializeDeviceSlot(kernel.kernelDevice(), GUEST, writer);
    writer.writeEndElement();
  }
}
