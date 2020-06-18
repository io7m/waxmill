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
import com.io7m.waxmill.client.api.WXMDeviceLPC;
import com.io7m.waxmill.client.api.WXMException;
import com.io7m.waxmill.client.api.WXMTTYBackendFile;
import com.io7m.waxmill.client.api.WXMTTYBackendNMDM;
import com.io7m.waxmill.client.api.WXMTTYBackendStdio;
import com.io7m.waxmill.client.api.WXMTTYBackends;
import com.io7m.waxmill.client.api.WXMVirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.io7m.waxmill.client.api.WXMDeviceType.WXMTTYBackendType;
import static com.io7m.waxmill.client.api.WXMTTYBackends.NMDMSide.NMDM_GUEST;
import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.FAILURE;
import static com.io7m.waxmill.cmdline.internal.WXMCommandType.Status.SUCCESS;
import static com.io7m.waxmill.cmdline.internal.WXMEnvironment.checkConfigurationPath;

@Parameters(commandDescription = "Add an LPC device to a virtual machine.")
public final class WXMCommandVMAddLPC extends WXMCommandRoot
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandVMAddLPC.class);

  @Parameter(
    names = "--configuration",
    description = "The path to the configuration file (environment variable: $WAXMILL_CONFIGURATION_FILE)",
    required = false
  )
  private Path configurationFile = WXMEnvironment.configurationFile();

  @Parameter(
    names = "--id",
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
  private String comment;

  @Parameter(
    names = "--add-backend",
    description = "A specification of the device backend to add "
      + "(such as 'file;com1;/dev/nmdm54B', 'stdio;com2', 'nmdm;com1;')",
    required = true,
    converter = WXMTTYBackendConverter.class
  )
  private List<WXMTTYBackendType> backends = List.of();

  public WXMCommandVMAddLPC()
  {

  }

  @Override
  public Status execute()
    throws Exception
  {
    if (super.execute() == FAILURE) {
      return FAILURE;
    }
    if (!checkConfigurationPath(LOG, this.configurationFile)) {
      return FAILURE;
    }

    try (var client = WXMServices.clients().open(this.configurationFile)) {
      final var machine = client.vmFind(this.id);
      final var deviceId =
        machine.findUnusedDeviceId()
          .orElseThrow(() -> new WXMException(
            "No slots left to add a device to the virtual machine"
          ));

      final Map<String, WXMTTYBackendType> backendMap = new HashMap<>(3);
      for (final WXMTTYBackendType backend : this.backends) {
        final var device = backend.device();
        if (backendMap.put(device, backend) != null) {
          LOG.error("Backend device names must be unique: {}", device);
          return FAILURE;
        }
      }

      final var lpc =
        WXMDeviceLPC.builder()
          .setId(deviceId)
          .setBackends(backendMap)
          .build();

      final var updatedMachine =
        WXMVirtualMachine.builder()
          .from(machine)
          .addDevices(lpc)
          .build();

      client.vmUpdate(updatedMachine);

      LOG.info("Added lpc device @ slot {}", Integer.valueOf(lpc.id().value()));
      for (final var entry : backendMap.entrySet()) {
        final var device = entry.getValue();
        switch (device.kind()) {
          case WXM_FILE:
            final var fileDeviceBackend = (WXMTTYBackendFile) device;
            LOG.info(
              "Backend file {} {}",
              fileDeviceBackend.device(),
              fileDeviceBackend.path());
            break;
          case WXM_NMDM:
            final var nmdmBackend = (WXMTTYBackendNMDM) device;
            LOG.info(
              "Backend nmdm {} {}",
              nmdmBackend.device(),
              WXMTTYBackends.nmdmPath(machine.id(), NMDM_GUEST));
            break;
          case WXM_STDIO:
            final var stdioBackend = (WXMTTYBackendStdio) device;
            LOG.info("Backend stdio {}", stdioBackend.device());
            break;
        }
      }
    }
    return SUCCESS;
  }
}
