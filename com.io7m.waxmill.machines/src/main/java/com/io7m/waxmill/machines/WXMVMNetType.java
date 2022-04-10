/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import java.util.List;
import java.util.Objects;

import static com.io7m.waxmill.machines.WXMNetworkDeviceBackendType.Kind.WXM_VMNET;

/**
 * A vmnet device.
 */

@ImmutablesStyleType
@Value.Immutable
public interface WXMVMNetType extends WXMNetworkDeviceBackendType
{
  @Override
  default Kind kind()
  {
    return WXM_VMNET;
  }

  /**
   * @return The underlying device name
   */

  WXMVMNetDeviceName name();

  @Override
  WXMMACAddress guestMAC();

  @Override
  WXMMACAddress hostMAC();

  @Override
  List<WXMInterfaceGroupName> groups();

  @Override
  @Value.Default
  default String comment()
  {
    return "";
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      !Objects.equals(this.guestMAC(), this.hostMAC()),
      "Host and guest MAC addresses must differ"
    );
  }
}
