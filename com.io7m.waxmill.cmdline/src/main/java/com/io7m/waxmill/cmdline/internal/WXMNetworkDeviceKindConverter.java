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

import static com.io7m.waxmill.machines.WXMNetworkDeviceBackendType.Kind;
import static com.io7m.waxmill.machines.WXMNetworkDeviceBackendType.Kind.WXM_TAP;
import static com.io7m.waxmill.machines.WXMNetworkDeviceBackendType.Kind.WXM_VMNET;

/**
 * A converter for {@link Kind} values.
 */

public final class WXMNetworkDeviceKindConverter
  implements IStringConverter<Kind>
{
  /**
   * Construct a converter.
   */

  public WXMNetworkDeviceKindConverter()
  {

  }

  @Override
  public Kind convert(
    final String value)
  {
    switch (value) {
      case "tap": {
        return WXM_TAP;
      }
      case "vmnet": {
        return WXM_VMNET;
      }
      default: {
        throw new IllegalArgumentException(String.format(
          "Unrecognized network backend kind: %s",
          value));
      }
    }
  }
}
