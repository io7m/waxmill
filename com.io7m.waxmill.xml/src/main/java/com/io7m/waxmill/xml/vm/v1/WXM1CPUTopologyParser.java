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

package com.io7m.waxmill.xml.vm.v1;

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.machines.WXMCPUTopology;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.util.Map;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

/**
 * CPU topology parser.
 */

public final class WXM1CPUTopologyParser
  implements BTElementHandlerType<Object, WXMCPUTopology>
{
  private final WXMCPUTopology.Builder builder;

  /**
   * CPU topology parser.
   */

  public WXM1CPUTopologyParser()
  {
    this.builder =
      WXMCPUTopology.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXParseException
  {
    try {
      this.builder.setCores(
        Integer.parseInt(attributes.getValue("cores")));
      this.builder.setThreads(
        Integer.parseInt(attributes.getValue("threads")));
      this.builder.setSockets(
        Integer.parseInt(attributes.getValue("sockets")));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(element("Comment"), c -> new WXM1CommentParser()),
      Map.entry(element("PinCPUs"), c -> new WXM1PinCPUsParser())
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof WXM1Comment) {
      this.builder.setComment(((WXM1Comment) result).text());
    } else if (result instanceof WXM1PinCPUs) {
      this.builder.addAllPinnedCPUs(((WXM1PinCPUs) result).cpus());
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public WXMCPUTopology onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
