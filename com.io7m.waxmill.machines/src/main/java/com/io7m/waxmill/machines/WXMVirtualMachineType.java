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

package com.io7m.waxmill.machines;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import org.immutables.value.Value;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMDeviceIDType.VALID_DEVICE_IDS;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_HOSTBRIDGE;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_LPC;

@Value.Immutable
@ImmutablesStyleType
public interface WXMVirtualMachineType
{
  /**
   * @return The machine ID
   */

  UUID id();

  /**
   * @return The machine name
   */

  WXMMachineName name();

  /**
   * @return A descriptive comment
   */

  @Value.Default
  default String comment()
  {
    return "";
  }

  /**
   * @return The CPU topology
   */

  @Value.Default
  default WXMCPUTopology cpuTopology()
  {
    return WXMCPUTopology.builder()
      .build();
  }

  /**
   * @return The machine's memory configuration
   */

  @Value.Default
  default WXMMemory memory()
  {
    return WXMMemory.builder()
      .build();
  }

  /**
   * @return The list of devices attached to the machine
   */

  List<WXMDeviceType> devices();

  /**
   * @return The machine flags
   */

  @Value.Default
  default WXMFlags flags()
  {
    return WXMFlags.builder()
      .build();
  }

  /**
   * @return The list of boot configurations associated with the machine
   */

  List<WXMBootConfigurationType> bootConfigurations();

  /**
   * @return The configuration file path, if the VM was configured from a file
   */

  Optional<URI> configurationFile();

  @Value.Derived
  @Value.Auxiliary
  default Map<WXMDeviceID, WXMDeviceType> deviceMap()
  {
    return this.devices()
      .stream()
      .collect(Collectors.toMap(
        WXMDeviceType::id,
        Function.identity()
      ));
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<WXMBootConfigurationName, WXMBootConfigurationType> bootConfigurationMap()
  {
    return this.bootConfigurations()
      .stream()
      .collect(Collectors.toMap(
        WXMBootConfigurationType::name,
        Function.identity()
      ));
  }

  /**
   * @return An unused device ID, if one exists
   */

  default Optional<WXMDeviceID> findUnusedDeviceId()
  {
    final var devicesMap = this.deviceMap();
    for (int deviceId = VALID_DEVICE_IDS.lower();
         deviceId <= VALID_DEVICE_IDS.upper();
         ++deviceId) {
      final var possibleDeviceId = WXMDeviceID.of(deviceId);
      if (!devicesMap.containsKey(possibleDeviceId)) {
        return Optional.of(possibleDeviceId);
      }
    }
    return Optional.empty();
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    this.checkAtMostOneLPC();
    this.checkAtMostOneHostBridge();
  }

  private void checkAtMostOneLPC()
  {
    final var lpcDevices =
      this.devices().stream()
        .filter(device -> device.kind() == WXM_LPC)
        .collect(Collectors.toList());

    final var lpcCount = lpcDevices.size();
    Preconditions.checkPreconditionI(
      lpcCount,
      lpcCount <= 1,
      count -> "At most 1 LPC device can be added to a virtual machine"
    );
  }

  private void checkAtMostOneHostBridge()
  {
    final var hbDevices =
      this.devices().stream()
        .filter(device -> device.kind() == WXM_HOSTBRIDGE)
        .collect(Collectors.toList());

    final var hbCount = hbDevices.size();
    Preconditions.checkPreconditionI(
      hbCount,
      hbCount <= 1,
      count -> "At most 1 host bridge device can be added to a virtual machine"
    );
  }
}
