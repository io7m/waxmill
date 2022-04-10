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

package com.io7m.waxmill.machines;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

/**
 * A boot configuration name, such as {@code install}.
 */

@Value.Immutable
@ImmutablesStyleType
public interface WXMBootConfigurationNameType
  extends Comparable<WXMBootConfigurationNameType>
{
  @Override
  default int compareTo(
    final WXMBootConfigurationNameType other)
  {
    return this.value().compareTo(other.value());
  }

  /**
   * @return The machine name
   */

  @Value.Parameter
  String value();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    WXMBootConfigurationNames.checkValid(this.value());
  }
}
