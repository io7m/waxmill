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

package com.io7m.waxmill.boot.internal;

import com.io7m.waxmill.machines.WXMDeviceID;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

public final class WXMGRUBDeviceMap
{
  private final SortedMap<Integer, WXMGRUBDeviceAndPath> disks;
  private final SortedMap<Integer, WXMGRUBDeviceAndPath> cds;

  public WXMGRUBDeviceMap(
    final SortedMap<Integer, WXMGRUBDeviceAndPath> inDisks,
    final SortedMap<Integer, WXMGRUBDeviceAndPath> inCds)
  {
    this.disks =
      Objects.requireNonNull(inDisks, "disks");
    this.cds =
      Objects.requireNonNull(inCds, "cds");
  }

  public Optional<Integer> searchForCD(
    final WXMDeviceID deviceID)
  {
    return this.cds.values()
      .stream()
      .filter(dp -> Objects.equals(dp.device().id(), deviceID))
      .map(dp -> Integer.valueOf(dp.index()))
      .findFirst();
  }

  public Optional<Integer> searchForHD(
    final WXMDeviceID deviceID)
  {
    return this.disks.values()
      .stream()
      .filter(dp -> Objects.equals(dp.device().id(), deviceID))
      .map(dp -> Integer.valueOf(dp.index()))
      .findFirst();
  }

  public Iterable<String> serialize()
  {
    final var lines =
      new ArrayList<String>(this.disks.size() + this.cds.size());

    for (final var entry : this.disks.entrySet()) {
      final var hd = entry.getValue();
      lines.add(String.format("(hd%d) %s", entry.getKey(), hd.path()));
    }
    for (final var entry : this.cds.entrySet()) {
      final var cd = entry.getValue();
      lines.add(String.format("(cd%d) %s", entry.getKey(), cd.path()));
    }
    return List.copyOf(lines);
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMGRUBDeviceMap 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
