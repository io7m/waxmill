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
import com.io7m.waxmill.machines.WXMBootConfigurationType;
import com.io7m.waxmill.machines.WXMCPUTopology;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMMemory;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1VirtualMachineParser
  implements BTElementHandlerType<Object, WXMVirtualMachine>
{
  private final WXMVirtualMachine.Builder builder;
  private final FileSystem fileSystem;
  private final URI sourceURI;

  public WXM1VirtualMachineParser(
    final FileSystem inFileSystem,
    final URI inSourceURI)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.sourceURI =
      Objects.requireNonNull(inSourceURI, "sourceURI");
    this.builder =
      WXMVirtualMachine.builder();
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
        element("CPUTopology"),
        c -> new WXM1CPUTopologyParser()
      ),
      Map.entry(
        element("Memory"),
        c -> new WXM1MemoryParser()
      ),
      Map.entry(
        element("Devices"),
        c -> new WXM1DevicesParser()
      ),
      Map.entry(
        element("BootConfigurations"),
        c -> new WXM1BootConfigurationsParser(this.fileSystem)
      ),
      Map.entry(
        element("Flags"),
        c -> new WXM1FlagsParser()
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
    } else if (result instanceof WXMMemory) {
      this.builder.setMemory((WXMMemory) result);
    } else if (result instanceof WXMCPUTopology) {
      this.builder.setCpuTopology((WXMCPUTopology) result);
    } else if (result instanceof List) {
      final var items = (List<?>) result;
      if (!items.isEmpty()) {
        if (items.get(0) instanceof WXMDeviceType) {
          this.builder.setDevices(
            (Iterable<? extends WXMDeviceType>) items);
          return;
        }
        if (items.get(0) instanceof WXMBootConfigurationType) {
          this.builder.setBootConfigurations(
            (Iterable<? extends WXMBootConfigurationType>) items);
          return;
        }
        throw new UnreachableCodeException();
      }
    } else if (result instanceof WXMFlags) {
      this.builder.setFlags((WXMFlags) result);
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
      this.builder.setId(
        UUID.fromString(attributes.getValue("id")));
      this.builder.setName(
        WXMMachineName.of(attributes.getValue("name")));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXMVirtualMachine onElementFinished(
    final BTElementParsingContextType context)
  {
    this.builder.setConfigurationFile(this.sourceURI);
    return this.builder.build();
  }
}
