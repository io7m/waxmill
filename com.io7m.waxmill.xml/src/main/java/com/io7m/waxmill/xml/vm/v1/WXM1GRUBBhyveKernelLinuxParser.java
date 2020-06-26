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

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.waxmill.machines.WXMGRUBKernelLinux;

import java.nio.file.FileSystem;
import java.util.Map;
import java.util.Objects;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1GRUBBhyveKernelLinuxParser
  implements BTElementHandlerType<Object, WXMGRUBKernelLinux>
{
  private final WXMGRUBKernelLinux.Builder builder;
  private final FileSystem fileSystem;

  public WXM1GRUBBhyveKernelLinuxParser(
    final FileSystem inFileSystem)
  {
    this.fileSystem =
      Objects.requireNonNull(inFileSystem, "fileSystem");
    this.builder = WXMGRUBKernelLinux.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("LinuxKernelArgument"),
        c -> new WXM1LinuxKernelArgumentParser()
      ),
      Map.entry(
        element("LinuxKernelDevice"),
        c -> new WXM1LinuxKernelDeviceParser(this.fileSystem)
      ),
      Map.entry(
        element("LinuxInitRDDevice"),
        c -> new WXM1LinuxInitRDDeviceParser(this.fileSystem)
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof String) {
      this.builder.addKernelArguments((String) result);
    } else if (result instanceof WXM1LinuxInitRDDevice) {
      final var device = (WXM1LinuxInitRDDevice) result;
      this.builder.setInitRDDevice(device.deviceSlot());
      this.builder.setInitRDPath(device.initRDPath());
    } else if (result instanceof WXM1LinuxKernelDevice) {
      final var device = (WXM1LinuxKernelDevice) result;
      this.builder.setKernelDevice(device.deviceSlot());
      this.builder.setKernelPath(device.kernelPath());
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public WXMGRUBKernelLinux onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
