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

import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Device slot parser.
 */

public final class WXM1DeviceSlotParser
  implements BTElementHandlerType<Object, WXMDeviceSlot>
{
  private final WXMDeviceSlot.Builder builder;

  /**
   * Device slot parser.
   */

  public WXM1DeviceSlotParser()
  {
    this.builder = WXMDeviceSlot.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setBusID(
        Integer.parseInt(attributes.getValue("bus"))
      );
      this.builder.setFunctionID(
        Integer.parseInt(attributes.getValue("function"))
      );
      this.builder.setSlotID(
        Integer.parseInt(attributes.getValue("slot"))
      );
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXMDeviceSlot onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
