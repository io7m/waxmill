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

package com.io7m.waxmill.tests;

import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceVirtioNetwork;
import com.io7m.waxmill.machines.WXMSectorSizes;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.parser.api.WXMParseError;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_AMD;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendFileType.WXMOpenOption.NO_CACHE;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendFileType.WXMOpenOption.READ_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class WXMVirtualMachineParserContract
{
  private Path directory;
  private ArrayList<WXMParseError> errors;

  protected abstract Logger logger();

  protected abstract WXMVirtualMachineParserProviderType parsers();

  @BeforeEach
  public void testSetup()
    throws Exception
  {
    this.directory = WXMTestDirectories.createTempDirectory();
    this.errors = new ArrayList<>();
  }

  /**
   * Individual machines are parsed correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public void exampleParses()
    throws Exception
  {
    final var machineOpt = this.parseResource("vm0.xml");
    assertTrue(machineOpt.isPresent());
    final var machineSet = machineOpt.get();
    final var machine =
      machineSet.machines()
        .get(UUID.fromString("1a438a53-2fcd-498f-8cc2-0ff0456e3dc4"));

    assertEquals("An example virtual machine.", machine.comment());

    final var cpu = machine.cpuTopology();
    assertEquals(
      "A CPU topology simulating a quad core CPU. The first two CPUs are pinned to the first two CPUs on the host.",
      cpu.comment());
    assertEquals(1, cpu.sockets());
    assertEquals(1, cpu.threads());
    assertEquals(4, cpu.cores());
    assertEquals(4, cpu.cpus());

    final var pins = cpu.pinnedCPUs();
    assertEquals(2, pins.size());
    assertEquals(0, pins.get(0).hostCPU());
    assertEquals(0, pins.get(0).guestCPU());
    assertEquals(1, pins.get(1).hostCPU());
    assertEquals(1, pins.get(1).guestCPU());

    final var memory = machine.memory();
    assertEquals("512mb of memory.", memory.comment());
    assertEquals(BigInteger.valueOf(512000000L), memory.totalBytes());

    final var devices = machine.devices();
    assertEquals(6, devices.size());

    final var hostBridge = (WXMDeviceHostBridge) devices.get(0);
    assertEquals(WXM_AMD, hostBridge.vendor());
    assertEquals(0, hostBridge.id().value());
    assertEquals("An AMD-branded host bridge.", hostBridge.comment());

    final var net0 = (WXMDeviceVirtioNetwork) devices.get(1);
    assertEquals(1, net0.id().value());
    assertEquals("A TAP-based network device.", net0.comment());
    final var tap = (WXMTap) net0.backend();
    assertEquals("A TAP device.", tap.comment());
    assertEquals("d7:94:b5:60:0d:ac", tap.address().value());
    assertEquals("tap23", tap.name().value());

    final var net1 = (WXMDeviceVirtioNetwork) devices.get(2);
    assertEquals(2, net1.id().value());
    assertEquals("A VMNet-based network device.", net1.comment());
    final var vmnet = (WXMVMNet) net1.backend();
    assertEquals("A VMNet device.", vmnet.comment());
    assertEquals("d7:92:b5:60:0d:ac", vmnet.address().value());
    assertEquals("vmnet23", vmnet.name().value());

    final var hd0 = (WXMDeviceAHCIDisk) devices.get(3);
    assertEquals(3, hd0.id().value());
    assertEquals("A disk device.", hd0.comment());
    final var block0 = (WXMStorageBackendFile) hd0.backend();
    assertEquals("File-based storage.", block0.comment());
    assertEquals(Set.of(NO_CACHE), block0.options());
    assertEquals(
      WXMSectorSizes.builder()
        .setLogical(BigInteger.valueOf(4096L))
        .setPhysical(BigInteger.valueOf(4096L))
        .build(),
      block0.sectorSizes().get()
    );

    final var hd1 = (WXMDeviceAHCIOpticalDisk) devices.get(4);
    assertEquals(4, hd1.id().value());
    assertEquals("An optical disk device.", hd1.comment());
    final var block1 = (WXMStorageBackendFile) hd1.backend();
    assertEquals("File-based storage.", block1.comment());
    assertEquals(Set.of(READ_ONLY), block1.options());
    assertEquals(Optional.empty(), block1.sectorSizes());

    final var lpc = (WXMDeviceLPC) devices.get(5);
    assertEquals(5, lpc.id().value());
    assertEquals("A TTY based on a filesystem socket.", lpc.comment());

    final var stdio = (WXMTTYBackendStdio) lpc.backends().get("com0");
    assertEquals("com0", stdio.device());
    final var file = (WXMTTYBackendFile) lpc.backends().get("com1");
    assertEquals("com1", file.device());
    assertEquals(
      "/dev/nmdm_1a438a53-2fcd-498f-8cc2-0ff0456e3dc4_B",
      file.path().toString());

    final var flags = machine.flags();
    assertFalse(flags.disableMPTableGeneration());
    assertFalse(flags.forceVirtualIOPCIToUseMSI());
    assertTrue(flags.generateACPITables());
    assertFalse(flags.guestAPICIsX2APIC());
    assertFalse(flags.includeGuestMemoryInCoreFiles());
    assertTrue(flags.realTimeClockIsUTC());
    assertFalse(flags.wireGuestMemory());
    assertTrue(flags.yieldCPUOnHLT());
  }

  /**
   * A machine is the same whether parsed individually or as part of a set.
   *
   * @throws Exception On errors
   */

  @Test
  public void exampleParsesEquivalent()
    throws Exception
  {
    final var machine0Opt = this.parseResource("vm0.xml");
    assertTrue(machine0Opt.isPresent());
    final var machineSet0 = machine0Opt.get();

    final var machine1Opt = this.parseResource("vmSet0.xml");
    assertTrue(machine1Opt.isPresent());
    final var machineSet1 = machine1Opt.get();

    assertEquals(machineSet0, machineSet1);
  }

  private Optional<WXMVirtualMachineSet> parseResource(
    final String name)
    throws IOException
  {
    try (var stream = WXMTestDirectories.resourceStreamOf(
      WXMVirtualMachineParserContract.class,
      this.directory,
      name)) {

      try (var parser = this.parsers().create(
        URI.create("urn:unknown"),
        stream,
        this::logError)) {
        return parser.parse();
      }
    }
  }

  private void logError(
    final WXMParseError error)
  {
    this.logger().debug("error: {}", error);
    this.errors.add(error);
  }
}
