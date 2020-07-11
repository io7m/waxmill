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
import java.util.List;

/**
 * An execution of an external command.
 */

@ImmutablesStyleType
@Value.Immutable
public abstract class WXMCommandExecutionType
{
  /**
   * @return The location of the executable
   */

  abstract Path executable();

  /**
   * @return The list of command-line arguments
   */

  abstract List<String> arguments();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  final void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.executable(),
      Path::isAbsolute,
      q -> "Executable path must be absolute"
    );
  }

  @Override
  public final String toString()
  {
    final var builder =
      new StringBuilder(this.executable().toString());
    if (!this.arguments().isEmpty()) {
      builder.append(' ');
      builder.append(String.join(" ", this.arguments()));
    }
    return builder.toString();
  }
}