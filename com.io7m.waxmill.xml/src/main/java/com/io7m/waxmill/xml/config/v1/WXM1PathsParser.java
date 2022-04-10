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

package com.io7m.waxmill.xml.config.v1;

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;

import java.nio.file.FileSystem;
import java.util.Map;
import java.util.Objects;

import static com.io7m.waxmill.xml.config.v1.WXM1CNames.element;

/**
 * "Paths" parser.
 */

public final class WXM1PathsParser
  implements BTElementHandlerType<WXM1Path, WXM1Paths>
{
  private final WXM1Paths.Builder builder;
  private final FileSystem fileSystem;

  /**
   * "Paths" parser.
   *
   * @param inFileSystem The filesystem
   */

  public WXM1PathsParser(
    final FileSystem inFileSystem)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.builder =
      WXM1Paths.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends WXM1Path>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.of(
      element("Path"),
      c -> new WXM1PathParser(this.fileSystem)
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final WXM1Path result)
  {
    this.builder.addPaths(result);
  }

  @Override
  public WXM1Paths onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
