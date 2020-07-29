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

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import org.immutables.value.Value;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A ZFS filesystem.
 */

@ImmutablesStyleType
@Value.Immutable
public interface WXMZFSFilesystemType
{
  /**
   * The name of the filesystem. This is the name that would be, for example,
   * passed to {@code zfs create -V name/of/fs}.
   *
   * @return The name of the filesystem
   */

  String name();

  /**
   * The path at which the filesystem is mounted.
   *
   * @return The filesystem mount point
   */

  Path mountPoint();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var nameRaw = this.name();
    final var nameTrim = nameRaw.trim();

    Preconditions.checkPrecondition(
      nameRaw,
      Objects.equals(nameRaw, nameTrim),
      n -> "Filesystem names cannot have leading or trailing whitespace"
    );

    Preconditions.checkPrecondition(
      nameTrim,
      !nameTrim.startsWith("/"),
      n -> "Filesystem names cannot have leading slashes"
    );

    final var mount = this.mountPoint();
    Preconditions.checkPrecondition(
      mount,
      mount.isAbsolute(),
      p -> "The filesystem mount point must be absolute"
    );
  }
}
