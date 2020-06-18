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

package com.io7m.waxmill.client.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import org.immutables.value.Value;

import java.util.Comparator;
import java.util.List;

/**
 * The CPU topology for the virtual machine.  The
 * default value for each of cpus, sockets, cores, and
 * threads is 1. The current maximum number of guest virtual
 * CPUs is 16. If cpus is not specified then it will be
 * calculated from the other arguments. The topology must be
 * consistent in that the cpus must equal the product of
 * sockets, cores, and threads.  If a setting is specified more
 * than once the last one has precedence.
 */

@Value.Immutable
@ImmutablesStyleType
public interface WXMCPUTopologyType
{
  /**
   * @return The number of sockets
   */

  @Value.Default
  default int sockets()
  {
    return 1;
  }

  /**
   * @return The number of threads per core
   */

  @Value.Default
  default int threads()
  {
    return 1;
  }

  /**
   * @return The number of cores per socket
   */

  @Value.Default
  default int cores()
  {
    return 1;
  }

  /**
   * @return The total number of CPU elements
   */

  @Value.Default
  default int cpus()
  {
    return this.sockets() * this.threads() * this.cores();
  }

  /**
   * @return A descriptive comment
   */

  @Value.Default
  default String comment()
  {
    return "";
  }

  /**
   * @return The list of CPU pinning instructions
   */

  List<WXMPinCPU> pinnedCPUs();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPreconditionI(
      this.cpus(),
      x -> x == this.sockets() * this.threads() * this.cores(),
      value -> String.format(
        "Number of CPUs must equal (Sockets) %d * (Threads) %d * (Cores) %d",
        Integer.valueOf(this.sockets()),
        Integer.valueOf(this.threads()),
        Integer.valueOf(this.cores())
      )
    );
  }

  /**
   * A description of a CPU pin. The given guest CPU is pinned to the given
   * host CPU.
   */

  @Value.Immutable
  @ImmutablesStyleType
  interface WXMPinCPUType extends Comparable<WXMPinCPUType>
  {
    /**
     * @return The index of the host CPU
     */

    int hostCPU();

    /**
     * @return The index of the guest CPU
     */

    int guestCPU();

    @Override
    default int compareTo(
      final WXMPinCPUType other)
    {
      return Comparator.comparingInt(WXMPinCPUType::hostCPU)
        .thenComparingInt(WXMPinCPUType::guestCPU)
        .compare(this, other);
    }
  }
}
