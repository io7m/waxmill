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
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDeviceSlot;
import com.io7m.waxmill.machines.WXMMachineMessages;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMTTYBackends;
import com.io7m.waxmill.machines.WXMVirtualMachines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType;
import static com.io7m.waxmill.machines.WXMTTYBackends.NMDMSide.NMDM_GUEST;

@Parameters(commandDescription = "Add an LPC device to a virtual machine.")
public final class WXMCommandVMAddLPC extends
  WXMAbstractCommandWithConfiguration
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddLPC.class);

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
    names = "--add-backend",
    description = "A specification of the TTY device backend to add",
    required = true,
    converter = WXMTTYBackendConverter.class
  )
  private List<WXMTTYBackendType> backends = List.of();

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

  public WXMCommandVMAddLPC(
    final CLPCommandContextType inContext)
  {
    super(LOG, inContext);
  }

  @Override
  public String name()
  {
    return "vm-add-lpc-device";
  }

  @Override
  public String extendedHelp()
  {
    final var messages = this.messages();
    return String.join("", List.of(
      messages.format("vmAddLPCDeviceHelp"),
      messages.format("ttyBackendSpec")
    ));
  }

  @Override
  protected Status executeActualWithConfiguration(
    final Path configurationPath)
    throws Exception
  {
    try (var client = WXMServices.clients().open(configurationPath)) {
      final var machine = client.vmFind(this.id);

      final Map<String, WXMTTYBackendType> backendMap = new HashMap<>(3);
      for (final WXMTTYBackendType backend : this.backends) {
        final var device = backend.device();
        if (backendMap.put(device, backend) != null) {
          this.error("errorDeviceNamesUnique", device);
          return FAILURE;
        }
      }

      final var lpc =
        WXMDeviceLPC.builder()
          .setDeviceSlot(this.deviceSlot)
          .addAllBackends(backendMap.values())
          .setComment(this.comment)
          .build();

      final var updatedMachine =
        WXMVirtualMachines.updateWithDevice(
          WXMMachineMessages.create(),
          machine,
          lpc,
          this.replace
        );

      client.vmUpdate(updatedMachine);

      this.info("infoAddedLPC", this.deviceSlot);
      for (final var entry : backendMap.entrySet()) {
        final var device = entry.getValue();
        switch (device.kind()) {
          case WXM_FILE:
            final var fileDeviceBackend = (WXMTTYBackendFile) device;
            this.info(
              "infoBackendFile",
              fileDeviceBackend.device(),
              fileDeviceBackend.path());
            break;
          case WXM_NMDM:
            final var nmdmBackend = (WXMTTYBackendNMDM) device;
            this.info(
              "infoBackendNMDM",
              nmdmBackend.device(),
              WXMTTYBackends.nmdmPath(
                FileSystems.getDefault(),
                machine.id(),
                NMDM_GUEST));
            break;
          case WXM_STDIO:
            final var stdioBackend = (WXMTTYBackendStdio) device;
            this.info("infoBackendStdio", stdioBackend.device());
            break;
        }
      }
    }
    return SUCCESS;
  }
}
