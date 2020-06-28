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
import java.util.Optional;

import static com.io7m.waxmill.machines.WXMBootConfigurationType.Kind.GRUB_BHYVE;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMGRUBKernelInstructionsType.Kind.KERNEL_LINUX;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMGRUBKernelInstructionsType.Kind.KERNEL_OPENBSD;

public interface WXMBootConfigurationType
{
  Kind kind();

  WXMBootConfigurationName name();

  default String comment()
  {
    return "";
  }

  enum Kind
  {
    GRUB_BHYVE
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
    Kind kind();

    enum Kind
    {
      KERNEL_OPENBSD,
      KERNEL_LINUX
    }
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

    WXMDeviceSlot bootDevice();

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

    WXMDeviceSlot kernelDevice();

    Path kernelPath();

    List<String> kernelArguments();

    WXMDeviceSlot initRDDevice();

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

    List<Path> requiredPaths();

    WXMEvaluatedBootCommands commands();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMEvaluatedBootCommandsType
  {
    List<WXMCommandExecution> configurationCommands();

    Optional<WXMCommandExecution> lastExecution();
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

    @Override
    List<Path> requiredPaths();

    @Override
    WXMEvaluatedBootCommands commands();

    Path deviceMapFile();

    List<String> deviceMap();

    Path grubConfigurationFile();

    List<String> grubConfiguration();
  }
}
