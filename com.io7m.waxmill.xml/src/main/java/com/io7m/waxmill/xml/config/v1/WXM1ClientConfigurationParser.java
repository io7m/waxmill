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

import java.nio.file.FileSystem;
import java.util.Map;
import java.util.Objects;

import static com.io7m.waxmill.xml.config.v1.WXM1CNames.element;

public final class WXM1ClientConfigurationParser
  implements BTElementHandlerType<Object, WXMClientConfiguration>
{
  private final WXMClientConfiguration.Builder builder;
  private final FileSystem fileSystem;

  public WXM1ClientConfigurationParser(
    final FileSystem inFileSystem)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.builder =
      WXMClientConfiguration.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Paths"),
        c -> new WXM1PathsParser(this.fileSystem)
      ),
      Map.entry(
        element("ZFSFilesystems"),
        c -> new WXM1ZFSFilesystemsParser(this.fileSystem)
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
    } else if (result instanceof WXM1ZFSFilesystems) {
      this.onZFSFilesystemsReceived(context, (WXM1ZFSFilesystems) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  private void onZFSFilesystemsReceived(
    final BTElementParsingContextType context,
    final WXM1ZFSFilesystems result)
    throws SAXParseException
  {
    for (final var fs : result.filesystems()) {
      final var type = fs.type();
      switch (type) {
        case "VirtualMachineRuntimeFilesystem": {
          this.builder.setVirtualMachineRuntimeFilesystem(fs.filesystem());
          break;
        }
        default:
          throw context.parseException(
            new IllegalArgumentException(String.format(
              "Unrecognized filesystem type: %s",
              type))
          );
      }
    }
  }

  private void onPathsReceived(
    final BTElementParsingContextType context,
    final WXM1Paths result)
    throws SAXParseException
  {
    for (final var path : result.paths()) {
      final var type = path.type();
      switch (type) {
        case "VirtualMachineConfigurationDirectory": {
          this.builder.setVirtualMachineConfigurationDirectory(path.path());
          break;
        }
        case "GRUBBhyveExecutable": {
          this.builder.setGrubBhyveExecutable(path.path());
          break;
        }
        case "BhyveExecutable": {
          this.builder.setBhyveExecutable(path.path());
          break;
        }
        case "BhyveCtlExecutable": {
          this.builder.setBhyveCtlExecutable(path.path());
          break;
        }
        case "ZFSExecutable": {
          this.builder.setZfsExecutable(path.path());
          break;
        }
        case "IfconfigExecutable": {
          this.builder.setIfconfigExecutable(path.path());
          break;
        }
        case "CuExecutable": {
          this.builder.setCuExecutable(path.path());
          break;
        }
        default:
          throw context.parseException(
            new IllegalArgumentException(String.format(
              "Unrecognized path type: %s",
              type))
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
