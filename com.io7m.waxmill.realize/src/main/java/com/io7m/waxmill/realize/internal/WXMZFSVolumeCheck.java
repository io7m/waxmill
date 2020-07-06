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

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMStorageBackends;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.realize.WXMRealizationStepType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDryRun.DRY_RUN;

public final class WXMZFSVolumeCheck implements WXMRealizationStepType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMZFSVolumeCheck.class);

  private final Optional<WXMProcessDescription> processOpt;
  private final WXMClientConfiguration clientConfiguration;
  private final WXMDeviceSlot slot;
  private final WXMProcessesType processes;
  private final WXMRealizeMessages messages;
  private final WXMStorageBackendZFSVolume zfsVolume;
  private final Path volumePath;
  private List<WXMProcessDescription> processList;

  public WXMZFSVolumeCheck(
    final WXMClientConfiguration inClientConfiguration,
    final WXMRealizeMessages inMessages,
    final WXMProcessesType inProcesses,
    final UUID inMachineId,
    final WXMDeviceSlot inSlot,
    final WXMStorageBackendZFSVolume inZFSVolume)
  {
    this.clientConfiguration =
      Objects.requireNonNull(inClientConfiguration, "clientConfiguration");
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.processes =
      Objects.requireNonNull(inProcesses, "inProcesses");
    this.slot =
      Objects.requireNonNull(inSlot, "slot");
    this.zfsVolume =
      Objects.requireNonNull(inZFSVolume, "zfsVolume");

    final var machineId =
      Objects.requireNonNull(inMachineId, "machineId");
    this.volumePath =
      WXMStorageBackends.determineZFSVolumePath(
        this.clientConfiguration.virtualMachineRuntimeDirectory(),
        machineId,
        this.slot
      );

    this.processOpt = this.makeProcesses();
    this.processList = this.processOpt.map(List::of).orElseGet(List::of);
  }

  private Optional<WXMProcessDescription> makeProcesses()
  {
    return this.zfsVolume.expectedSize()
      .map(size -> WXMProcessDescription.builder()
        .setExecutable(this.clientConfiguration.zfsExecutable())
        .addArguments("create")
        .addArguments("-V")
        .addArguments(size.toString())
        .addArguments(this.volumePath.toString())
        .build()
      );
  }

  @Override
  public String description()
  {
    final var expectedSize = this.zfsVolume.expectedSize();
    return this.messages.format(
      "zfsVolumeCheck",
      this.volumePath,
      expectedSize.map(WXMZFSVolumeCheck::formatSize)
        .orElse("<unspecified>"),
      this.slot
    );
  }

  private static String formatSize(
    final BigInteger size)
  {
    return String.format(
      "%s (~%s)",
      size,
      FileUtils.byteCountToDisplaySize(size));
  }

  @Override
  public List<WXMProcessDescription> processes()
  {
    return this.processList;
  }

  @Override
  public void execute(
    final WXMDryRun dryRun)
    throws WXMException
  {
    if (dryRun == DRY_RUN) {
      return;
    }

    if (Files.exists(this.volumePath)) {
      final var sizeOpt = this.zfsVolume.expectedSize();
      if (sizeOpt.isPresent()) {
        final var expectedSize = sizeOpt.get();

        try {
          final var size = Files.size(this.volumePath);
          if (!Objects.equals(BigInteger.valueOf(size), expectedSize)) {
            LOG.warn("{}", this.zfsVolumeSizeMismatch(expectedSize, size));
          }
        } catch (final IOException e) {
          throw new WXMException(e);
        }
      }
      return;
    }

    LOG.debug("creating ZFS volume {}", this.volumePath);

    final var sizeOpt = this.zfsVolume.expectedSize();
    if (sizeOpt.isEmpty()) {
      throw new WXMException(this.zfsVolumeMissingNoSize());
    }

    if (this.processOpt.isPresent()) {
      final var processDescription = this.processOpt.get();
      try {
        this.processes.processStartAndWait(processDescription);
      } catch (final Exception e) {
        throw new WXMException(e);
      }
    }
  }

  private String zfsVolumeMissingNoSize()
  {
    return this.messages.format(
      "zfsVolumeMissingNoSize",
      this.volumePath,
      this.slot
    );
  }

  private String zfsVolumeSizeMismatch(
    final BigInteger expectedSize,
    final long size)
  {
    return this.messages.format(
      "zfsVolumeSizeMismatch",
      this.volumePath,
      expectedSize,
      Long.valueOf(size),
      this.slot
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMZFSVolumeCheck 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
