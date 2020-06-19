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

import java.nio.file.Path;
import java.util.List;

import static com.io7m.waxmill.machines.WXMBootConfigurationType.Kind.GRUB_BHYVE;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMGRUBKernelInstructionsType.Kind.KERNEL_LINUX;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMGRUBKernelInstructionsType.Kind.KERNEL_OPENBSD;

public interface WXMBootConfigurationType
{
  enum Kind
  {
    GRUB_BHYVE
  }

  Kind kind();

  WXMBootConfigurationName name();

  default String comment()
  {
    return "";
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMBootConfigurationGRUBBhyveType extends WXMBootConfigurationType
  {
    @Override
    default Kind kind()
    {
      return GRUB_BHYVE;
    }

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    WXMBootConfigurationName name();

    WXMGRUBKernelInstructionsType kernelInstructions();
  }

  interface WXMGRUBKernelInstructionsType
  {
    enum Kind
    {
      KERNEL_OPENBSD,
      KERNEL_LINUX
    }

    Kind kind();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMGRUBKernelOpenBSDType extends WXMGRUBKernelInstructionsType
  {
    @Override
    default Kind kind()
    {
      return KERNEL_OPENBSD;
    }

    WXMDeviceID bootDevice();

    Path kernelPath();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      Preconditions.checkPrecondition(
        this.kernelPath(),
        Path::isAbsolute,
        q -> "Kernel path must be absolute"
      );
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMGRUBKernelLinuxType extends WXMGRUBKernelInstructionsType
  {
    @Override
    default Kind kind()
    {
      return KERNEL_LINUX;
    }

    WXMDeviceID kernelDevice();

    Path kernelPath();

    List<String> kernelArguments();

    WXMDeviceID initRDDevice();

    Path initRDPath();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      Preconditions.checkPrecondition(
        this.kernelPath(),
        Path::isAbsolute,
        q -> "Kernel path must be absolute"
      );
      Preconditions.checkPrecondition(
        this.initRDPath(),
        Path::isAbsolute,
        q -> "initrd path must be absolute"
      );
    }
  }

  interface WXMEvaluatedBootConfigurationType
  {
    Kind kind();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMEvaluatedBootConfigurationGRUBBhyveType
    extends WXMEvaluatedBootConfigurationType
  {
    @Override
    default Kind kind()
    {
      return GRUB_BHYVE;
    }

    List<String> deviceMap();

    List<String> grubConfiguration();
  }
}
