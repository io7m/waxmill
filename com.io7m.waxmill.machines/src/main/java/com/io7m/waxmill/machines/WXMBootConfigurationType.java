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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMBootConfigurationType.Kind.GRUB_BHYVE;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.Kind.UEFI;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMGRUBKernelInstructionsType.Kind.KERNEL_LINUX;
import static com.io7m.waxmill.machines.WXMBootConfigurationType.WXMGRUBKernelInstructionsType.Kind.KERNEL_OPENBSD;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType;

/**
 * A boot configuration.
 */

public interface WXMBootConfigurationType
{
  /**
   * @return The kind of boot configuration
   */

  Kind kind();

  /**
   * The unique-within-a-virtual-machine name of the boot configuration.
   *
   * @return The name of the boot configuration
   */

  WXMBootConfigurationName name();

  /**
   * @return A comment
   */

  default String comment()
  {
    return "";
  }

  /**
   * @return The set of devices required by the boot configuration
   */

  Set<WXMDeviceSlot> requiredDevices();

  /**
   * @return The disks attached to the virtual machine on boot
   */

  List<WXMBootDiskAttachment> diskAttachments();

  /**
   * @return The disk attachments by device slot
   */

  @Value.Derived
  @Value.Auxiliary
  default Map<WXMDeviceSlot, WXMBootDiskAttachment> diskAttachmentMap()
  {
    return this.diskAttachments()
      .stream()
      .collect(Collectors.toMap(
        WXMBootDiskAttachment::device,
        Function.identity()
      ));
  }

  /**
   * The kind of boot configurations.
   */

  enum Kind
  {
    /**
     * The boot configuration uses grub-bhyve.
     */

    GRUB_BHYVE,

    /**
     * The boot configuration uses UEFI.
     */

    UEFI
  }

  /**
   * A disk attachment.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMBootDiskAttachmentType
  {
    /**
     * @return The disk device slot to which the storage will be attached
     */

    WXMDeviceSlot device();

    /**
     * @return The storage backend
     */

    WXMStorageBackendType backend();
  }

  /**
   * A boot configuration that uses UEFI.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMBootConfigurationUEFIType extends WXMBootConfigurationType
  {
    @Override
    default Kind kind()
    {
      return UEFI;
    }

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    WXMBootConfigurationName name();

    /**
     * @return The location of the UEFI firmware to load into the machine
     */

    Path firmware();

    @Override
    default Set<WXMDeviceSlot> requiredDevices()
    {
      return Set.of();
    }

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      Preconditions.checkPrecondition(
        this.firmware(),
        Path::isAbsolute,
        q -> "Firmware path must be absolute"
      );
    }
  }

  /**
   * A boot configuration that uses grub-bhyve.
   */

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

    /**
     * @return The underlying kernel instructions
     */

    WXMGRUBKernelInstructionsType kernelInstructions();

    @Override
    default Set<WXMDeviceSlot> requiredDevices()
    {
      return this.kernelInstructions().requiredDevices();
    }
  }

  /**
   * Operating system specific GRUB boot instructions.
   */

  interface WXMGRUBKernelInstructionsType
  {
    /**
     * @return The kind of underlying kernel
     */

    Kind kind();

    /**
     * The kind of kernels.
     */

    enum Kind
    {
      /**
       * The kernel is OpenBSD.
       */

      KERNEL_OPENBSD,

      /**
       * The kernel is Linux.
       */

      KERNEL_LINUX
    }

    /**
     * @return The slots required to have assigned storage
     */

    Set<WXMDeviceSlot> requiredDevices();
  }

  /**
   * OpenBSD kernel instructions.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMGRUBKernelOpenBSDType extends WXMGRUBKernelInstructionsType
  {
    @Override
    default Kind kind()
    {
      return KERNEL_OPENBSD;
    }

    /**
     * @return The device from which booting will occur
     */

    WXMDeviceSlot bootDevice();

    /**
     * @return The path to the kernel on the guest filesystem
     */

    Path kernelPath();

    @Override
    default Set<WXMDeviceSlot> requiredDevices()
    {
      return Set.of(this.bootDevice());
    }

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

  /**
   * Linux kernel instructions.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMGRUBKernelLinuxType extends WXMGRUBKernelInstructionsType
  {
    @Override
    default Kind kind()
    {
      return KERNEL_LINUX;
    }

    /**
     * @return The device that contains the kernel
     */

    WXMDeviceSlot kernelDevice();

    /**
     * @return The path to the kernel on the guest filesystem
     */

    Path kernelPath();

    /**
     * @return The arguments for the kernel
     */

    List<String> kernelArguments();

    /**
     * @return The device that contains the initial ramdisk
     */

    WXMDeviceSlot initRDDevice();

    /**
     * @return The path to the initial ramdisk on the guest filesystem
     */

    Path initRDPath();

    @Override
    default Set<WXMDeviceSlot> requiredDevices()
    {
      final var devices = new HashSet<WXMDeviceSlot>();
      devices.add(this.kernelDevice());
      devices.add(this.initRDDevice());
      return Set.copyOf(devices);
    }

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

  /**
   * The type of evaluated boot configurations.
   */

  interface WXMEvaluatedBootConfigurationType
  {
    /**
     * @return The kind of underlying kernel
     */

    Kind kind();

    /**
     * @return The files required to exist by the boot configuration
     */

    List<Path> requiredPaths();

    /**
     * @return The nmdm files required to exist by the boot configuration
     */

    Set<Path> requiredNMDMs();

    /**
     * @return The set of commands required to bring up the virtual machine
     */

    WXMEvaluatedBootCommands commands();
  }

  /**
   * A set of boot commands.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMEvaluatedBootCommandsType
  {
    /**
     * @return A list of commands executed in declaration order to configure the virtual machine
     */

    List<WXMCommandExecution> configurationCommands();

    /**
     * The final execution to bring up the virtual machine. This is typically
     * executed using the operating system's equivalent of {@code execve()} and
     * will therefore replace the current process.
     *
     * @return The final execution used to bring up the virtual machine
     */

    Optional<WXMCommandExecution> lastExecution();
  }

  /**
   * A set of evaluated commands used to boot with grub-bhyve.
   */

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
    Set<Path> requiredNMDMs();

    @Override
    WXMEvaluatedBootCommands commands();

    /**
     * @return A file that will contain a GRUB device map
     */

    Path deviceMapFile();

    /**
     * @return The lines that will be written to the GRUB device map
     */

    List<String> deviceMap();

    /**
     * @return A file that will contain a GRUB configuration
     */

    Path grubConfigurationFile();

    /**
     * @return The lines that will be written to the GRUB configuration file
     */

    List<String> grubConfiguration();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      Preconditions.checkPrecondition(
        this.requiredPaths(),
        this.requiredPaths().stream().allMatch(Path::isAbsolute),
        q -> "All required paths must be absolute"
      );
      Preconditions.checkPrecondition(
        this.requiredNMDMs(),
        this.requiredPaths().containsAll(this.requiredNMDMs()),
        q -> "All NMDM paths must be required"
      );
    }
  }

  /**
   * A set of evaluated commands used to boot with UEFI.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMEvaluatedBootConfigurationUEFIType
    extends WXMEvaluatedBootConfigurationType
  {
    @Override
    default Kind kind()
    {
      return UEFI;
    }

    @Override
    List<Path> requiredPaths();

    @Override
    Set<Path> requiredNMDMs();

    @Override
    WXMEvaluatedBootCommands commands();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      Preconditions.checkPrecondition(
        this.requiredPaths(),
        this.requiredPaths().stream().allMatch(Path::isAbsolute),
        q -> "All required paths must be absolute"
      );
      Preconditions.checkPrecondition(
        this.requiredNMDMs(),
        this.requiredPaths().containsAll(this.requiredNMDMs()),
        q -> "All NMDM paths must be required"
      );
    }
  }
}
