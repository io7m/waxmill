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
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.realize.WXMRealizationStepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDryRun.DRY_RUN;
import static java.util.Locale.ROOT;

public final class WXMRuntimeDirectoryCreate implements WXMRealizationStepType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMRuntimeDirectoryCreate.class);

  private final WXMRealizeMessages messages;
  private final UUID machineId;
  private final WXMClientConfiguration clientConfiguration;
  private final WXMProcessesType processes;
  private List<WXMProcessDescription> processesList;

  public WXMRuntimeDirectoryCreate(
    final WXMClientConfiguration inClientConfiguration,
    final WXMRealizeMessages inMessages,
    final WXMProcessesType inProcesses,
    final UUID inMachineId)
  {
    this.clientConfiguration =
      Objects.requireNonNull(inClientConfiguration, "clientConfiguration");
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.processes =
      Objects.requireNonNull(inProcesses, "inProcesses");
    this.machineId =
      Objects.requireNonNull(inMachineId, "inMachineId");

    this.processesList = List.of();
  }

  @Override
  public String description()
  {
    return this.messages.format(
      "runtimeDirectoryCreate",
      this.clientConfiguration.virtualMachineRuntimeDirectory()
        .resolve(this.machineId.toString())
    );
  }

  @Override
  public List<WXMProcessDescription> processes()
  {
    return List.copyOf(this.processesList);
  }

  @Override
  public void execute(
    final WXMDryRun dryRun)
    throws WXMException
  {
    if (dryRun == DRY_RUN) {
      return;
    }

    final var baseDirectory =
      this.clientConfiguration.virtualMachineRuntimeDirectory();
    final var path =
      baseDirectory.resolve(this.machineId.toString());

    LOG.info("checking if {} is a directory", path);

    if (Files.isDirectory(path)) {
      LOG.info("{} is a directory", path);
      return;
    }

    if (Files.exists(path)) {
      throw new WXMException(this.notADirectory(path));
    }

    try {
      final var store = Files.getFileStore(baseDirectory);
      if ("ZFS".equals(store.type().toUpperCase(ROOT))) {
        final var createPath =
          String.format("%s/%s", store.name(), this.machineId);

        LOG.info("creating ZFS filesystem {}", createPath);
        final var process =
          WXMProcessDescription.builder()
            .setExecutable(this.clientConfiguration.zfsExecutable())
            .addArguments("create")
            .addArguments(createPath)
            .build();

        this.processesList = List.of(process);
        this.processes.processStartAndWait(process);
      } else {
        LOG.info("creating directory {}", path);
        Files.createDirectories(path);
      }
    } catch (final IOException | InterruptedException e) {
      throw new WXMException(e);
    }
  }

  private String notADirectory(
    final Path path)
  {
    return this.messages.format(
      "runtimeDirectoryNotADirectory",
      path
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMRuntimeDirectoryCreate 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
