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

import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.xml.WXMSchemas;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public final class WXM1DeviceSlots
{
  private WXM1DeviceSlots()
  {

  }

  public static void serializeDeviceSlot(
    final WXMDeviceSlot deviceSlot,
    final SlotSide side,
    final XMLStreamWriter writer)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    switch (side) {
      case HOST:
        writer.writeStartElement(namespaceURI, "HostDeviceSlot");
        break;
      case GUEST:
        writer.writeStartElement(namespaceURI, "DeviceSlot");
        break;
    }

    writer.writeAttribute(
      "bus",
      String.valueOf(deviceSlot.busID()));
    writer.writeAttribute(
      "slot",
      String.valueOf(deviceSlot.slotID()));
    writer.writeAttribute(
      "function",
      String.valueOf(deviceSlot.functionID()));
    writer.writeEndElement();
  }

  /**
   * The side of emulation upon which the device slot lives.
   */

  public enum SlotSide
  {
    /**
     * The slot is a host PCI slot.
     */

    HOST,

    /**
     * The slot is a guest PCI slot.
     */

    GUEST
  }
}