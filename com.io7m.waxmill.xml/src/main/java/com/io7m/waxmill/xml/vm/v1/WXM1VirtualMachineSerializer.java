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

package com.io7m.waxmill.xml.vm.v1;

import com.io7m.waxmill.machines.WXMCPUTopology;
import com.io7m.waxmill.machines.WXMDeviceAHCIDisk;
import com.io7m.waxmill.machines.WXMDeviceAHCIOpticalDisk;
import com.io7m.waxmill.machines.WXMDeviceHostBridge;
import com.io7m.waxmill.machines.WXMDeviceLPC;
import com.io7m.waxmill.machines.WXMDevicePassthru;
import com.io7m.waxmill.machines.WXMDeviceType;
import com.io7m.waxmill.machines.WXMDeviceType.WXMTTYBackendType;
import com.io7m.waxmill.machines.WXMDeviceVirtioBlockStorage;
import com.io7m.waxmill.machines.WXMDeviceVirtioNetwork;
import com.io7m.waxmill.machines.WXMFlags;
import com.io7m.waxmill.machines.WXMMemory;
import com.io7m.waxmill.machines.WXMPinCPU;
import com.io7m.waxmill.machines.WXMTTYBackendFile;
import com.io7m.waxmill.machines.WXMTTYBackendNMDM;
import com.io7m.waxmill.machines.WXMTTYBackendStdio;
import com.io7m.waxmill.machines.WXMTap;
import com.io7m.waxmill.machines.WXMVMNet;
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;
import com.io7m.waxmill.serializer.api.WXMSerializerType;
import com.io7m.waxmill.xml.WXMSchemas;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.io7m.waxmill.xml.vm.v1.WXM1DeviceSlots.SlotSide.GUEST;
import static com.io7m.waxmill.xml.vm.v1.WXM1DeviceSlots.SlotSide.HOST;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class WXM1VirtualMachineSerializer implements WXMSerializerType
{
  private static final byte[] XML_DECLARATION =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(UTF_8);

  private final XMLStreamWriter writer;
  private final ByteArrayOutputStream bufferedOutput;
  private final OutputStream stream;
  private final Transformer transformer;
  private final WXMVirtualMachineSet machineSet;

  public WXM1VirtualMachineSerializer(
    final XMLStreamWriter inWriter,
    final ByteArrayOutputStream inBufferedOutput,
    final OutputStream inStream,
    final Transformer inTransformer,
    final WXMVirtualMachineSet inValue)
  {
    this.writer =
      Objects.requireNonNull(inWriter, "writer");
    this.bufferedOutput =
      Objects.requireNonNull(inBufferedOutput, "inBufferedOutput");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
    this.transformer =
      Objects.requireNonNull(inTransformer, "transformer");
    this.machineSet =
      Objects.requireNonNull(inValue, "value");
  }

  @Override
  public void execute()
    throws IOException
  {
    try {
      this.start();

      final var machines = this.machineSet.machines();
      for (final var entry : machines.entrySet()) {
        this.serializeMachine(entry.getValue());
      }

      this.finish();
    } catch (final XMLStreamException e) {
      throw new IOException(e);
    }
  }

  private void serializeMachine(
    final WXMVirtualMachine machine)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "VirtualMachine");
    this.writer.writeAttribute("id", machine.id().toString());
    this.writer.writeAttribute("name", machine.name().value());

    WXM1Comments.serializeComment(machine.comment(), this.writer);
    this.serializeCPUTopology(machine.cpuTopology());
    this.serializeMemory(machine.memory());
    this.serializeDevices(machine.devices());
    WXM1BootConfigurations.serializeBootConfigurations(
      machine.bootConfigurations(),
      this.writer,
      false
    );
    this.serializeFlags(machine.flags());
    this.writer.writeEndElement();
  }

  private void serializeDevices(
    final List<WXMDeviceType> devices)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "Devices");

    final var iter =
      devices.stream()
        .sorted(Comparator.comparing(WXMDeviceType::deviceSlot))
        .iterator();

    while (iter.hasNext()) {
      final var device = iter.next();
      switch (device.kind()) {
        case WXM_HOSTBRIDGE:
          this.serializeDeviceHostBridge((WXMDeviceHostBridge) device);
          break;
        case WXM_VIRTIO_NETWORK:
          this.serializeDeviceVirtioNetwork((WXMDeviceVirtioNetwork) device);
          break;
        case WXM_VIRTIO_BLOCK:
          this.serializeDeviceVirtioBlockStorage(
            (WXMDeviceVirtioBlockStorage) device
          );
          break;
        case WXM_AHCI_HD:
          this.serializeDeviceAHCIDisk((WXMDeviceAHCIDisk) device);
          break;
        case WXM_AHCI_CD:
          this.serializeDeviceAHCIOpticalDisk((WXMDeviceAHCIOpticalDisk) device);
          break;
        case WXM_LPC:
          this.serializeDeviceLPC((WXMDeviceLPC) device);
          break;
        case WXM_PASSTHRU:
          this.serializeDevicePassthru((WXMDevicePassthru) device);
          break;
      }
    }

    this.writer.writeEndElement();
  }

  private void serializeDevicePassthru(
    final WXMDevicePassthru device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "PassthruDevice");
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);
    WXM1DeviceSlots.serializeDeviceSlot(device.hostPCISlot(), HOST, this.writer);
    this.writer.writeEndElement();
  }

  private void serializeDeviceLPC(
    final WXMDeviceLPC device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "LPCDevice");
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);

    final var iter =
      device.backends()
        .stream()
        .sorted(Comparator.comparing(WXMTTYBackendType::device))
        .iterator();

    while (iter.hasNext()) {
      final var backend = iter.next();
      switch (backend.kind()) {
        case WXM_FILE:
          this.serializeTTYFileBackend((WXMTTYBackendFile) backend);
          break;
        case WXM_NMDM:
          this.serializeTTYBackendNMDM((WXMTTYBackendNMDM) backend);
          break;
        case WXM_STDIO:
          this.serializeTTYBackendStdio((WXMTTYBackendStdio) backend);
          break;
      }
    }

    this.writer.writeEndElement();
  }

  private void serializeTTYBackendNMDM(
    final WXMTTYBackendNMDM backend)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "TTYBackendNMDM");
    this.writer.writeAttribute("device", backend.device());
    this.writer.writeEndElement();
  }

  private void serializeTTYBackendStdio(
    final WXMTTYBackendStdio backend)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "TTYBackendStdio");
    this.writer.writeAttribute("device", backend.device());
    this.writer.writeEndElement();
  }

  private void serializeTTYFileBackend(
    final WXMTTYBackendFile backend)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "TTYBackendFile");
    this.writer.writeAttribute("device", backend.device());
    this.writer.writeAttribute("path", backend.path().toString());
    this.writer.writeEndElement();
  }

  private void serializeDeviceAHCIOpticalDisk(
    final WXMDeviceAHCIOpticalDisk device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "AHCIOpticalDiskDevice");
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);
    this.writer.writeEndElement();
  }

  private void serializeDeviceAHCIDisk(
    final WXMDeviceAHCIDisk device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "AHCIDiskDevice");
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);

    WXM1StorageBackends.serializeStorageBackend(device.backend(), this.writer);

    this.writer.writeEndElement();
  }

  private void serializeDeviceVirtioNetwork(
    final WXMDeviceVirtioNetwork device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "VirtioNetworkDevice");
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);

    final var backend = device.backend();
    switch (backend.kind()) {
      case WXM_TAP:
        this.serializeTAP((WXMTap) backend);
        break;
      case WXM_VMNET:
        this.serializeVMNet((WXMVMNet) backend);
        break;
    }

    this.writer.writeEndElement();
  }

  private void serializeDeviceVirtioBlockStorage(
    final WXMDeviceVirtioBlockStorage device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "VirtioBlockStorageDevice");
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);
    WXM1StorageBackends.serializeStorageBackend(device.backend(), this.writer);
    this.writer.writeEndElement();
  }

  private void serializeVMNet(
    final WXMVMNet backend)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "VMNetDevice");
    this.writer.writeAttribute("name", backend.name().value());
    this.writer.writeAttribute("address", backend.address().value());
    WXM1Comments.serializeComment(backend.comment(), this.writer);
    this.writer.writeEndElement();
  }

  private void serializeTAP(
    final WXMTap backend)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "TAPDevice");
    this.writer.writeAttribute("name", backend.name().value());
    this.writer.writeAttribute("address", backend.address().value());
    WXM1Comments.serializeComment(backend.comment(), this.writer);
    this.writer.writeEndElement();
  }

  private void serializeDeviceHostBridge(
    final WXMDeviceHostBridge device)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "HostBridge");
    this.writer.writeAttribute("vendor", device.vendor().externalName());
    WXM1DeviceSlots.serializeDeviceSlot(device.deviceSlot(), GUEST, this.writer);
    WXM1Comments.serializeComment(device.comment(), this.writer);
    this.writer.writeEndElement();
  }

  private void serializeFlags(
    final WXMFlags flags)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "Flags");
    this.serializeFlag(
      "DisableMPTableGeneration",
      flags.disableMPTableGeneration());
    this.serializeFlag(
      "ForceVirtualIOPCIToUseMSI",
      flags.forceVirtualIOPCIToUseMSI());
    this.serializeFlag(
      "GenerateACPITables",
      flags.generateACPITables());
    this.serializeFlag(
      "GuestAPICIsX2APIC",
      flags.guestAPICIsX2APIC());
    this.serializeFlag(
      "IncludeGuestMemoryInCoreFiles",
      flags.includeGuestMemoryInCoreFiles());
    this.serializeFlag(
      "RealTimeClockIsUTC",
      flags.realTimeClockIsUTC());
    this.serializeFlag(
      "WireGuestMemory",
      flags.wireGuestMemory());
    this.serializeFlag(
      "ExitCPUOnPAUSE",
      flags.exitOnPAUSE());
    this.serializeFlag(
      "YieldCPUOnHLT",
      flags.yieldCPUOnHLT());
    this.writer.writeEndElement();
  }

  private void serializeFlag(
    final String name,
    final boolean value)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "Flag");
    this.writer.writeAttribute("name", name);
    this.writer.writeAttribute("enabled", String.valueOf(value));
    this.writer.writeEndElement();
  }

  private void serializeMemory(
    final WXMMemory memory)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "Memory");
    this.writer.writeAttribute(
      "gigabytes",
      memory.gigabytes().toString());
    this.writer.writeAttribute(
      "megabytes",
      memory.megabytes().toString());
    WXM1Comments.serializeComment(memory.comment(), this.writer);
    this.writer.writeEndElement();
  }

  private void serializeCPUTopology(
    final WXMCPUTopology cpuTopology)
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartElement(namespaceURI, "CPUTopology");
    this.writer.writeAttribute(
      "sockets",
      String.valueOf(cpuTopology.sockets()));
    this.writer.writeAttribute(
      "threads",
      String.valueOf(cpuTopology.threads()));
    this.writer.writeAttribute(
      "cores",
      String.valueOf(cpuTopology.cores()));
    WXM1Comments.serializeComment(cpuTopology.comment(), this.writer);
    this.serializePinCPUs(cpuTopology.pinnedCPUs());
    this.writer.writeEndElement();
  }

  private void serializePinCPUs(
    final List<WXMPinCPU> pinnedCPUs)
    throws XMLStreamException
  {
    if (!pinnedCPUs.isEmpty()) {
      final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
      this.writer.writeStartElement(namespaceURI, "PinCPUs");
      final var iter = pinnedCPUs.stream().sorted().iterator();
      while (iter.hasNext()) {
        final var pinCPU = iter.next();
        this.writer.writeStartElement(namespaceURI, "PinCPU");
        this.writer.writeAttribute(
          "host",
          String.valueOf(pinCPU.hostCPU()));
        this.writer.writeAttribute(
          "guest",
          String.valueOf(pinCPU.guestCPU()));
        this.writer.writeEndElement();
      }
      this.writer.writeEndElement();
    }
  }

  private void finish()
    throws XMLStreamException
  {
    this.writer.flush();
    this.writer.writeEndElement();
    this.writer.writeEndDocument();
  }

  private void start()
    throws XMLStreamException
  {
    final var namespaceURI = WXMSchemas.vmSchemaV1p0NamespaceText();
    this.writer.writeStartDocument("UTF-8", "1.0");
    this.writer.writeCharacters("\n");
    this.writer.setPrefix("wxm", namespaceURI);
    this.writer.writeStartElement(namespaceURI, "VirtualMachines");
    this.writer.writeNamespace("wxm", namespaceURI);
  }

  @Override
  public void close()
    throws IOException
  {
    try {
      this.writer.flush();

      final var source =
        new StreamSource(
          new ByteArrayInputStream(this.bufferedOutput.toByteArray())
        );
      final var result =
        new StreamResult(this.stream);

      this.stream.write(XML_DECLARATION);
      this.stream.write(System.lineSeparator().getBytes(UTF_8));
      this.transformer.transform(source, result);
      this.stream.flush();
      this.stream.close();
    } catch (final XMLStreamException | TransformerException e) {
      throw new IOException(e);
    }
  }
}
