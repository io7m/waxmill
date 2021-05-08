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
import com.io7m.waxmill.machines.WXMDeviceFramebuffer;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMMachineMessages;
import com.io7m.waxmill.machines.WXMVirtualMachines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceFramebufferType.WXMVGAConfiguration;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceFramebufferType.WXMVGAConfiguration.IO;

/**
 * The "vm-add-framebuffer-device" command.
 */

@Parameters(commandDescription = "Add a framebuffer device to a virtual machine.")
public final class WXMCommandVMAddFramebufferDevice
  extends WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddFramebufferDevice.class);

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
    names = "--width",
    description = "The framebuffer width",
    required = false
  )
  private int width = 1024;

  @Parameter(
    names = "--height",
    description = "The framebuffer height",
    required = false
  )
  private int height = 1024;

  @Parameter(
    names = "--listen-address",
    description = "The VNC server listen address",
    required = false
  )
  private String listenAddress = "localhost";

  @Parameter(
    names = "--listen-port",
    description = "The VNC server listen port",
    required = false
  )
  private int listenPort = 5900;

  @Parameter(
    names = "--vga-configuration",
    description = "The guest VGA configuration",
    required = false
  )
  private WXMVGAConfiguration vgaConfiguration = IO;

  @Parameter(
    names = "--wait-for-vnc",
    description = "Will cause the machine to wait for a VNC connection before booting",
    required = false,
    arity = 1
  )
  private boolean waitForVNC;

  @Parameter(
    names = "--replace",
    description = "Replace an existing device, if one exists",
    required = false,
    arity = 1
  )
  private boolean replace;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVMAddFramebufferDevice(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-add-framebuffer-device";
  }

  @Override
  public String extendedHelp()
  {
    final var messages = this.messages();
    return String.join("", List.of(
      messages.format("vmAddFramebufferDeviceHelp")
    ));
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);

      final var virtio =
        WXMDeviceFramebuffer.builder()
          .setDeviceSlot(this.deviceSlot)
          .setComment(this.comment)
          .setHeight(this.height)
          .setWidth(this.width)
          .setListenAddress(InetAddress.getByName(this.listenAddress))
          .setListenPort(this.listenPort)
          .setVgaConfiguration(this.vgaConfiguration)
          .setWaitForVNC(this.waitForVNC)
          .build();

      final var updatedMachine =
        WXMVirtualMachines.updateWithDevice(
          WXMMachineMessages.create(),
          machine,
          virtio,
          this.replace
        );

      client.vmUpdate(updatedMachine);
    }
    return SUCCESS;
  }
}
