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
import com.io7m.waxmill.machines.WXMDeviceSlot;

import java.util.Optional;
import java.util.regex.Pattern;

public final class WXMDeviceSlotConverter
  implements IStringConverter<WXMDeviceSlot>
{
  private static final Pattern VALID_DEVICE_SLOT =
    Pattern.compile("([0-9]+):([0-9]+):([0-9]+)");

  private final WXMMessages messages;

  public WXMDeviceSlotConverter()
  {
    this.messages = WXMMessages.create();
  }

  @Override
  public WXMDeviceSlot convert(
    final String value)
  {
    final var matcher = VALID_DEVICE_SLOT.matcher(value);
    if (matcher.matches()) {
      try {
        return WXMDeviceSlot.builder()
          .setBusID(Integer.parseInt(matcher.group(1)))
          .setSlotID(Integer.parseInt(matcher.group(2)))
          .setFunctionID(Integer.parseInt(matcher.group(3)))
          .build();
      } catch (final Exception e) {
        throw this.syntaxError(Optional.of(e), value);
      }
    }

    throw this.syntaxError(Optional.empty(), value);
  }

  private IllegalArgumentException syntaxError(
    final Optional<Exception> exceptionOpt,
    final String value)
  {
    return exceptionOpt.map(
      ex -> new IllegalArgumentException(this.errorSyntaxMessage(value), ex))
      .orElseGet(() -> new IllegalArgumentException(this.errorSyntaxMessage(value)));
  }

  private String errorSyntaxMessage(
    final String value)
  {
    return this.messages.format("errorInvalidDeviceSlot", value);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMDeviceSlotConverter 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
