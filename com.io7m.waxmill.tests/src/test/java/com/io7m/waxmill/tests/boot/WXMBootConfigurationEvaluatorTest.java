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

package com.io7m.waxmill.tests.boot;

import com.io7m.waxmill.boot.WXMBootConfigurationEvaluator;
import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceID;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMException;
import com.io7m.waxmill.machines.WXMExceptionNonexistent;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.tests.WXMTestDirectories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMBootConfigurationEvaluatorTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMBootConfigurationEvaluatorTest.class);

  private Path directory;
  private Path configs;
  private Path vms;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      WXMTestDirectories.createTempDirectory();
    this.configs =
      this.directory.resolve("configs");
    this.vms =
      this.directory.resolve("vms");
  }

  @Test
  public void noSuchBootConfiguration()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("anything")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("No such boot configuration"));
  }

  @Test
  public void openbsdZFSNotConfigured()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelOpenBSD.builder()
                .setBootDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(Optional.empty())
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("ZFS volume"));
  }

  @Test
  public void openbsdBootDeviceMissing()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelOpenBSD.builder()
                .setBootDevice(WXMDeviceID.of(12))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(1))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(2))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .addDevices(
          WXMDeviceLPC.builder()
            .setId(WXMDeviceID.of(3))
            .addBackends(
              WXMTTYBackendStdio.builder()
                .setDevice("com0")
                .build()
            ).build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("nonexistent or inappropriate boot device"));
  }

  @Test
  public void openbsdSimpleHD()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelOpenBSD.builder()
                .setBootDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      this.vms.resolve(machine.id().toString())
        .resolve("disk-0")
        .toString();

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grubConfiguration = evaluated.grubConfiguration();
    assertEquals(
      "kopenbsd -h com0 -r sd0a (hd0)/bsd",
      grubConfiguration.get(0)
    );
    assertEquals(
      "boot",
      grubConfiguration.get(1)
    );
    assertEquals(2, grubConfiguration.size());
  }

  @Test
  public void openbsdSimpleCD()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelOpenBSD.builder()
                .setBootDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      this.vms.resolve(machine.id().toString())
        .resolve("disk-0")
        .toString();

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grubConfiguration = evaluated.grubConfiguration();
    assertEquals(
      "kopenbsd -h com0 (cd0)/bsd",
      grubConfiguration.get(0)
    );
    assertEquals(
      "boot",
      grubConfiguration.get(1)
    );
    assertEquals(2, grubConfiguration.size());
  }

  @Test
  public void linuxZFSNotConfigured()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/vmlinux"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(WXMDeviceID.of(0))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(Optional.empty())
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("ZFS volume"));
  }

  @Test
  public void linuxZFSKernelDeviceMissing()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(WXMDeviceID.of(12))
                .setKernelPath(Paths.get("/vmlinux"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(WXMDeviceID.of(0))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("nonexistent or inappropriate boot device"));
  }

  @Test
  public void linuxZFSInitRDDeviceMissing()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/vmlinux"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(WXMDeviceID.of(12))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("nonexistent or inappropriate boot device"));
  }

  @Test
  public void linuxLinuxSimpleHD()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(WXMDeviceID.of(0))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      this.vms.resolve(machine.id().toString())
        .resolve("disk-0")
        .toString();

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grubConfiguration = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grubConfiguration.get(0)
    );
    assertEquals(
      "initrd (hd0)/initrd.img",
      grubConfiguration.get(1)
    );
    assertEquals(
      "boot",
      grubConfiguration.get(2)
    );
    assertEquals(3, grubConfiguration.size());
  }

  @Test
  public void linuxLinuxSimpleCD()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(WXMDeviceID.of(0))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(WXMDeviceID.of(0))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      this.vms.resolve(machine.id().toString())
        .resolve("disk-0")
        .toString();

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grubConfiguration = evaluated.grubConfiguration();
    assertEquals(
      "linux (cd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grubConfiguration.get(0)
    );
    assertEquals(
      "initrd (cd0)/initrd.img",
      grubConfiguration.get(1)
    );
    assertEquals(
      "boot",
      grubConfiguration.get(2)
    );
    assertEquals(3, grubConfiguration.size());
  }

  @Test
  public void notAStorageDevice()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(WXMDeviceID.of(1))
                .setKernelPath(Paths.get("/vmlinux"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(WXMDeviceID.of(1))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setId(WXMDeviceID.of(0))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .addDevices(
          WXMDeviceLPC.builder()
            .setId(WXMDeviceID.of(1))
            .addBackends(WXMTTYBackendStdio.builder()
                           .setDevice("com1")
                           .build())
            .build()
        )
        .build();

    final var clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setZfsVirtualMachineDirectory(this.vms)
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("nonexistent or inappropriate boot device"));
  }

  private static <T extends Throwable> T assertThrowsLogged(
    final Class<T> expectedType,
    final Executable executable)
  {
    final var ex = assertThrows(expectedType, executable);
    LOG.debug("", ex);
    return ex;
  }
}
