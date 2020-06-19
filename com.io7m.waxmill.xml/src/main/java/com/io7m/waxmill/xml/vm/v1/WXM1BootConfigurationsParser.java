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
import com.io7m.waxmill.machines.WXMBootConfigurationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.io7m.waxmill.xml.vm.v1.WXM1Names.element;

public final class WXM1BootConfigurationsParser
  implements BTElementHandlerType<Object, List<WXMBootConfigurationType>>
{
  private final List<WXMBootConfigurationType> bootConfigurations;

  public WXM1BootConfigurationsParser()
  {
    this.bootConfigurations = new ArrayList<>();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    return Map.ofEntries(
      Map.entry(
        element("BootConfigurationGRUBBhyve"),
        c -> new WXM1BootConfigurationGRUBBhyveParser()
      )
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object result)
  {
    if (result instanceof WXMBootConfigurationType) {
      this.bootConfigurations.add((WXMBootConfigurationType) result);
    } else {
      throw new UnreachableCodeException();
    }
  }

  @Override
  public List<WXMBootConfigurationType> onElementFinished(
    final BTElementParsingContextType context)
  {
    return List.copyOf(this.bootConfigurations);
  }
}
