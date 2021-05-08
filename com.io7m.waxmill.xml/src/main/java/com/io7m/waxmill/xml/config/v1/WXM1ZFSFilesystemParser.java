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

import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.nio.file.FileSystem;
import java.util.Objects;

/**
 * "ZFSFilesystem" parser.
 */

public final class WXM1ZFSFilesystemParser
  implements BTElementHandlerType<Object, WXM1ZFSFilesystem>
{
  private final WXM1ZFSFilesystem.Builder builder;
  private final FileSystem fileSystem;

  /**
   * "ZFSFilesystem" parser.
   *
   * @param inFileSystem The filesystem
   */

  public WXM1ZFSFilesystemParser(
    final FileSystem inFileSystem)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.builder =
      WXM1ZFSFilesystem.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setType(
        attributes.getValue("type"));

      final var fs =
        WXMZFSFilesystem.builder()
          .setMountPoint(
            this.fileSystem.getPath(attributes.getValue("mountPoint"))
              .toAbsolutePath())
          .setName(attributes.getValue("name").trim())
          .build();

      this.builder.setFilesystem(fs);
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public WXM1ZFSFilesystem onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
