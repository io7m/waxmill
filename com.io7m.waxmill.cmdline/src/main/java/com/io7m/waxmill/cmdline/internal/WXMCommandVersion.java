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

package com.io7m.waxmill.cmdline.internal;

import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.waxmill.client.api.WXMApplicationVersion;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "version" command.
 */

@Parameters(commandDescription = "Show the application version.")
public final class WXMCommandVersion extends CLPAbstractCommand
{
  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandVersion(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final WXMApplicationVersion version =
      WXMServices.findApplicationVersion();

    System.out.printf(
      "%s %s (%s)%n",
      version.applicationName(),
      version.applicationVersion(),
      version.applicationBuild()
    );
    return SUCCESS;
  }

  @Override
  public String name()
  {
    return "version";
  }
}
