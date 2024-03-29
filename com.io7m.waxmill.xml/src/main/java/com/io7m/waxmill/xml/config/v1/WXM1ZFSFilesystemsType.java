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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A set of ZFS filesystems.
 */

@Value.Immutable
@ImmutablesStyleType
public interface WXM1ZFSFilesystemsType
{
  /**
   * @return The list of filesystems
   */

  @Value.Parameter
  List<WXM1ZFSFilesystem> filesystems();

  /**
   * @return The filesystems by type
   */

  @Value.Derived
  default Map<String, WXM1ZFSFilesystem> pathMap()
  {
    return this.filesystems()
      .stream()
      .collect(Collectors.toMap(WXM1ZFSFilesystem::type, Function.identity()));
  }
}
