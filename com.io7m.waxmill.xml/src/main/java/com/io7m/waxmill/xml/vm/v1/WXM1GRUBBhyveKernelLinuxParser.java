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
import com.io7m.waxmill.machines.WXMDeviceID;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.nio.file.Paths;
import java.util.Map;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1GRUBBhyveKernelLinuxParser
  implements BTElementHandlerType<Object, WXMGRUBKernelLinux>
{
  private final WXMGRUBKernelLinux.Builder builder;

  public WXM1GRUBBhyveKernelLinuxParser()
  {
    this.builder = WXMGRUBKernelLinux.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("LinuxKernelArgument"),
        c -> new WXM1LinuxKernelArgumentParser()
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof String) {
      this.builder.addKernelArguments((String) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXParseException
  {
    try {
      this.builder.setInitRDPath(
        Paths.get(attributes.getValue("initRDPath").trim())
          .toAbsolutePath()
      );
      this.builder.setInitRDDevice(
        WXMDeviceID.of(
          Integer.parseInt(attributes.getValue("initRDDevice").trim()))
      );
      this.builder.setKernelPath(
        Paths.get(attributes.getValue("kernelPath").trim())
          .toAbsolutePath()
      );
      this.builder.setKernelDevice(
        WXMDeviceID.of(
          Integer.parseInt(attributes.getValue("kernelDevice").trim()))
      );
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXMGRUBKernelLinux onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
