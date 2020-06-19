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
import com.io7m.waxmill.machines.WXMVirtualMachine;
import com.io7m.waxmill.machines.WXMVirtualMachineSet;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1VirtualMachineSetParser
  implements BTElementHandlerType<Object, WXMVirtualMachineSet>
{
  private final WXMVirtualMachineSet.Builder builder;
  private final TreeMap<UUID, WXMVirtualMachine> machines;
  private final URI sourceURI;

  public WXM1VirtualMachineSetParser(
    final URI inSourceURI)
  {
    this.sourceURI =
      Objects.requireNonNull(inSourceURI, "sourceURI");
    this.builder = WXMVirtualMachineSet.builder();
    this.machines = new TreeMap<>();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("VirtualMachine"),
        c -> new WXM1VirtualMachineParser(this.sourceURI)
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof WXMVirtualMachine) {
      final var machine = (WXMVirtualMachine) result;
      this.machines.put(machine.id(), machine);
    } else if (result instanceof WXMVirtualMachineSet) {
      this.machines.putAll(((WXMVirtualMachineSet) result).machines());
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public WXMVirtualMachineSet onElementFinished(
    final BTElementParsingContextType context)
  {
    this.builder.setMachines(this.machines);
    return this.builder.build();
  }
}
