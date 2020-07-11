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

package com.io7m.waxmill.client.vanilla.internal;

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.boot.WXMBootConfigurationEvaluator;
import com.io7m.waxmill.boot.WXMBootConfigurationExecutor;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.client.api.WXMClientType;
import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseType;
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDryRun;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackends;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.realize.WXMRealizations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_LPC;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMLPCTTYNames.WXM_COM1;
import static com.io7m.waxmill.machines.WXMDryRun.DRY_RUN;
import static com.io7m.waxmill.machines.WXMDryRun.EXECUTE;
import static com.io7m.waxmill.machines.WXMTTYBackends.NMDMSide.NMDM_HOST;

public final class WXMClient implements WXMClientType
{
  private static final Logger LOG = LoggerFactory.getLogger(WXMClient.class);

  private final WXMClientConfiguration configuration;
  private final WXMVirtualMachineDatabaseType database;
  private final WXMProcessesType processes;

  public WXMClient(
    final WXMClientConfiguration inConfiguration,
    final WXMVirtualMachineDatabaseType inDatabase,
    final WXMProcessesType inProcesses)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");
    this.processes =
      Objects.requireNonNull(inProcesses, "inProcesses");
  }

  @Override
  public void close()
    throws WXMException
  {
    this.database.close();
  }

  @Override
  public WXMVirtualMachineSet vmList()
    throws WXMException
  {
    return this.database.vmList();
  }

  @Override
  public WXMVirtualMachine vmFind(
    final UUID id)
    throws WXMException
  {
    Objects.requireNonNull(id, "id");

    return this.database.vmGet(id)
      .orElseThrow(() -> new WXMException(
        String.format("No such virtual machine: %s", id))
      );
  }

  @Override
  public Optional<WXMVirtualMachine> vmFindOptional(
    final UUID id)
    throws WXMException
  {
    return this.database.vmGet(
      Objects.requireNonNull(id, "id")
    );
  }

  @Override
  public void vmDefine(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    Objects.requireNonNull(machine, "machine");
    this.database.vmDefine(machine);
  }

  @Override
  public void vmRun(
    final WXMVirtualMachine machine,
    final WXMBootConfigurationName bootConfigurationName,
    final WXMDryRun dryRun)
    throws WXMException, InterruptedException
  {
    Objects.requireNonNull(machine, "machine");
    Objects.requireNonNull(bootConfigurationName, "bootConfigurationName");

    final var evaluated =
      new WXMBootConfigurationEvaluator(
        this.configuration,
        machine,
        bootConfigurationName
      ).evaluate();

    final var executor =
      WXMBootConfigurationExecutor.create(
        this.processes,
        this.configuration,
        machine,
        evaluated
      );

    switch (dryRun) {
      case DRY_RUN:
        executor.execute(DRY_RUN);
        break;
      case EXECUTE:
        executor.execute(EXECUTE);
        break;
    }
  }


  @Override
  public void vmKill(
    final WXMVirtualMachine machine,
    final WXMDryRun dryRun)
    throws WXMException
  {
    final var processDescription =
      WXMProcessDescription.builder()
        .setExecutable(this.configuration.bhyveCtlExecutable())
        .addArguments(String.format("--vm=%s", machine.id()))
        .addArguments("--destroy")
        .build();

    switch (dryRun) {
      case DRY_RUN:
        System.out.printf(
          "%s %s%n",
          processDescription.executable(),
          String.join(" ", processDescription.arguments())
        );
        break;
      case EXECUTE:
        try {
          this.processes.processReplaceCurrent(processDescription);
        } catch (final IOException e) {
          throw new WXMException(e);
        }
        break;
    }
  }

  @Override
  public void vmDelete(final UUID id)
    throws WXMException
  {
    Objects.requireNonNull(id, "id");
    this.database.vmDelete(id);
  }

  @Override
  public Optional<WXMDeviceType> vmConsoleGet(
    final WXMVirtualMachine machine)
  {
    Objects.requireNonNull(machine, "machine");

    final var consoleDevices =
      machine.devices()
        .stream()
        .filter(dev -> dev.kind() == WXM_LPC)
        .map(WXMDeviceLPC.class::cast)
        .filter(lpc -> lpc.backendMap().containsKey(WXM_COM1.deviceName()))
        .collect(Collectors.toList());

    final var deviceCount = consoleDevices.size();
    LOG.debug(
      "found {} console devices in machine {}",
      Integer.valueOf(deviceCount),
      machine.id()
    );

    if (deviceCount == 1) {
      return Optional.of(consoleDevices.get(0));
    }
    return Optional.empty();
  }

  @Override
  public Optional<WXMProcessDescription> vmConsole(
    final WXMVirtualMachine machine)
  {
    Objects.requireNonNull(machine, "machine");

    return this.vmConsoleGet(machine)
      .map(WXMDeviceLPC.class::cast)
      .flatMap(device -> this.handleLPCConsole(machine, device));
  }

  @Override
  public void vmRealize(
    final WXMVirtualMachine machine,
    final WXMDryRun dryRun)
    throws WXMException
  {
    Objects.requireNonNull(machine, "machine");
    Objects.requireNonNull(dryRun, "dryRun");

    final var realization =
      WXMRealizations.create(this.processes, this.configuration, machine);
    final var instructions =
      realization.evaluate();

    instructions.execute(dryRun);
  }

  private Optional<WXMProcessDescription> handleLPCConsole(
    final WXMVirtualMachine machine,
    final WXMDeviceLPC device)
  {
    final var com1 = WXM_COM1.deviceName();
    final var tty = device.backendMap().get(com1);
    Objects.requireNonNull(tty, "tty");

    switch (tty.kind()) {
      case WXM_FILE: {
        final var ttyFile = (WXMTTYBackendFile) tty;
        return Optional.of(
          WXMProcessDescription.builder()
            .setExecutable(this.configuration.cuExecutable())
            .addArguments("-l")
            .addArguments(ttyFile.path().toString())
            .build()
        );
      }

      case WXM_NMDM:
        final var fileSystem =
          this.configuration.virtualMachineRuntimeDirectory()
            .getFileSystem();

        final var path =
          WXMTTYBackends.nmdmPath(
            fileSystem,
            machine.id(),
            NMDM_HOST
          );

        return Optional.of(
          WXMProcessDescription.builder()
            .setExecutable(this.configuration.cuExecutable())
            .addArguments("-l")
            .addArguments(path.toString())
            .build()
        );

      case WXM_STDIO:
        LOG.debug("cannot use a stdio-based console");
        return Optional.empty();
    }

    throw new UnreachableCodeException();
  }

  @Override
  public void vmUpdate(
    final WXMVirtualMachine machine)
    throws WXMException
  {
    Objects.requireNonNull(machine, "machine");
    this.database.vmUpdate(machine);
  }

  @Override
  public WXMClientConfiguration configuration()
  {
    return this.configuration;
  }

  @Override
  public void vmDefineAll(
    final WXMVirtualMachineSet machines)
    throws WXMException
  {
    Objects.requireNonNull(machines, "machines");
    this.database.vmDefineAll(machines);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMClient 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
