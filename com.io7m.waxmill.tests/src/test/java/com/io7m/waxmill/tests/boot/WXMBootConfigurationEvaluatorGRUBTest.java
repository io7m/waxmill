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
import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptionNonexistent;
import com.io7m.waxmill.exceptions.WXMExceptionUnsatisfiedRequirement;
import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMBootDiskAttachment;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceE1000;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDevicePassthru;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMDeviceVirtioNetwork;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMInterfaceGroupName;
import com.io7m.waxmill.machines.WXMMACAddress;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMSectorSizes;
import com.io7m.waxmill.machines.WXMShortIDs;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMTAPDeviceName;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVMNetDeviceName;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.tests.WXMTestDirectories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_AMD;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_UNSPECIFIED;
import static com.io7m.waxmill.machines.WXMOpenOption.NO_CACHE;
import static com.io7m.waxmill.machines.WXMOpenOption.READ_ONLY;
import static com.io7m.waxmill.machines.WXMOpenOption.SYNCHRONOUS;
import static com.io7m.waxmill.tests.WXMDeviceIDTest.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WXMBootConfigurationEvaluatorGRUBTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMBootConfigurationEvaluatorGRUBTest.class);

  private Path directory;
  private Path configs;
  private Path vms;
  private WXMClientConfiguration clientConfiguration;

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

    this.clientConfiguration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.configs)
        .setVirtualMachineRuntimeFilesystem(
          WXMZFSFilesystem.builder()
            .setMountPoint(this.vms)
            .setName("storage/vm")
            .build())
        .build();
  }

  @Test
  public void noSuchBootConfiguration()
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("anything")
      );

    final var ex =
      assertThrowsLogged(WXMExceptionNonexistent.class, evaluator::evaluate);
    assertTrue(ex.getMessage().contains("No such boot configuration"));
  }

  @Test
  public void openbsdSimpleAHCIHD()
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
                .setBootDevice(convert("0:0:0"))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .setOptions(Set.of(NO_CACHE, READ_ONLY, SYNCHRONOUS))
                .setSectorSizes(
                  WXMSectorSizes.builder()
                    .setLogical(BigInteger.valueOf(2048L))
                    .setPhysical(BigInteger.valueOf(4096L))
                    .build())
                .build()
            ).build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals("kopenbsd -h com0 -r sd0a (hd0)/bsd", grub.get(0));
    assertEquals("boot", grub.get(1));
    assertEquals(2, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,ahci-hd,/tmp/file,nocache,direct,ro,sectorsize=2048/4096 %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
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
                .setBootDevice(convert("0:0:0"))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var expectedZFSDisk =
      String.format("/dev/zvol/storage/vm/%s/disk-0_0_0", machine.id());

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals("kopenbsd -h com0 -r sd0a (hd0)/bsd", grub.get(0));
    assertEquals("boot", grub.get(1));
    assertEquals(2, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,virtio-blk,/dev/zvol/storage/vm/%s/disk-0_0_0 %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
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
            .addDiskAttachments(
              WXMBootDiskAttachment.builder()
                .setDevice(convert("0:1:0"))
                .setBackend(WXMStorageBackendZFSVolume.builder().build())
                .build()
            )
            .setKernelInstructions(
              WXMGRUBKernelOpenBSD.builder()
                .setBootDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_AMD)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .build()
        )
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      String.format("/dev/zvol/storage/vm/%s/disk-0_1_0", machine.id());

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals("kopenbsd -h com0 (cd0)/bsd", grub.get(0));
    assertEquals("boot", grub.get(1));
    assertEquals(2, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,amd_hostbridge -s 0:1:0,ahci-cd,/dev/zvol/storage/vm/%s/disk-0_1_0 %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void openbsdSimpleCDUnused()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .addDiskAttachments(
              WXMBootDiskAttachment.builder()
                .setDevice(convert("0:1:0"))
                .setBackend(WXMStorageBackendZFSVolume.builder().build())
                .build()
            )
            .setKernelInstructions(
              WXMGRUBKernelOpenBSD.builder()
                .setBootDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_AMD)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setDeviceSlot(convert("0:2:0"))
            .build()
        )
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      String.format("/dev/zvol/storage/vm/%s/disk-0_1_0", machine.id());

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals("kopenbsd -h com0 (cd0)/bsd", grub.get(0));
    assertEquals("boot", grub.get(1));
    assertEquals(2, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,amd_hostbridge -s 0:1:0,ahci-cd,/dev/zvol/storage/vm/%s/disk-0_1_0 %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void openbsdSimpleCDMissingAttachment()
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
                .setBootDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/bsd"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_AMD)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .build()
        )
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    assertThrowsLogged(
      WXMExceptionUnsatisfiedRequirement.class,
      evaluator::evaluate
    );
  }

  @Test
  public void linuxSimpleHD()
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
                .setKernelDevice(convert("0:0:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:0:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      String.format("/dev/zvol/storage/vm/%s/disk-0_0_0", machine.id());

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,virtio-blk,/dev/zvol/storage/vm/%s/disk-0_0_0 %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxSimpleCD()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .addDiskAttachments(
              WXMBootDiskAttachment.builder()
                .setDevice(convert("0:1:0"))
                .setBackend(WXMStorageBackendZFSVolume.builder().build())
                .build()
            )
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIOpticalDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .build()
        )
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      String.format("/dev/zvol/storage/vm/%s/disk-0_1_0", machine.id());

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (cd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (cd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-cd,/dev/zvol/storage/vm/%s/disk-0_1_0 %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxVirtioNetTAP()
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
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .build()
            ).build()
        )
        .addDevices(
          WXMDeviceVirtioNetwork.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setBackend(
              WXMTap.builder()
                .setAddress(WXMMACAddress.of("1b:61:cb:ba:c0:12"))
                .setName(WXMTAPDeviceName.of("tap23"))
                .build())
            .build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = new ArrayList<>(commands.configurationCommands());

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("tap23", arguments.get(0));
      assertEquals("create", arguments.get(1));
      assertEquals(2, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("tap23", arguments.get(0));
      assertEquals("ether", arguments.get(1));
      assertEquals("1b:61:cb:ba:c0:12", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/usr/local/sbin/grub-bhyve", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals(5, arguments.size());
    }

    final var lastExec = commands.lastExecution().orElseThrow();
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:0:0,hostbridge", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,ahci-hd,/tmp/file", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:2:0,virtio-net,tap23,mac=1b:61:cb:ba:c0:12", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-hd,/tmp/file -s 0:2:0,virtio-net,tap23,mac=1b:61:cb:ba:c0:12 %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxVirtioNetTAPGroups()
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
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .build()
            ).build()
        )
        .addDevices(
          WXMDeviceVirtioNetwork.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setBackend(
              WXMTap.builder()
                .setAddress(WXMMACAddress.of("1b:61:cb:ba:c0:12"))
                .setName(WXMTAPDeviceName.of("tap23"))
                .addGroups(WXMInterfaceGroupName.of("wwwUsers"))
                .addGroups(WXMInterfaceGroupName.of("ntpUsers"))
                .build())
            .build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = new ArrayList<>(commands.configurationCommands());

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("tap23", arguments.get(0));
      assertEquals("create", arguments.get(1));
      assertEquals(2, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("tap23", arguments.get(0));
      assertEquals("ether", arguments.get(1));
      assertEquals("1b:61:cb:ba:c0:12", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("tap23", arguments.get(0));
      assertEquals("group", arguments.get(1));
      assertEquals("wwwUsers", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("tap23", arguments.get(0));
      assertEquals("group", arguments.get(1));
      assertEquals("ntpUsers", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/usr/local/sbin/grub-bhyve", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals(5, arguments.size());
    }

    final var lastExec = commands.lastExecution().orElseThrow();
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:0:0,hostbridge", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,ahci-hd,/tmp/file", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:2:0,virtio-net,tap23,mac=1b:61:cb:ba:c0:12", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-hd,/tmp/file -s 0:2:0,virtio-net,tap23,mac=1b:61:cb:ba:c0:12 %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxVirtioNetVMNet()
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
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .build()
            ).build()
        )
        .addDevices(
          WXMDeviceVirtioNetwork.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setBackend(
              WXMVMNet.builder()
                .setAddress(WXMMACAddress.of("1b:61:cb:ba:c0:12"))
                .setName(WXMVMNetDeviceName.of("vmnet23"))
                .build())
            .build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = new ArrayList<>(commands.configurationCommands());

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("create", arguments.get(1));
      assertEquals(2, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("ether", arguments.get(1));
      assertEquals("1b:61:cb:ba:c0:12", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/usr/local/sbin/grub-bhyve", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals(5, arguments.size());
    }

    final var lastExec = commands.lastExecution().orElseThrow();
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:0:0,hostbridge", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,ahci-hd,/tmp/file", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:2:0,virtio-net,vmnet23,mac=1b:61:cb:ba:c0:12", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-hd,/tmp/file -s 0:2:0,virtio-net,vmnet23,mac=1b:61:cb:ba:c0:12 %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxVirtioNetVMNetGroups()
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
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .build()
            ).build()
        )
        .addDevices(
          WXMDeviceVirtioNetwork.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setBackend(
              WXMVMNet.builder()
                .setAddress(WXMMACAddress.of("1b:61:cb:ba:c0:12"))
                .setName(WXMVMNetDeviceName.of("vmnet23"))
                .addGroups(WXMInterfaceGroupName.of("wwwUsers"))
                .addGroups(WXMInterfaceGroupName.of("ntpUsers"))
                .build())
            .build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = new ArrayList<>(commands.configurationCommands());

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("create", arguments.get(1));
      assertEquals(2, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("ether", arguments.get(1));
      assertEquals("1b:61:cb:ba:c0:12", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("group", arguments.get(1));
      assertEquals("wwwUsers", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("group", arguments.get(1));
      assertEquals("ntpUsers", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/usr/local/sbin/grub-bhyve", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals(5, arguments.size());
    }

    final var lastExec = commands.lastExecution().orElseThrow();
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:0:0,hostbridge", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,ahci-hd,/tmp/file", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:2:0,virtio-net,vmnet23,mac=1b:61:cb:ba:c0:12", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-hd,/tmp/file -s 0:2:0,virtio-net,vmnet23,mac=1b:61:cb:ba:c0:12 %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxE1000Net()
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
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .build()
            ).build()
        )
        .addDevices(
          WXMDeviceE1000.builder()
            .setDeviceSlot(convert("0:2:0"))
            .setBackend(
              WXMVMNet.builder()
                .setAddress(WXMMACAddress.of("1b:61:cb:ba:c0:12"))
                .setName(WXMVMNetDeviceName.of("vmnet23"))
                .build())
            .build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = new ArrayList<>(commands.configurationCommands());

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("create", arguments.get(1));
      assertEquals(2, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/sbin/ifconfig", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals("vmnet23", arguments.get(0));
      assertEquals("ether", arguments.get(1));
      assertEquals("1b:61:cb:ba:c0:12", arguments.get(2));
      assertEquals(3, arguments.size());
    }

    {
      final var cmd = configs.remove(0);
      assertEquals("/usr/local/sbin/grub-bhyve", cmd.executable().toString());
      final var arguments = cmd.arguments();
      assertEquals(5, arguments.size());
    }

    final var lastExec = commands.lastExecution().orElseThrow();
    final var lastArgs = new ArrayList<>(lastExec.arguments());
    assertEquals("-U", lastArgs.remove(0));
    assertEquals(machine.id().toString(), lastArgs.remove(0));
    assertEquals("-P", lastArgs.remove(0));
    assertEquals("-A", lastArgs.remove(0));
    assertEquals("-w", lastArgs.remove(0));
    assertEquals("-H", lastArgs.remove(0));
    assertEquals("-c", lastArgs.remove(0));
    assertEquals("cpus=1,sockets=1,cores=1,threads=1", lastArgs.remove(0));
    assertEquals("-m", lastArgs.remove(0));
    assertEquals("512M", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:0:0,hostbridge", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:1:0,ahci-hd,/tmp/file", lastArgs.remove(0));
    assertEquals("-s", lastArgs.remove(0));
    assertEquals("0:2:0,e1000,vmnet23,mac=1b:61:cb:ba:c0:12", lastArgs.remove(0));
    assertEquals(WXMShortIDs.encode(machine.id()), lastArgs.remove(0));
    assertEquals(0, lastArgs.size());

    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-hd,/tmp/file -s 0:2:0,e1000,vmnet23,mac=1b:61:cb:ba:c0:12 %s",
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxLPC()
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
                .setKernelDevice(convert("0:1:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:1:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceHostBridge.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setVendor(WXM_UNSPECIFIED)
            .build()
        )
        .addDevices(
          WXMDeviceAHCIDisk.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setBackend(
              WXMStorageBackendFile.builder()
                .setFile(Path.of("/tmp/file"))
                .build()
            ).build()
        )
        .addDevices(
          WXMDeviceLPC.builder()
            .setDeviceSlot(convert("0:2:0"))
            .addBackends(
              WXMTTYBackendFile.builder()
                .setDevice("bootrom")
                .setPath(Paths.get("/tmp/rom"))
                .build()
            )
            .addBackends(
              WXMTTYBackendNMDM.builder()
                .setDevice("com1")
                .build()
            )
            .addBackends(
              WXMTTYBackendStdio.builder()
                .setDevice("com2")
                .build()
            )
            .build()
        ).build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains("/tmp/file"));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,hostbridge -s 0:1:0,ahci-hd,/tmp/file -s 0:2:0,lpc -l bootrom,/tmp/rom -l com1,/dev/nmdm_%s_A -l com2,stdio %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
  }

  @Test
  public void linuxPassthru()
    throws WXMException
  {
    final var machine =
      WXMVirtualMachine.builder()
        .setId(UUID.randomUUID())
        .setName(WXMMachineName.of("vm"))
        .setFlags(
          WXMFlags.builder()
            .setWireGuestMemory(true)
            .build())
        .addBootConfigurations(
          WXMBootConfigurationGRUBBhyve.builder()
            .setName(WXMBootConfigurationName.of("install"))
            .setKernelInstructions(
              WXMGRUBKernelLinux.builder()
                .setKernelDevice(convert("0:0:0"))
                .setKernelPath(Paths.get("/vmlinuz"))
                .addKernelArguments("root=/dev/sda1")
                .addKernelArguments("init=/sbin/runit-init")
                .setInitRDDevice(convert("0:0:0"))
                .setInitRDPath(Paths.get("/initrd.img"))
                .build())
            .build()
        )
        .addDevices(
          WXMDeviceVirtioBlockStorage.builder()
            .setDeviceSlot(convert("0:0:0"))
            .setBackend(WXMStorageBackendZFSVolume.builder().build())
            .build()
        )
        .addDevices(
          WXMDevicePassthru.builder()
            .setDeviceSlot(convert("0:1:0"))
            .setHostPCISlot(convert("1:2:3"))
            .build())
        .build();

    final var evaluator =
      new WXMBootConfigurationEvaluator(
        this.clientConfiguration,
        machine,
        WXMBootConfigurationName.of("install")
      );

    final var evaluated =
      (WXMEvaluatedBootConfigurationGRUBBhyve) evaluator.evaluate();
    LOG.debug("evaluated: {}", evaluated);

    final String expectedZFSDisk =
      String.format(
        "/dev/zvol/storage/vm/%s/disk-0_0_0",
        machine.id()
      );

    final var mapLines = evaluated.deviceMap();
    assertTrue(mapLines.get(0).contains(expectedZFSDisk));
    assertEquals(1, mapLines.size());

    final var grub = evaluated.grubConfiguration();
    assertEquals(
      "linux (hd0)/vmlinuz root=/dev/sda1 init=/sbin/runit-init",
      grub.get(0)
    );
    assertEquals("initrd (hd0)/initrd.img", grub.get(1));
    assertEquals("boot", grub.get(2));
    assertEquals(3, grub.size());

    final var commands = evaluated.commands();
    final var configs = commands.configurationCommands();
    final var cmd0 = configs.get(0);
    assertEquals("/usr/local/sbin/grub-bhyve", cmd0.executable().toString());
    assertEquals(1, configs.size());

    final var lastExec = commands.lastExecution().orElseThrow();
    assertEquals(
      String.format(
        "/usr/sbin/bhyve -U %s -P -A -w -S -H -c cpus=1,sockets=1,cores=1,threads=1 -m 512M -s 0:0:0,virtio-blk,/dev/zvol/storage/vm/%s/disk-0_0_0 -s 0:1:0,passthru,1/2/3 %s",
        machine.id(),
        machine.id(),
        WXMShortIDs.encode(machine.id())),
      lastExec.toString()
    );
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
