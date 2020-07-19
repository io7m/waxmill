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

package com.io7m.waxmill.boot;

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.boot.internal.WXMBootMessages;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.locks.WXMFileLock;
import com.io7m.waxmill.machines.WXMCommandExecution;
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.machines.WXMEvaluatedBootCommands;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.io7m.waxmill.boot.WXMBootConfigurationExecutor.WithComment.WITHOUT_COMMENT;
import static com.io7m.waxmill.boot.WXMBootConfigurationExecutor.WithComment.WITH_COMMENT;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMEvaluatedBootConfigurationGRUBBhyveType;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMEvaluatedBootConfigurationType;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMEvaluatedBootConfigurationUEFIType;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class WXMBootConfigurationExecutor
  implements WXMBootConfigurationExecutorType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMBootConfigurationExecutor.class);

  private final WXMProcessesType processes;
  private final WXMClientConfiguration clientConfiguration;
  private final WXMVirtualMachine machine;
  private final WXMEvaluatedBootConfigurationType bootConfiguration;
  private final WXMBootMessages messages;

  private WXMBootConfigurationExecutor(
    final WXMBootMessages inMessages,
    final WXMProcessesType inProcesses,
    final WXMClientConfiguration inClientConfiguration,
    final WXMVirtualMachine inMachine,
    final WXMEvaluatedBootConfigurationType inBootConfiguration)
  {
    this.messages =
      Objects.requireNonNull(inMessages, "inMessages");
    this.processes =
      Objects.requireNonNull(inProcesses, "processes");
    this.clientConfiguration =
      Objects.requireNonNull(inClientConfiguration, "inClientConfiguration");
    this.machine =
      Objects.requireNonNull(inMachine, "inMachine");
    this.bootConfiguration =
      Objects.requireNonNull(inBootConfiguration, "bootConfiguration");
  }

  public static WXMBootConfigurationExecutorType create(
    final WXMProcessesType inProcesses,
    final WXMClientConfiguration inClientConfiguration,
    final WXMVirtualMachine inMachine,
    final WXMEvaluatedBootConfigurationType inBootConfiguration)
  {
    return new WXMBootConfigurationExecutor(
      WXMBootMessages.create(),
      inProcesses,
      inClientConfiguration,
      inMachine,
      inBootConfiguration
    );
  }

  private static void writeGrubConfig(
    final WXMDryRun execute,
    final WXMEvaluatedBootConfigurationGRUBBhyveType grubBhyveConfiguration)
    throws IOException
  {
    final var file = grubBhyveConfiguration.grubConfigurationFile();
    switch (execute) {
      case DRY_RUN: {
        LOG.debug("write grub.cfg: {}", file);
        break;
      }
      case EXECUTE: {
        writeFileLinesAtomically(
          file, WITH_COMMENT, grubBhyveConfiguration.grubConfiguration()
        );
        break;
      }
    }
  }

  private static void writeGrubDeviceMap(
    final WXMDryRun execute,
    final WXMEvaluatedBootConfigurationGRUBBhyveType grubBhyveConfiguration)
    throws IOException
  {
    final var file = grubBhyveConfiguration.deviceMapFile();
    switch (execute) {
      case DRY_RUN: {
        LOG.debug("write device map: {}", file);
        break;
      }
      case EXECUTE: {
        writeFileLinesAtomically(
          file, WITHOUT_COMMENT, grubBhyveConfiguration.deviceMap()
        );
        break;
      }
    }
  }

  private static void writeFileLinesAtomically(
    final Path file,
    final WithComment withComment,
    final List<String> lines)
    throws IOException
  {
    final var fileTmp = file.getFileSystem().getPath(file + ".tmp");
    try (var stream =
           Files.newBufferedWriter(
             fileTmp, UTF_8, CREATE, TRUNCATE_EXISTING, WRITE)) {
      switch (withComment) {
        case WITH_COMMENT:
          stream.write("# Automatically generated. Do not edit.");
          stream.newLine();
          break;
        case WITHOUT_COMMENT:
          break;
      }
      for (final var line : lines) {
        stream.write(line);
        stream.newLine();
      }
      stream.flush();
      Files.move(fileTmp, file, REPLACE_EXISTING, ATOMIC_MOVE);
    }
  }

  @Override
  public void execute(
    final WXMDryRun execute)
    throws WXMException, InterruptedException
  {
    Objects.requireNonNull(execute, "execute");

    try {
      this.checkRequiredPaths();

      switch (this.bootConfiguration.kind()) {
        case GRUB_BHYVE:
          this.executeGRUBBhyve(
            execute,
            (WXMEvaluatedBootConfigurationGRUBBhyveType) this.bootConfiguration
          );
          return;
        case UEFI:
          this.executeUEFI(
            execute,
            (WXMEvaluatedBootConfigurationUEFIType) this.bootConfiguration
          );
          return;
      }
    } catch (final IOException e) {
      throw new WXMException(e);
    }

    throw new UnreachableCodeException();
  }

  private void checkRequiredPaths()
    throws IOException
  {
    final var missingPaths = new ArrayList<Path>();
    final var missingNMDMs = new ArrayList<Path>();

    for (final var path : this.bootConfiguration.requiredPaths()) {
      if (!Files.exists(path)) {
        if (this.bootConfiguration.requiredNMDMs().contains(path)) {
          missingNMDMs.add(path);
        }
        missingPaths.add(path);
      }
    }
    if (!missingPaths.isEmpty()) {
      if (!missingNMDMs.isEmpty()) {
        throw new IOException(this.errorRequiredPathsMissingWithNMDMs(missingPaths, missingNMDMs));
      }
      throw new IOException(this.errorRequiredPathsMissing(missingPaths));
    }
  }

  private void executeUEFI(
    final WXMDryRun execute,
    final WXMEvaluatedBootConfigurationUEFIType uefiConfiguration)
    throws IOException, InterruptedException
  {
    this.executeCommands(execute, uefiConfiguration.commands());
  }

  private void executeCommands(
    final WXMDryRun execute,
    final WXMEvaluatedBootCommands commands)
    throws IOException, InterruptedException
  {
    for (final var command : commands.configurationCommands()) {
      switch (execute) {
        case DRY_RUN: {
          System.out.println(command.toString());
          break;
        }
        case EXECUTE: {
          this.executeAndWait(command);
          break;
        }
      }
    }

    final var lastExecutionOpt = commands.lastExecution();
    if (lastExecutionOpt.isPresent()) {
      final var lastExecution = lastExecutionOpt.get();
      switch (execute) {
        case DRY_RUN:
          System.out.printf("exec %s%n", lastExecution.toString());
          return;
        case EXECUTE:
          this.executeAndReplace(lastExecution);
          return;
      }
    }
  }

  private void executeGRUBBhyve(
    final WXMDryRun execute,
    final WXMEvaluatedBootConfigurationGRUBBhyveType grubBhyveConfiguration)
    throws IOException, WXMException, InterruptedException
  {
    final var lockFile =
      this.clientConfiguration.virtualMachineRuntimeDirectory()
        .resolve(this.machine.id().toString())
        .resolve("lock");

    try (var ignored = WXMFileLock.acquire(lockFile)) {
      writeGrubDeviceMap(execute, grubBhyveConfiguration);
      writeGrubConfig(execute, grubBhyveConfiguration);
    }

    this.executeCommands(execute, grubBhyveConfiguration.commands());
  }

  private void executeAndReplace(
    final WXMCommandExecution command)
    throws IOException
  {
    LOG.info("execute: {}", command);

    final var processDescription =
      WXMProcessDescription.builder()
        .setExecutable(command.executable())
        .addAllArguments(command.arguments())
        .build();

    this.processes.processReplaceCurrent(processDescription);
  }

  private void executeAndWait(
    final WXMCommandExecution command)
    throws IOException, InterruptedException
  {
    LOG.info("execute: {}", command);

    final var processDescription =
      WXMProcessDescription.builder()
        .setExecutable(command.executable())
        .addAllArguments(command.arguments())
        .build();

    try {
      this.processes.processStartAndWait(processDescription);
    } catch (final Exception e) {
      if (!command.ignoreFailure()) {
        throw e;
      }
    }
  }

  private String errorRequiredPathsMissing(
    final Collection<Path> missingPaths)
  {
    return this.messages.format(
      "bootRequiredPathsMissing",
      this.machine.id(),
      missingPaths
    );
  }

  private String errorRequiredPathsMissingWithNMDMs(
    final Collection<Path> missingPaths,
    final Collection<Path> missingNMDMs)
  {
    return this.messages.format(
      "bootRequiredPathsMissingNMDM",
      this.machine.id(),
      missingPaths,
      missingNMDMs
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMBootConfigurationExecutor 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }

  enum WithComment
  {
    WITH_COMMENT,
    WITHOUT_COMMENT
  }
}
