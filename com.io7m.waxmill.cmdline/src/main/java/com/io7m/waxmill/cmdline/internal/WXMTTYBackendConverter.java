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
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;

import java.nio.file.Path;
import java.util.Locale;

import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType;

/**
 * A converter of TTY values.
 */

public final class WXMTTYBackendConverter
  implements IStringConverter<WXMTTYBackendType>
{
  private final WXMMessages messages;

  /**
   * A converter of TTY values.
   */

  public WXMTTYBackendConverter()
  {
    this.messages = WXMMessages.create();
  }

  @Override
  public WXMTTYBackendType convert(
    final String value)
  {
    final String[] segments = value.split(";");
    for (int index = 0; index < segments.length; ++index) {
      segments[index] = segments[index].trim();
    }

    if (segments.length < 2) {
      throw this.syntaxError(value);
    }

    final var deviceName = segments[1].toLowerCase(Locale.ROOT);
    switch (segments[0].trim()) {
      case "stdio": {
        return this.convertStdio(value, deviceName);
      }
      case "nmdm": {
        return this.convertNmdm(value, deviceName);
      }
      case "file": {
        return this.convertFile(value, segments, deviceName);
      }
      default:
        throw this.syntaxError(value);
    }
  }

  private WXMTTYBackendType convertFile(
    final String value,
    final String[] segments,
    final String deviceName)
  {
    if (segments.length < 3) {
      throw this.syntaxError(value);
    }
    switch (deviceName) {
      case "com1":
      case "com2":
      case "bootrom":
        return WXMTTYBackendFile.builder()
          .setDevice(deviceName)
          .setPath(Path.of(segments[2]))
          .build();
      default:
        throw this.syntaxError(value);
    }
  }

  private WXMTTYBackendType convertNmdm(
    final String value,
    final String deviceName)
  {
    switch (deviceName) {
      case "com1":
      case "com2":
      case "bootrom":
        return WXMTTYBackendNMDM.builder()
          .setDevice(deviceName)
          .build();
      default:
        throw this.syntaxError(value);
    }
  }

  private WXMTTYBackendType convertStdio(
    final String value,
    final String deviceName)
  {
    switch (deviceName) {
      case "com1":
      case "com2":
      case "bootrom":
        return WXMTTYBackendStdio.builder()
          .setDevice(deviceName)
          .build();
      default:
        throw this.syntaxError(value);
    }
  }

  private IllegalArgumentException syntaxError(
    final String value)
  {
    return new IllegalArgumentException(
      this.messages.format(
        "errorInvalidTTYBackend",
        this.messages.format("ttyBackendSpec"),
        value
      )
    );
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMTTYBackendConverter 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
