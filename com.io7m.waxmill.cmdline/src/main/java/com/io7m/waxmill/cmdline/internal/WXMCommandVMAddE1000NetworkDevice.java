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
import com.io7m.waxmill.machines.WXMDeviceE1000;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMInterfaceGroupName;
import com.io7m.waxmill.machines.WXMMACAddress;
import com.io7m.waxmill.machines.WXMMachineMessages;
import com.io7m.waxmill.machines.WXMNetworkDeviceBackendType;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVirtualMachines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "vm-add-e1000-network-device" command.
 */

@Parameters(commandDescription = "Add an e1000 network device to a virtual machine.")
public final class WXMCommandVMAddE1000NetworkDevice
  extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddE1000NetworkDevice.class);

  @Parameter(
    names = "--machine",
    description = "The ID of the virtual machine",
    required = true,
    converter = WXMUUIDConverter.class
  )
  private UUID id;

  @Parameter(
    names = "--comment",
    description = "A comment describing the new device"
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
    names = "--replace",
    description = "Replace an existing device, if one exists",
    arity = 1
  )
  private boolean replace;

  @Parameter(
    names = "--type",
    description = "The type of network backend",
    required = true,
    converter = WXMNetworkDeviceKindConverter.class
  )
  private WXMNetworkDeviceBackendType.Kind type;

  @Parameter(
    names = "--host-mac",
    description = "The MAC address on the host device",
    required = true,
    converter = WXMMACAddressConverter.class
  )
  private WXMMACAddress hostMAC;

  @Parameter(
    names = "--guest-mac",
    description = "The MAC address on the guest device",
    required = true,
    converter = WXMMACAddressConverter.class
  )
  private WXMMACAddress guestMAC;

  @Parameter(
    names = "--name",
    description = "The name of the device"
  )
  private String deviceName;

  @Parameter(
    names = "--interface-group",
    description = "The group to which the device will belong on the host",
    converter = WXMInterfaceGroupNameConverter.class
  )
  private List<WXMInterfaceGroupName> groups = new ArrayList<>();

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMAddE1000NetworkDevice(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-add-e1000-network-device";
  }

  @Override
  public String extendedHelp()
  {
    return this.messages().format("vmAddE1000NetworkDeviceHelp");
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);

      final var backend =
        new WXMNetworkBackendArguments()
          .parse(
            WXMNamedParameter.of("--type", this.type),
            WXMNamedParameter.of("--comment", this.comment),
            WXMNamedParameters.optional("--name", this.deviceName),
            WXMNamedParameters.optional("--host-mac", this.hostMAC),
            WXMNamedParameters.optional("--guest-mac", this.guestMAC),
            WXMNamedParameter.of("--interface-group", this.groups)
          );

      final var virtio =
        WXMDeviceE1000.builder()
          .setDeviceSlot(this.deviceSlot)
          .setBackend(backend)
          .setComment(this.comment)
          .build();

      final var updatedMachine =
        WXMVirtualMachines.updateWithDevice(
          WXMMachineMessages.create(),
          machine,
          virtio,
          this.replace
        );

      client.vmUpdate(updatedMachine);

      this.info("infoAddedE1000Net", this.deviceSlot);
      switch (this.type) {
        case WXM_TAP:
          final var tap = (WXMTap) backend;
          this.info(
            "infoBackendTAP",
            tap.name().value(),
            tap.hostMAC().value(),
            tap.guestMAC().value()
          );
          break;
        case WXM_VMNET:
          final var vmnet = (WXMVMNet) backend;
          this.info(
            "infoBackendVMNet",
            vmnet.name().value(),
            vmnet.hostMAC().value(),
            vmnet.guestMAC().value()
          );
          break;
      }
    }
    return SUCCESS;
  }
}
