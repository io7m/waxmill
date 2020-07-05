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

package com.io7m.waxmill.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMDeviceSlots;
import com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMVirtioNetworkBackendType;
import com.io7m.waxmill.machines.WXMDeviceVirtioNetwork;
import com.io7m.waxmill.machines.WXMMachineMessages;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

@Parameters(commandDescription = "Add a virtio network device to a virtual machine.")
public final class WXMCommandVMAddVirtioNetworkDevice
  extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddVirtioNetworkDevice.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--comment",
    description = "A comment describing the new device",
    required = false
  )
  private String comment = "";

  @Parameter(
    names = "--device-slot",
    description = "The slot to which the device will be attached.",
    required = true,
    converter = WXMDeviceSlotConverter.class
  )
  private WXMDeviceSlot deviceSlot;

  @Parameter(
    names = "--backend",
    description = "A specification of the Virtio network device backend to add",
    required = true,
    converter = WXMVirtioNetworkBackendConverter.class
  )
  private WXMVirtioNetworkBackendType backend;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMAddVirtioNetworkDevice(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-add-virtio-network-device";
  }

  @Override
  public String extendedHelp()
  {
    final var messages = this.messages();
    return String.join("", List.of(
      messages.format("vmAddVirtioNetworkDeviceHelp"),
      messages.format("virtioNetworkBackendSpec")
    ));
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);
      this.deviceSlot =
        WXMDeviceSlots.checkDeviceSlotNotUsed(
          WXMMachineMessages.create(),
          machine,
          this.deviceSlot
        );

      final var virtio =
        WXMDeviceVirtioNetwork.builder()
          .setDeviceSlot(this.deviceSlot)
          .setBackend(this.backend)
          .setComment(this.comment)
          .build();

      final var updatedMachine =
        WXMVirtualMachine.builder()
          .from(machine)
          .addDevices(virtio)
          .build();

      client.vmUpdate(updatedMachine);

      this.info("infoAddedVirtioNet", this.deviceSlot);
      switch (this.backend.kind()) {
        case WXM_TAP:
          final var tap = (WXMTap) this.backend;
          this.info(
            "infoBackendTAP",
            tap.name().value(),
            tap.address().value()
          );
          break;
        case WXM_VMNET:
          final var vmnet = (WXMVMNet) this.backend;
          this.info(
            "infoBackendVMNet",
            vmnet.name().value(),
            vmnet.address().value()
          );
          break;
      }
    }
    return SUCCESS;
  }
}
