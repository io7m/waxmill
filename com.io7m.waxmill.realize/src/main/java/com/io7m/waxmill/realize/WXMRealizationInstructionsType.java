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

package com.io7m.waxmill.realize;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptions;
import com.io7m.waxmill.machines.WXMDryRun;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;

/**
 * The instructions that comprise a realization.
 */

@Value.Immutable
@ImmutablesStyleType
public interface WXMRealizationInstructionsType
{
  /**
   * @return The realization steps
   */

  List<WXMRealizationStepType> steps();

  /**
   * Execute the realization.
   *
   * @param dryRun If this is a dry run
   *
   * @throws WXMException On errors
   */

  default void execute(
    final WXMDryRun dryRun)
    throws WXMException
  {
    Objects.requireNonNull(dryRun, "dryRun");

    final var exceptions = new WXMExceptions();
    for (final var step : this.steps()) {
      try {
        step.execute(dryRun);
      } catch (final Exception e) {
        exceptions.add(e);
      }
    }
    exceptions.throwIfRequired();
  }
}
