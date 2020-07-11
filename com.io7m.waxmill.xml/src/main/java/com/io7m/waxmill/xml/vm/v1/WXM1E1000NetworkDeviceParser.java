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
import com.io7m.waxmill.machines.WXMDeviceE1000;
import com.io7m.waxmill.machines.WXMDeviceSlot;

import java.util.Map;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMNetworkDeviceBackendType;
import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1E1000NetworkDeviceParser
  implements BTElementHandlerType<Object, WXMDeviceE1000>
{
  private final WXMDeviceE1000.Builder builder;

  public WXM1E1000NetworkDeviceParser()
  {
    this.builder = WXMDeviceE1000.builder();
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
      ),
      Map.entry(
        element("TAPDevice"),
        c -> new WXM1TapParser()
      ),
      Map.entry(
        element("VMNetDevice"),
        c -> new WXM1VMNetParser()
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
    } else if (result instanceof WXMNetworkDeviceBackendType) {
      this.builder.setBackend((WXMNetworkDeviceBackendType) result);
    } else if (result instanceof WXMDeviceSlot) {
      this.builder.setDeviceSlot((WXMDeviceSlot) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public WXMDeviceE1000 onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}