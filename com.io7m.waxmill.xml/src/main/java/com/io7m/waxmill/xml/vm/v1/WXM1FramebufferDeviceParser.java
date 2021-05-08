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

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.machines.WXMDeviceFramebuffer;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.net.InetAddress;
import java.util.Map;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceFramebufferType.WXMVGAConfiguration;
import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

/**
 * "FramebufferDevice" parser.
 */

public final class WXM1FramebufferDeviceParser
  implements BTElementHandlerType<Object, WXMDeviceFramebuffer>
{
  private final WXMDeviceFramebuffer.Builder builder;

  /**
   * "FramebufferDevice" parser.
   */

  public WXM1FramebufferDeviceParser()
  {
    this.builder = WXMDeviceFramebuffer.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("DeviceSlot"),
        c -> new WXM1DeviceSlotParser()
      ),
      Map.entry(
        element("Comment"),
        c -> new WXM1CommentParser()
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof WXM1Comment) {
      this.builder.setComment(((WXM1Comment) result).text());
    } else if (result instanceof WXMDeviceSlot) {
      this.builder.setDeviceSlot((WXMDeviceSlot) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setWidth(
        Integer.parseInt(attributes.getValue("width").trim())
      );
      this.builder.setHeight(
        Integer.parseInt(attributes.getValue("height").trim())
      );
      this.builder.setWaitForVNC(
        Boolean.parseBoolean(attributes.getValue("waitForVNC").trim())
      );

      final var address =
        InetAddress.getByName(attributes.getValue("listenAddress").trim());
      final var port =
        Integer.parseInt(attributes.getValue("listenPort").trim());

      this.builder.setListenAddress(address);
      this.builder.setListenPort(port);

      this.builder.setVgaConfiguration(
        WXMVGAConfiguration.valueOf(
          attributes.getValue("vgaConfiguration").trim()
        )
      );
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXMDeviceFramebuffer onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
