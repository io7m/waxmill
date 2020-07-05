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
import com.io7m.junreachable.UnreachableCodeException;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_AHCI_CD;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_AHCI_HD;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_HOSTBRIDGE;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_LPC;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_VIRTIO_BLOCK;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_VIRTIO_NETWORK;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMVirtioNetworkBackendType.Kind.WXM_TAP;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMDeviceVirtioNetworkType.WXMVirtioNetworkBackendType.Kind.WXM_VMNET;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType.Kind.WXM_STORAGE_FILE;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType.Kind.WXM_STORAGE_ZFS_VOLUME;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType.Kind.WXM_FILE;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType.Kind.WXM_NMDM;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType.Kind.WXM_STDIO;

/**
 * The type of devices that can be attached to virtual machines.
 */

public interface WXMDeviceType
{
  /**
   * @return The device slot
   */

  WXMDeviceSlot deviceSlot();

  /**
   * @return The device kind
   */

  Kind kind();

  /**
   * @return The external name of the type (such as {@code ahci-cd}).
   */

  String externalName();

  /**
   * @return A descriptive comment
   */

  String comment();

  /**
   * @return {@code true} if this device is a storage device
   */

  default boolean isStorageDevice()
  {
    switch (this.kind()) {
      case WXM_HOSTBRIDGE:
      case WXM_VIRTIO_NETWORK:
      case WXM_LPC:
        return false;
      case WXM_VIRTIO_BLOCK:
      case WXM_AHCI_HD:
      case WXM_AHCI_CD:
        return true;
    }

    throw new UnreachableCodeException();
  }

  /**
   * @return {@code true} if this device is a console/tty device
   */

  default boolean isConsoleDevice()
  {
    switch (this.kind()) {
      case WXM_HOSTBRIDGE:
      case WXM_VIRTIO_NETWORK:
      case WXM_VIRTIO_BLOCK:
      case WXM_AHCI_HD:
      case WXM_AHCI_CD:
        return false;
      case WXM_LPC:
        return true;
    }

    throw new UnreachableCodeException();
  }

  /**
   * The device kind
   */

  enum Kind
  {
    /**
     * A host bridge.
     */

    WXM_HOSTBRIDGE,

    /**
     * A virtio network device.
     */

    WXM_VIRTIO_NETWORK,

    /**
     * A virtio block storage device.
     */

    WXM_VIRTIO_BLOCK,

    /**
     * An AHCI disk device.
     */

    WXM_AHCI_HD,

    /**
     * An AHCI optical disk device.
     */

    WXM_AHCI_CD,

    /**
     * An LPC PCI-ISA bridge with COM1 and COM2 16550 serial ports and a boot ROM.
     */

    WXM_LPC
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceHostBridgeType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    Vendor vendor();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_HOSTBRIDGE;
    }

    @Override
    default String externalName()
    {
      switch (this.vendor()) {
        case WXM_AMD:
          return "amd_hostbridge";
        case WXM_UNSPECIFIED:
          return "hostbridge";
        default:
          throw new UnreachableCodeException();
      }
    }

    enum Vendor
    {
      WXM_UNSPECIFIED,
      WXM_AMD;

      public static Vendor ofExternalString(
        final String vendor)
      {
        switch (vendor.toUpperCase(Locale.ROOT)) {
          case "UNSPECIFIED":
            return WXM_UNSPECIFIED;
          case "AMD":
            return WXM_AMD;
          default:
            throw new IllegalArgumentException(
              String.format("Unrecognized vendor: %s", vendor)
            );
        }
      }

      public String externalName()
      {
        switch (this) {
          case WXM_UNSPECIFIED:
            return "UNSPECIFIED";
          case WXM_AMD:
            return "AMD";
          default:
            throw new UnreachableCodeException();
        }
      }
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceVirtioNetworkType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    default Kind kind()
    {
      return WXM_VIRTIO_NETWORK;
    }

    @Override
    default String externalName()
    {
      return "virtio-net";
    }

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    WXMVirtioNetworkBackendType backend();

    interface WXMVirtioNetworkBackendType
    {
      Kind kind();

      String comment();

      enum Kind
      {
        WXM_TAP,
        WXM_VMNET
      }
    }

    @ImmutablesStyleType
    @Value.Immutable
    interface WXMTapType extends WXMVirtioNetworkBackendType
    {
      @Override
      default Kind kind()
      {
        return WXM_TAP;
      }

      WXMTAPDeviceName name();

      WXMMACAddress address();

      @Override
      @Value.Default
      default String comment()
      {
        return "";
      }
    }

    @ImmutablesStyleType
    @Value.Immutable
    interface WXMVMNetType extends WXMVirtioNetworkBackendType
    {
      @Override
      default Kind kind()
      {
        return WXM_VMNET;
      }

      WXMVMNetDeviceName name();

      WXMMACAddress address();

      @Override
      @Value.Default
      default String comment()
      {
        return "";
      }
    }
  }

  interface WXMStorageBackendType
  {
    Kind kind();

    /**
     * @return A descriptive comment
     */

    String comment();

    enum Kind
    {
      WXM_STORAGE_FILE,
      WXM_STORAGE_ZFS_VOLUME,
      WXM_SCSI
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMSectorSizesType
  {
    BigInteger logical();

    @Value.Default
    default BigInteger physical()
    {
      return this.logical();
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMStorageBackendFileType extends WXMStorageBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_STORAGE_FILE;
    }

    Path file();

    Set<WXMOpenOption> options();

    Optional<WXMSectorSizes> sectorSizes();

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
        this.file(),
        Path::isAbsolute,
        path -> "Storage backend path must be absolute"
      );
    }

  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMStorageBackendZFSVolumeType extends WXMStorageBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_STORAGE_ZFS_VOLUME;
    }

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceVirtioBlockStorageType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_VIRTIO_BLOCK;
    }

    @Override
    default String externalName()
    {
      return "virtio-blk";
    }

    WXMStorageBackendType backend();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceAHCIDiskType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_AHCI_HD;
    }

    @Override
    default String externalName()
    {
      return "ahci-hd";
    }

    WXMStorageBackendType backend();
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceAHCIOpticalDiskType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_AHCI_CD;
    }

    @Override
    default String externalName()
    {
      return "ahci-cd";
    }
  }

  interface WXMTTYBackendType
  {
    Kind kind();

    String comment();

    String device();

    enum Kind
    {
      WXM_FILE,
      WXM_NMDM,
      WXM_STDIO
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMTTYBackendFileType extends WXMTTYBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_FILE;
    }

    Path path();

    @Override
    String device();

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
        this.path(),
        Path::isAbsolute,
        q -> "TTY backend path must be absolute"
      );
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMTTYBackendNMDMType extends WXMTTYBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_NMDM;
    }

    @Override
    String device();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMTTYBackendStdioType extends WXMTTYBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_STDIO;
    }

    @Override
    String device();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }
  }

  enum WXMLPCTTYNames
  {
    WXM_COM1("com1"),
    WXM_COM2("com2"),
    WXM_BOOTROM("bootrom");

    private final String deviceName;

    public String deviceName()
    {
      return this.deviceName;
    }

    WXMLPCTTYNames(
      final String inDeviceName)
    {
      this.deviceName =
        Objects.requireNonNull(inDeviceName, "deviceName");
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceLPCType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_LPC;
    }

    @Override
    default String externalName()
    {
      return "lpc";
    }

    List<WXMTTYBackendType> backends();

    @Value.Derived
    @Value.Auxiliary
    default Map<String, WXMTTYBackendType> backendMap()
    {
      return this.backends()
        .stream()
        .collect(Collectors.toMap(
          WXMTTYBackendType::device,
          Function.identity()
        ));
    }

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      Preconditions.checkPrecondition(
        !this.backends().isEmpty(),
        "At least one LPC TTY backend must be provided."
      );

      final var busID = this.deviceSlot().busID();
      Preconditions.checkPreconditionI(
        busID,
        busID == 0,
        bus -> "LPC devices may only be configured on bus 0"
      );
    }
  }
}
