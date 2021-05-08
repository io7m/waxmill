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
import com.io7m.waxmill.machines.WXMInterfaceGroupName;
import com.io7m.waxmill.machines.WXMMACAddressWithSide;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVMNetDeviceName;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.util.Map;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

/**
 * VMNet parser.
 */

public final class WXM1VMNetParser
  implements BTElementHandlerType<Object, WXMVMNet>
{
  private final WXMVMNet.Builder builder;

  /**
   * VMNet parser.
   */

  public WXM1VMNetParser()
  {
    this.builder = WXMVMNet.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Comment"),
        c -> new WXM1CommentParser()
      ),
      Map.entry(
        element("MACAddress"),
        c -> new WXM1MACAddressWithSideParser()
      ),
      Map.entry(
        element("InterfaceGroup"),
        c -> new WXM1InterfaceGroupParser()
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
    } else if (result instanceof WXMInterfaceGroupName) {
      this.builder.addGroups((WXMInterfaceGroupName) result);
    } else if (result instanceof WXMMACAddressWithSide) {
      this.onMACAddress((WXMMACAddressWithSide) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  private void onMACAddress(
    final WXMMACAddressWithSide mac)
  {
    switch (mac.side()) {
      case HOST:
        this.builder.setHostMAC(mac.value());
        return;
      case GUEST:
        this.builder.setGuestMAC(mac.value());
        return;
    }

    throw new UnreachableCodeException();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXParseException
  {
    try {
      this.builder.setName(
        WXMVMNetDeviceName.of(attributes.getValue("name"))
      );
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXMVMNet onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
