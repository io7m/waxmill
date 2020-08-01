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

import com.io7m.waxmill.machines.WXMBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMBootConfigurationName;
import com.io7m.waxmill.machines.WXMCPUTopology;
import com.io7m.waxmill.machines.WXMCommandExecution;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceVirtioNetwork;
import com.io7m.waxmill.machines.WXMEvaluatedBootCommands;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationGRUBBhyve;
import com.io7m.waxmill.machines.WXMEvaluatedBootConfigurationUEFI;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;
import com.io7m.waxmill.machines.WXMGRUBKernelOpenBSD;
import com.io7m.waxmill.machines.WXMMACAddress;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMMemory;
import com.io7m.waxmill.machines.WXMPinCPU;
import com.io7m.waxmill.machines.WXMSectorSizes;
import com.io7m.waxmill.machines.WXMStorageBackendFile;
import com.io7m.waxmill.machines.WXMTAPDeviceName;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVMNetDeviceName;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.parser.api.WXMParseError;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class WXMEqualsTest
{
  private static final List<WXMClassUnderTest> CLASSES =
    List.of(
      new WXMClassUnderTest(
        WXMCommandExecution.class,
        Set.of("executable", "arguments")),
      new WXMClassUnderTest(
        WXMEvaluatedBootCommands.class,
        Set.of("configurationCommands", "lastExecution")),
      new WXMClassUnderTest(
        WXMBootConfigurationGRUBBhyve.class,
        Set.of("comment", "diskAttachments", "name", "kernelInstructions")),
      new WXMClassUnderTest(
        WXMBootConfigurationName.class,
        Set.of("value")),
      new WXMClassUnderTest(
        WXMEvaluatedBootConfigurationGRUBBhyve.class,
        Set.of(
          "requiredPaths",
          "requiredNMDMs",
          "deviceMap",
          "deviceMapFile",
          "grubConfiguration",
          "grubConfigurationFile",
          "commands")),
      new WXMClassUnderTest(
        WXMEvaluatedBootConfigurationUEFI.class,
        Set.of(
          "requiredPaths",
          "requiredNMDMs",
          "commands")),
      new WXMClassUnderTest(
        WXMGRUBKernelLinux.class,
        Set.of(
          "kernelDevice",
          "kernelPath",
          "initRDDevice",
          "initRDPath",
          "kernelArguments")),
      new WXMClassUnderTest(
        WXMGRUBKernelOpenBSD.class,
        Set.of("bootDevice", "kernelPath", "partition")),
      new WXMClassUnderTest(
        WXMParseError.class,
        Set.of("lexical", "severity", "message")),
      new WXMClassUnderTest(
        WXMFlags.class,
        Set.of()),
      new WXMClassUnderTest(
        WXMMemory.class,
        Set.of("comment", "megabytes", "gigabytes")),
      new WXMClassUnderTest(
        WXMStorageBackendFile.class,
        Set.of("file", "options", "comment")),
      new WXMClassUnderTest(
        WXMCPUTopology.class,
        Set.of("comment", "pinnedCPUs")),
      new WXMClassUnderTest(
        WXMDeviceAHCIDisk.class,
        Set.of("deviceSlot", "comment", "backend")),
      new WXMClassUnderTest(
        WXMDeviceAHCIOpticalDisk.class,
        Set.of("deviceSlot", "comment")),
      new WXMClassUnderTest(
        WXMDeviceHostBridge.class,
        Set.of("deviceSlot", "comment", "vendor")),
      new WXMClassUnderTest(
        WXMDeviceSlot.class,
        Set.of()),
      new WXMClassUnderTest(
        WXMDeviceLPC.class,
        Set.of("deviceSlot", "backends", "comment")),
      new WXMClassUnderTest(
        WXMDeviceVirtioNetwork.class,
        Set.of("deviceSlot", "backend", "comment")),
      new WXMClassUnderTest(
        WXMMACAddress.class,
        Set.of("value")),
      new WXMClassUnderTest(
        WXMMachineName.class,
        Set.of("value")),
      new WXMClassUnderTest(
        WXMPinCPU.class,
        Set.of()),
      new WXMClassUnderTest(
        WXMSectorSizes.class,
        Set.of("logical", "physical")),
      new WXMClassUnderTest(
        WXMTap.class,
        Set.of("comment", "name", "hostMAC", "guestMAC", "groups")),
      new WXMClassUnderTest(
        WXMTAPDeviceName.class,
        Set.of("value")),
      new WXMClassUnderTest(
        WXMTTYBackendFile.class,
        Set.of("comment", "path", "device")),
      new WXMClassUnderTest(
        WXMTTYBackendStdio.class,
        Set.of("comment", "device")),
      new WXMClassUnderTest(
        WXMVirtualMachine.class,
        Set.of(
          "id",
          "bootConfigurations",
          "comment",
          "name",
          "flags",
          "devices",
          "cpuTopology",
          "memory")),
      new WXMClassUnderTest(
        WXMVirtualMachineSet.class,
        Set.of("machines")),
      new WXMClassUnderTest(
        WXMVMNet.class,
        Set.of("comment", "name", "hostMAC", "guestMAC", "groups")),
      new WXMClassUnderTest(
        WXMVMNetDeviceName.class,
        Set.of("value"))
    );

  private static DynamicTest makeEqualsCheckDynamicTest(
    final Class<?> clazz)
  {
    final var name =
      String.format("checkEqualsFor_%s", clazz.getCanonicalName());

    return DynamicTest.dynamicTest(
      name,
      () -> {
        EqualsVerifier.forClass(clazz)
          .withNonnullFields(nonNullFieldsFor(clazz))
          .verify();
      });
  }

  private static String[] nonNullFieldsFor(
    final Class<?> clazz)
  {
    final var names =
      CLASSES.stream()
        .filter(c -> Objects.equals(c.theClass, clazz))
        .map(c -> c.nonNullFields)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
          String.format("Could not get non-null fields for %s", clazz))
        );

    final var nameArray = new String[names.size()];
    names.toArray(nameArray);
    return nameArray;
  }

  @TestFactory
  public Stream<DynamicTest> equalsForGeneratedClasses()
  {
    return CLASSES.stream()
      .map(c -> c.theClass)
      .map(WXMEqualsTest::makeEqualsCheckDynamicTest);
  }
}
