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
import com.io7m.waxmill.client.api.WXMFlags;

import java.util.Locale;
import java.util.Map;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1FlagsParser
  implements BTElementHandlerType<WXM1Flag, WXMFlags>
{
  private final WXMFlags.Builder builder;

  public WXM1FlagsParser()
  {
    this.builder = WXMFlags.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends WXM1Flag>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("Flag"),
        c -> new WXM1FlagParser()
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final WXM1Flag result)
  {
    switch (result.name().toUpperCase(Locale.ROOT).trim()) {
      case "DISABLEMPTABLEGENERATION": {
        this.builder.setDisableMPTableGeneration(result.value());
        break;
      }
      case "FORCEVIRTUALIOPCITOUSEMSI": {
        this.builder.setForceVirtualIOPCIToUseMSI(result.value());
        break;
      }
      case "GENERATEACPITABLES": {
        this.builder.setGenerateACPITables(result.value());
        break;
      }
      case "GUESTAPICISX2APIC": {
        this.builder.setGuestAPICIsX2APIC(result.value());
        break;
      }
      case "INCLUDEGUESTMEMORYINCOREFILES": {
        this.builder.setIncludeGuestMemoryInCoreFiles(result.value());
        break;
      }
      case "REALTIMECLOCKISUTC": {
        this.builder.setRealTimeClockIsUTC(result.value());
        break;
      }
      case "WIREGUESTMEMORY": {
        this.builder.setWireGuestMemory(result.value());
        break;
      }
      case "YIELDCPUONHLT": {
        this.builder.setYieldCPUOnHLT(result.value());
        break;
      }
      default:
        throw new UnreachableCodeException();
    }
  }

  @Override
  public WXMFlags onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.builder.build();
  }
}
