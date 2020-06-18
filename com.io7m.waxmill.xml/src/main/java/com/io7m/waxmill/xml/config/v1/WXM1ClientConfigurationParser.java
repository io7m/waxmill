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

package com.io7m.waxmill.xml.config.v1;

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.Locale;
import java.util.Map;

import static com.io7m.waxmill.xml.config.v1.WXM1CNames.element;

public final class WXM1ClientConfigurationParser
  implements BTElementHandlerType<Object, WXMClientConfiguration>
{
  private final WXMClientConfiguration.Builder builder;

  public WXM1ClientConfigurationParser()
  {
    this.builder = WXMClientConfiguration.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Paths"),
        c -> new WXM1PathsParser()
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
    throws SAXException
  {
    if (result instanceof WXM1Paths) {
      this.onPathsReceived(context, (WXM1Paths) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  private void onPathsReceived(
    final BTElementParsingContextType context,
    final WXM1Paths result)
    throws SAXParseException
  {
    for (final var path : result.paths()) {
      final var type = path.type();
      switch (type.toUpperCase(Locale.ROOT)) {
        case "VIRTUALMACHINECONFIGURATIONSDIRECTORY": {
          this.builder.setVirtualMachineConfigurationDirectory(path.path());
          break;
        }
        case "ZFSVIRTUALMACHINESDIRECTORY": {
          this.builder.setZfsVirtualMachineDirectory(path.path());
          break;
        }
        default:
          throw context.parseException(
            new IllegalArgumentException("Unrecognized path type: " + type)
          );
      }
    }
  }

  @Override
  public WXMClientConfiguration onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
