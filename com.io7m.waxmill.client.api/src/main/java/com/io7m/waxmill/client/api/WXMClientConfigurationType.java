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

package com.io7m.waxmill.client.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.waxmill.machines.WXMZFSFilesystem;
import com.io7m.waxmill.machines.WXMZFSFilesystems;
import org.immutables.value.Value;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuration values passed to the client.
 */

@ImmutablesStyleType
@Value.Immutable
public interface WXMClientConfigurationType
{
  /**
   * A directory that contains virtual machine configurations.
   *
   * @return The virtual machine configuration directory
   */

  Path virtualMachineConfigurationDirectory();

  /**
   * A ZFS filesystem that contains virtual machines. This directory will
   * contain one ZFS filesystem per virtual machine. Note that this value is
   * the name of the ZFS filesystem, and this doesn't necessarily match where
   * that filesystem is mounted! For example, {@code zfs create x/y/z && zfs set mountpoint=/a/b/c x/y/z}
   * will create a ZFS filesystem {@code x/y/z} and mount it at {@code /a/b/c};
   * the value expected here is {@code x/y/z}.
   *
   * @return The ZFS filesystem containing virtual machines
   */

  WXMZFSFilesystem virtualMachineRuntimeFilesystem();

  /**
   * @return The "bhyve" executable path, such as {@code /usr/sbin/bhyve}
   */

  @Value.Default
  default Path bhyveExecutable()
  {
    return this.virtualMachineConfigurationDirectory()
      .getFileSystem()
      .getPath("/usr/sbin/bhyve");
  }

  /**
   * @return The "bhyvectl" executable path, such as {@code /usr/sbin/bhyvectl}
   */

  @Value.Default
  default Path bhyveCtlExecutable()
  {
    return this.virtualMachineConfigurationDirectory()
      .getFileSystem()
      .getPath("/usr/sbin/bhyvectl");
  }

  /**
   * @return The "grub-bhyve" executable path, such as {@code /usr/local/sbin/grub-bhyve}
   */

  @Value.Default
  default Path grubBhyveExecutable()
  {
    return this.virtualMachineConfigurationDirectory()
      .getFileSystem()
      .getPath("/usr/local/sbin/grub-bhyve");
  }

  /**
   * @return The "zfs" executable path, such as {@code /sbin/zfs}
   */

  @Value.Default
  default Path zfsExecutable()
  {
    return this.virtualMachineConfigurationDirectory()
      .getFileSystem()
      .getPath("/sbin/zfs");
  }

  /**
   * @return The "ifconfig" executable path, such as {@code /sbin/ifconfig}
   */

  @Value.Default
  default Path ifconfigExecutable()
  {
    return this.virtualMachineConfigurationDirectory()
      .getFileSystem()
      .getPath("/sbin/ifconfig");
  }

  /**
   * @return The "cu" executable path, such as {@code /usr/bin/cu}
   */

  @Value.Default
  default Path cuExecutable()
  {
    return this.virtualMachineConfigurationDirectory()
      .getFileSystem()
      .getPath("/usr/bin/cu");
  }

  /**
   * Derive a runtime directory for a specific virtual machine.
   *
   * @param machineId The machine ID
   *
   * @return A runtime directory
   */

  default WXMZFSFilesystem virtualMachineRuntimeFilesystemFor(
    final UUID machineId)
  {
    Objects.requireNonNull(machineId, "machineId");

    return WXMZFSFilesystems.resolve(
      this.virtualMachineRuntimeFilesystem(), machineId.toString());
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.bhyveExecutable(),
      Path::isAbsolute,
      path -> "bhyve executable path must be absolute"
    );

    Preconditions.checkPrecondition(
      this.grubBhyveExecutable(),
      Path::isAbsolute,
      path -> "grub-bhyve executable path must be absolute"
    );

    Preconditions.checkPrecondition(
      this.zfsExecutable(),
      Path::isAbsolute,
      path -> "zfs executable path must be absolute"
    );

    Preconditions.checkPrecondition(
      this.ifconfigExecutable(),
      Path::isAbsolute,
      path -> "ifconfig executable path must be absolute"
    );

    Preconditions.checkPrecondition(
      this.virtualMachineConfigurationDirectory(),
      Path::isAbsolute,
      path -> "Virtual machine configuration directory must be absolute"
    );
  }
}

