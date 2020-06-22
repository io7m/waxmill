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

import com.io7m.jaffirm.core.PreconditionViolationException;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceID;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMMachineName;
import com.io7m.waxmill.machines.WXMStorageBackendZFSVolume;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceHostBridgeType.Vendor.WXM_UNSPECIFIED;

public final class WXMVirtualMachineTest
{
  @Test
  public void virtualMachineDuplicateDevice()
  {
    final var ex =
      Assertions.assertThrows(IllegalStateException.class, () -> {
        WXMVirtualMachine.builder()
          .setName(WXMMachineName.of("test"))
          .setId(UUID.randomUUID())
          .addDevices(
            WXMDeviceHostBridge.builder()
              .setId(WXMDeviceID.of(0))
              .setVendor(WXM_UNSPECIFIED)
              .build())
          .addDevices(
            WXMDeviceVirtioBlockStorage.builder()
              .setId(WXMDeviceID.of(0))
              .setBackend(WXMStorageBackendZFSVolume.builder().build())
              .build())
          .build();
      });

    Assertions.assertTrue(ex.getMessage().contains("Duplicate key WXMDeviceID"));
  }

  @Test
  public void virtualMachineMultipleLPC()
  {
    final var ex =
      Assertions.assertThrows(PreconditionViolationException.class, () -> {
        WXMVirtualMachine.builder()
          .setName(WXMMachineName.of("test"))
          .setId(UUID.randomUUID())
          .addDevices(
            WXMDeviceLPC.builder()
              .setId(WXMDeviceID.of(0))
              .addBackends(
                WXMTTYBackendStdio.builder()
                  .setDevice("com1")
                  .build())
              .build())
          .addDevices(
            WXMDeviceLPC.builder()
              .setId(WXMDeviceID.of(1))
              .addBackends(
                WXMTTYBackendStdio.builder()
                  .setDevice("com1")
                  .build())
              .build())
          .build();
      });

    Assertions.assertTrue(ex.getMessage().contains("At most 1 LPC"));
  }

  @Test
  public void virtualMachineMultipleHostBridge()
  {
    final var ex =
      Assertions.assertThrows(PreconditionViolationException.class, () -> {
        WXMVirtualMachine.builder()
          .setName(WXMMachineName.of("test"))
          .setId(UUID.randomUUID())
          .addDevices(
            WXMDeviceHostBridge.builder()
              .setId(WXMDeviceID.of(0))
              .setVendor(WXM_UNSPECIFIED)
              .build())
          .addDevices(
            WXMDeviceHostBridge.builder()
              .setId(WXMDeviceID.of(1))
              .setVendor(WXM_UNSPECIFIED)
              .build())
          .build();
      });

    Assertions.assertTrue(ex.getMessage().contains("At most 1 host bridge"));
  }
}
