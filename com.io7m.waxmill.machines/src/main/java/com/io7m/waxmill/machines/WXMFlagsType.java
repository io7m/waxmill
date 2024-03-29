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
import org.immutables.value.Value;

/**
 * The configuration flags associated with a virtual machine.
 */

@ImmutablesStyleType
@Value.Immutable
public interface WXMFlagsType
{
  /**
   * Include guest memory in core files.
   *
   * @return {@code true} if guest memory should appear in core files.
   *
   * @see "-C option to bhyve"
   */

  @Value.Default
  default boolean includeGuestMemoryInCoreFiles()
  {
    return false;
  }

  /**
   * Yield the virtual CPU thread when a HLT instruction is
   * detected.  If this option is not specified, virtual CPUs will
   * use 100% of a host CPU.
   *
   * @return {@code true} if the virtual CPU should yield on HLT
   *
   * @see "-H option to bhyve"
   */

  @Value.Default
  default boolean yieldCPUOnHLT()
  {
    return true;
  }

  /**
   * Force the guest virtual CPU to exit when a PAUSE instruction
   * is detected.
   *
   * @return {@code true} if the virtual CPU should exit on PAUSE
   *
   * @see "-P option to bhyve"
   */

  @Value.Default
  default boolean exitOnPAUSE()
  {
    return true;
  }

  /**
   * Generate ACPI tables.  Required for FreeBSD/amd64 guests.
   *
   * @return {@code true} if ACPI tables should be generated.
   *
   * @see "-A option to bhyve"
   */

  @Value.Default
  default boolean generateACPITables()
  {
    return true;
  }

  /**
   * Disable MP table generation.
   *
   * @return {@code true} if generation should be disabled
   *
   * @see "-Y option to bhyve"
   */

  @Value.Default
  default boolean disableMPTableGeneration()
  {
    return false;
  }

  /**
   * Force virtio PCI device emulations to use MSI interrupts
   * instead of MSI-X interrupts.
   *
   * @return {@code true} if MSI interrupts should be used
   *
   * @see "-W option to bhyve"
   */

  @Value.Default
  default boolean forceVirtualIOPCIToUseMSI()
  {
    return false;
  }

  /**
   * The guest's local APIC is configured in x2APIC mode.
   *
   * @return {@code true} if the guest's local APIC is configured in x2APIC mode.
   *
   * @see "-x option to bhyve"
   */

  @Value.Default
  default boolean guestAPICIsX2APIC()
  {
    return false;
  }

  /**
   * Wire guest memory.
   *
   * @return {@code true} if the guest's memory should be wired
   *
   * @see "-S option to bhyve"
   */

  @Value.Default
  default boolean wireGuestMemory()
  {
    return false;
  }

  /**
   * RTC keeps UTC time.
   *
   * @return {@code true} if the guest's RTC keeps UTC time.
   *
   * @see "-u option to bhyve"
   */

  @Value.Default
  default boolean realTimeClockIsUTC()
  {
    return false;
  }

  /**
   * Ignore accesses to unimplemented Model Specific Registers
   * (MSRs).  This is intended for debug purposes.
   *
   * @return {@code true} if unimplemented accesses should be ignored
   *
   * @see "-w option to bhyve"
   */

  @Value.Default
  default boolean ignoreUnimplementedModelSpecificRegisters()
  {
    return true;
  }
}
