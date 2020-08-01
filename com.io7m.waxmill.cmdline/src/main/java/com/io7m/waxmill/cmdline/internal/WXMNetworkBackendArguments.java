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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.machines.WXMInterfaceGroupName;
import com.io7m.waxmill.machines.WXMMACAddress;
import com.io7m.waxmill.machines.WXMNetworkDeviceBackendType;
import com.io7m.waxmill.machines.WXMTAPDeviceName;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVMNetDeviceName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class WXMNetworkBackendArguments
{
  private final WXMMessages messages;

  public WXMNetworkBackendArguments()
  {
    this.messages = WXMMessages.create();
  }

  public WXMNetworkDeviceBackendType parse(
    final WXMNamedParameter<WXMNetworkDeviceBackendType.Kind> type,
    final WXMNamedParameter<String> comment,
    final WXMNamedParameter<Optional<String>> deviceName,
    final WXMNamedParameter<Optional<WXMMACAddress>> hostMAC,
    final WXMNamedParameter<Optional<WXMMACAddress>> guestMAC,
    final WXMNamedParameter<List<WXMInterfaceGroupName>> groups)
  {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(comment, "comment");
    Objects.requireNonNull(deviceName, "deviceName");
    Objects.requireNonNull(hostMAC, "hostMAC");
    Objects.requireNonNull(guestMAC, "guestMAC");
    Objects.requireNonNull(groups, "groups");

    switch (type.value()) {
      case WXM_TAP:
        return this.parseTAP(
          type,
          comment,
          deviceName,
          hostMAC,
          guestMAC,
          groups
        );
      case WXM_VMNET:
        return this.parseVMNet(
          type,
          comment,
          deviceName,
          hostMAC,
          guestMAC,
          groups
        );
    }

    throw new UnreachableCodeException();
  }

  private WXMVMNet parseVMNet(
    final WXMNamedParameter<WXMNetworkDeviceBackendType.Kind> type,
    final WXMNamedParameter<String> comment,
    final WXMNamedParameter<Optional<String>> deviceName,
    final WXMNamedParameter<Optional<WXMMACAddress>> hostMAC,
    final WXMNamedParameter<Optional<WXMMACAddress>> guestMAC,
    final WXMNamedParameter<List<WXMInterfaceGroupName>> groups)
  {
    final WXMVMNetDeviceName vmNetName =
      WXMVMNetDeviceName.of(
        WXMNamedParameters.checkRequired(this.messages, type, deviceName)
      );
    final WXMMACAddress hMAC =
      WXMNamedParameters.checkRequired(this.messages, type, hostMAC);
    final WXMMACAddress gMAC =
      WXMNamedParameters.checkRequired(this.messages, type, guestMAC);

    return WXMVMNet.builder()
      .addAllGroups(groups.value())
      .setHostMAC(hMAC)
      .setGuestMAC(gMAC)
      .setName(vmNetName)
      .setComment(comment.value())
      .build();
  }

  private WXMTap parseTAP(
    final WXMNamedParameter<WXMNetworkDeviceBackendType.Kind> type,
    final WXMNamedParameter<String> comment,
    final WXMNamedParameter<Optional<String>> deviceName,
    final WXMNamedParameter<Optional<WXMMACAddress>> hostMAC,
    final WXMNamedParameter<Optional<WXMMACAddress>> guestMAC,
    final WXMNamedParameter<List<WXMInterfaceGroupName>> groups)
  {
    final WXMTAPDeviceName tapName =
      WXMTAPDeviceName.of(
        WXMNamedParameters.checkRequired(this.messages, type, deviceName)
      );
    final WXMMACAddress hMAC =
      WXMNamedParameters.checkRequired(this.messages, type, hostMAC);
    final WXMMACAddress gMAC =
      WXMNamedParameters.checkRequired(this.messages, type, guestMAC);

    return WXMTap.builder()
      .addAllGroups(groups.value())
      .setHostMAC(hMAC)
      .setGuestMAC(gMAC)
      .setName(tapName)
      .setComment(comment.value())
      .build();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMNetworkBackendArguments 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
