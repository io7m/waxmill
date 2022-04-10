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
import com.io7m.junreachable.UnreachableCodeException;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.net.InetAddress;
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
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_E1000;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_FRAMEBUFFER;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_HOSTBRIDGE;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_LPC;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_PASSTHRU;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_VIRTIO_BLOCK;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_VIRTIO_NETWORK;
import static com.io7m.waxmill.machines.WXMDeviceType.Kind.WXM_XHCI_USB_TABLET;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType.Kind.WXM_STORAGE_FILE;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMStorageBackendType.Kind.WXM_STORAGE_ZFS_VOLUME;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType.Kind.WXM_FILE;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType.Kind.WXM_NMDM;
import static com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType.Kind.WXM_STDIO;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;

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
      case WXM_E1000:
      case WXM_FRAMEBUFFER:
      case WXM_HOSTBRIDGE:
      case WXM_LPC:
      case WXM_PASSTHRU:
      case WXM_VIRTIO_NETWORK:
      case WXM_XHCI_USB_TABLET:
        return false;
      case WXM_AHCI_CD:
      case WXM_AHCI_HD:
      case WXM_VIRTIO_BLOCK:
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
      case WXM_AHCI_CD:
      case WXM_AHCI_HD:
      case WXM_E1000:
      case WXM_FRAMEBUFFER:
      case WXM_HOSTBRIDGE:
      case WXM_PASSTHRU:
      case WXM_VIRTIO_BLOCK:
      case WXM_VIRTIO_NETWORK:
      case WXM_XHCI_USB_TABLET:
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

    WXM_LPC,

    /**
     * A PCI passthru device.
     */

    WXM_PASSTHRU,

    /**
     * An emulation of an Intel e82545 network device.
     */

    WXM_E1000,

    /**
     * A raw framebuffer device attached to a VNC server.
     */

    WXM_FRAMEBUFFER,

    /**
     * A USB tablet device which provides precise cursor synchronization when
     * using VNC.
     */

    WXM_XHCI_USB_TABLET
  }

  /**
   * The names of various TTY devices.
   */

  enum WXMLPCTTYNames
  {
    /**
     * "com1"
     */

    WXM_COM1("com1"),

    /**
     * "com2"
     */

    WXM_COM2("com2"),

    /**
     * "bootrom"
     */

    WXM_BOOTROM("bootrom");

    private final String deviceName;

    WXMLPCTTYNames(
      final String inDeviceName)
    {
      this.deviceName =
        Objects.requireNonNull(inDeviceName, "deviceName");
    }

    /**
     * @return The device name
     */

    public String deviceName()
    {
      return this.deviceName;
    }
  }

  /**
   * A PCI passthru device.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDevicePassthruType extends WXMDeviceType
  {
    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_PASSTHRU;
    }

    @Override
    WXMDeviceSlot deviceSlot();

    /**
     * @return The slot containing the host PCI devices
     */

    WXMDeviceSlot hostPCISlot();

    @Override
    default String externalName()
    {
      return "passthru";
    }
  }

  /**
   * A network device.
   */

  interface WXMDeviceNetworkType extends WXMDeviceType
  {
    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    WXMDeviceSlot deviceSlot();

    /**
     * @return The underlying device backend
     */

    WXMNetworkDeviceBackendType backend();
  }

  /**
   * An emulation of an Intel e82545 network device.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceE1000Type extends WXMDeviceNetworkType
  {
    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_E1000;
    }

    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    default String externalName()
    {
      return "e1000";
    }

    @Override
    WXMNetworkDeviceBackendType backend();
  }

  /**
   * A USB tablet device which provides precise cursor synchronization when
   * using VNC.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceXHCIUSBTabletType extends WXMDeviceType
  {
    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_XHCI_USB_TABLET;
    }

    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    default String externalName()
    {
      return "xhci,tablet";
    }
  }

  /**
   * A raw framebuffer device attached to a VNC server.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceFramebufferType extends WXMDeviceType
  {
    @Override
    @Value.Default
    default String comment()
    {
      return "";
    }

    @Override
    default Kind kind()
    {
      return WXM_FRAMEBUFFER;
    }

    @Override
    WXMDeviceSlot deviceSlot();

    @Override
    default String externalName()
    {
      return "fbuf";
    }

    /**
     * @return The IP address upon which the VNC server will listen
     */

    InetAddress listenAddress();

    /**
     * @return The port upon which the VNC server will listen
     */

    int listenPort();

    /**
     * @return The width of the framebuffer
     */

    @Value.Default
    default int width()
    {
      return 1024;
    }

    /**
     * @return The height of the framebuffer
     */

    @Value.Default
    default int height()
    {
      return 768;
    }

    /**
     * @return The guest VGA configuration used
     */

    @Value.Default
    default WXMVGAConfiguration vgaConfiguration()
    {
      return WXMVGAConfiguration.IO;
    }

    /**
     * @return Delay booting until a VNC connection has arrived
     */

    boolean waitForVNC();

    /**
     * The VGA configuration used.
     */

    enum WXMVGAConfiguration
    {
      /**
       * Used along with the CSM BIOS capability in UEFI to boot traditional
       * BIOS guests that require the legacy VGA I/O and memory regions to be
       * available.
       */

      ON,

      /**
       * Used for the UEFI guests that assume that VGA adapter is present if
       * they detect the I/O ports. An example of such a guest is OpenBSD in
       * UEFI mode.
       */

      OFF,

      /**
       * Used for guests that attempt to issue BIOS calls which result in I/O
       * port queries, and fail to boot if I/O decode is disabled.
       */

      IO;

      /**
       * @return The external name of the configuration
       */

      public String externalName()
      {
        switch (this) {
          case ON:
            return "on";
          case OFF:
            return "off";
          case IO:
            return "io";
        }
        throw new UnreachableCodeException();
      }
    }
  }

  /**
   * A host bridge device.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceHostBridgeType extends WXMDeviceType
  {
    @Override
    WXMDeviceSlot deviceSlot();

    /**
     * @return The host bridge vendor
     */

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

    /**
     * The host bridge vendor.
     */

    enum Vendor
    {
      /**
       * An unspecified vendor.
       */

      WXM_UNSPECIFIED,

      /**
       * AMD.
       */

      WXM_AMD;

      /**
       * @param vendor The name of the vendor
       *
       * @return The vendor of the external string
       *
       * @see #externalName()
       */

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

      /**
       * @return The external name of the vendor
       *
       * @see #ofExternalString(String)
       */

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

  /**
   * A Virtio network device.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMDeviceVirtioNetworkType extends WXMDeviceNetworkType
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

    @Override
    WXMNetworkDeviceBackendType backend();
  }

  /**
   * The storage backend.
   */

  interface WXMStorageBackendType
  {
    /**
     * @return The backend kind
     */

    Kind kind();

    /**
     * @return A descriptive comment
     */

    String comment();

    /**
     * The kind of storage backend.
     */

    enum Kind
    {
      /**
       * The backend is a file.
       */

      WXM_STORAGE_FILE,

      /**
       * The backend is a ZFS volume.
       */

      WXM_STORAGE_ZFS_VOLUME,

      /**
       * The backend is a SCSI device.
       */

      WXM_SCSI
    }
  }

  /**
   * The sector sizes for a device.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMSectorSizesType
  {
    /**
     * @return The logical sector size
     */

    BigInteger logical();

    /**
     * @return The physical sector size
     */

    @Value.Default
    default BigInteger physical()
    {
      return this.logical();
    }
  }

  /**
   * A file-based storage backend.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMStorageBackendFileType extends WXMStorageBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_STORAGE_FILE;
    }

    /**
     * @return The file path
     */

    Path file();

    /**
     * @return The file open options
     */

    Set<WXMOpenOption> options();

    /**
     * @return The sector size specification
     */

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

  /**
   * A ZFS volume-based storage backend.
   */

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

    /**
     * @return The expected size, in bytes, of the ZFS volume
     */

    Optional<BigInteger> expectedSize();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      this.expectedSize().ifPresent(size -> {
        Preconditions.checkPrecondition(
          size,
          Objects.equals(size.mod(valueOf(128000L)), ZERO),
          x -> "ZFS volume size must be a multiple of 128 kilobytes"
        );
      });
    }
  }

  /**
   * A Virtio block storage device.
   */

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

    /**
     * @return The storage backend
     */

    WXMStorageBackendType backend();
  }

  /**
   * An AHCI block storage device.
   */

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

    /**
     * @return The storage backend
     */

    WXMStorageBackendType backend();
  }

  /**
   * An AHCI optical storage device.
   */

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

  /**
   * The type of TTY backends.
   */

  interface WXMTTYBackendType
  {
    /**
     * @return The TTY backend kind
     */

    Kind kind();

    /**
     * @return The comment associated with the backend
     */

    String comment();

    /**
     * @return The device name
     */

    String device();

    /**
     * The TTY backend kind.
     */

    enum Kind
    {
      /**
       * A file-based TTY backend.
       */

      WXM_FILE,

      /**
       * An nmdm device backend.
       */

      WXM_NMDM,

      /**
       * A stdio backend.
       */

      WXM_STDIO
    }
  }

  /**
   * A file-based TTY backend.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface WXMTTYBackendFileType extends WXMTTYBackendType
  {
    @Override
    default Kind kind()
    {
      return WXM_FILE;
    }

    /**
     * @return The file path
     */

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

  /**
   * An nmdm TTY backend.
   */

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

  /**
   * A stdio TTY backend.
   */

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

  /**
   * An LPC device.
   */

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

    /**
     * @return The list of TTY backends
     */

    List<WXMTTYBackendType> backends();

    /**
     * @return The map of TTY backends
     */

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
