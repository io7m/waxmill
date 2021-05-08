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

package com.io7m.waxmill.realize.internal;

import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.realize.WXMRealizationStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDryRun.DRY_RUN;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 * A realization step that checks a file exists.
 */

public final class WXMFileCheck implements WXMRealizationStepType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMFileCheck.class);

  private final WXMRealizeMessages messages;
  private final WXMDeviceSlot slot;
  private final UUID machineId;
  private final Path file;

  /**
   * A realization step that checks a file exists.
   *
   * @param inMessages  The string resources
   * @param inSlot      The device slot
   * @param inMachineId The machine ID
   * @param inFile      The file
   */

  public WXMFileCheck(
    final WXMRealizeMessages inMessages,
    final WXMDeviceSlot inSlot,
    final UUID inMachineId,
    final Path inFile)
  {
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.slot =
      Objects.requireNonNull(inSlot, "slot");
    this.machineId =
      Objects.requireNonNull(inMachineId, "inMachineId");
    this.file =
      Objects.requireNonNull(inFile, "file");
  }

  @Override
  public String description()
  {
    return this.messages.format("fileCheck", this.file, this.slot);
  }

  @Override
  public List<WXMProcessDescription> processes()
  {
    return List.of();
  }

  @Override
  public void execute(
    final WXMDryRun dryRun)
    throws WXMException
  {
    if (dryRun == DRY_RUN) {
      return;
    }

    LOG.info("checking {} is regular file", this.file);

    if (!Files.isRegularFile(this.file, NOFOLLOW_LINKS)) {
      throw new WXMException(this.errorMissingFile(this.file));
    }
  }

  private String errorMissingFile(
    final Path path)
  {
    return this.messages.format(
      "errorRequiredPathsMissing",
      this.machineId,
      path
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMFileCheck 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
