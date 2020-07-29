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

package com.io7m.waxmill.machines;

import java.util.Objects;

/**
 * Functions over ZFS filesystems.
 */

public final class WXMZFSFilesystems
{
  private WXMZFSFilesystems()
  {

  }

  /**
   * Resolve the given name against the given filesystem.
   *
   * @param filesystem The filesystem
   * @param name       The file name
   *
   * @return A new filesystem
   *
   * @see java.nio.file.Path#resolve(String)
   */

  public static WXMZFSFilesystem resolve(
    final WXMZFSFilesystem filesystem,
    final String name)
  {
    Objects.requireNonNull(filesystem, "filesystem");
    Objects.requireNonNull(name, "name");

    final var mountPoint =
      filesystem.mountPoint()
        .resolve(name);

    return WXMZFSFilesystem.builder()
      .setMountPoint(mountPoint)
      .setName(String.format("%s/%s", filesystem.name(), name))
      .build();
  }
}
