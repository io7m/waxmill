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

package com.io7m.waxmill.realize;

import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.process.api.WXMProcessDescription;

import java.io.IOException;
import java.util.List;

/**
 * A single step within a realization.
 */

public interface WXMRealizationStepType
{
  /**
   * @return The description of the step
   */

  String description();

  /**
   * @return The list of processes that will be executed
   */

  List<WXMProcessDescription> processes();

  /**
   * Execute the step.
   *
   * @param dryRun A specification of whether this is a dry run or not
   *
   * @throws WXMException On errors
   */

  void execute(
    WXMDryRun dryRun)
    throws WXMException, IOException, InterruptedException;
}
