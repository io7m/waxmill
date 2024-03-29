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

package com.io7m.waxmill.cmdline.internal;

import com.beust.jcommander.ParameterException;

import java.util.Optional;

/**
 * Functions over named parameters.
 */

public final class WXMNamedParameters
{
  private WXMNamedParameters()
  {

  }

  /**
   * An optional parameter.
   *
   * @param name      The name
   * @param parameter The value
   * @param <T>       The type of values
   *
   * @return A parameter
   */

  public static <T> WXMNamedParameter<Optional<T>> optional(
    final String name,
    final T parameter)
  {
    return WXMNamedParameter.<Optional<T>>builder()
      .setName(name)
      .setValue(Optional.ofNullable(parameter))
      .build();
  }

  /**
   * Check a required parameter is present.
   *
   * @param messages  The messages
   * @param owner     The owner
   * @param parameter The parameter
   * @param <T>       The type of parameter values
   *
   * @return The parameter value
   */

  public static <T> T checkRequired(
    final WXMMessages messages,
    final WXMNamedParameter<?> owner,
    final WXMNamedParameter<Optional<T>> parameter)
  {
    final Optional<T> value = parameter.value();
    if (value.isEmpty()) {
      throw new ParameterException(
        messages.format(
          "errorDependentParameterMissing",
          owner.name(),
          owner.value(),
          parameter.name()
        )
      );
    }
    return value.get();
  }
}
