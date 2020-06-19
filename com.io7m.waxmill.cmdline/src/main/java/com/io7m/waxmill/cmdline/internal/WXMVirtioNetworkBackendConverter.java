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

import com.beust.jcommander.IStringConverter;
import com.io7m.waxmill.machines.WXMMACAddress;
import com.io7m.waxmill.machines.WXMTAPDeviceName;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVMNetDeviceName;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMVirtioNetworkBackendType;
public final class WXMVirtioNetworkBackendConverter
  implements IStringConverter<WXMVirtioNetworkBackendType>
{
  private final WXMMessages messages;

  public WXMVirtioNetworkBackendConverter()
  {
    try {
      this.messages = WXMMessages.create();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public WXMVirtioNetworkBackendType convert(
    final String value)
  {
    final String[] segments = value.split(";");
    for (int index = 0; index < segments.length; ++index) {
      segments[index] = segments[index].trim();
    }

    if (segments.length < 3) {
      throw this.syntaxError(value);
    }

    switch (segments[0]) {
      case "tap": {
        return WXMTap.builder()
          .setName(WXMTAPDeviceName.of(segments[1]))
          .setAddress(WXMMACAddress.of(segments[2]))
          .build();
      }
      case "vmnet": {
        return WXMVMNet.builder()
          .setName(WXMVMNetDeviceName.of(segments[1]))
          .setAddress(WXMMACAddress.of(segments[2]))
          .build();
      }
      default:
        throw this.syntaxError(value);
    }
  }

  private IllegalArgumentException syntaxError(
    final String value)
  {
    throw new IllegalArgumentException(
      this.messages.format("errorInvalidVirtioNetworkBackend", value)
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMVirtioNetworkBackendConverter 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
