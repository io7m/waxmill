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
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMBootDiskAttachment;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.nio.file.FileSystem;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1BootConfigurationUEFIParser
  implements BTElementHandlerType<Object, WXMBootConfigurationUEFI>
{
  private final WXMBootConfigurationUEFI.Builder builder;
  private final FileSystem fileSystem;

  public WXM1BootConfigurationUEFIParser(
    final FileSystem inFileSystem)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.builder =
      WXMBootConfigurationUEFI.builder();
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
        element("BootDiskAttachments"),
        c -> new WXM1BootDiskAttachmentsParser()
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof List) {
      this.builder.setDiskAttachments(
        (Iterable<? extends WXMBootDiskAttachment>) result);
    } else if (result instanceof WXM1Comment) {
      this.builder.setComment(((WXM1Comment) result).text());
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
      this.builder.setName(
        WXMBootConfigurationName.of(
          attributes.getValue("name").trim()
        )
      );
      this.builder.setFirmware(
        this.fileSystem.getPath(
          attributes.getValue("firmware").trim()
        )
      );
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXMBootConfigurationUEFI onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
