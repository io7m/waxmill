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

import com.beust.jcommander.Parameter;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.waxmill.cmdline.internal.WXMEnvironment.checkConfigurationPath;

/**
 * The abstract base command.
 */

public abstract class WXMAbstractCommandWithConfiguration
  extends CLPAbstractCommand
{
  private final WXMMessages messages;
  private final Logger logger;

  @Parameter(
    names = "--configuration",
    description = "The path to the configuration file (environment variable: $WAXMILL_CONFIGURATION_FILE)",
    required = false
  )
  private Path configurationFile = WXMEnvironment.configurationFile();

  /**
   * Construct a command.
   *
   * @param inLogger  The command logger
   * @param inContext The command context
   */

  public WXMAbstractCommandWithConfiguration(
    final Logger inLogger,
    final CLPCommandContextType inContext)
  {
    super(inContext);
    this.logger = Objects.requireNonNull(inLogger, "logger");
    this.messages = WXMMessages.create();
  }

  protected abstract Status executeActualWithConfiguration(
    Path configurationPath)
    throws Exception;

  @Override
  protected final Status executeActual()
    throws Exception
  {
    if (!checkConfigurationPath(this.logger(), this.configurationFile)) {
      return FAILURE;
    }

    return this.executeActualWithConfiguration(this.configurationFile);
  }

  protected final WXMMessages messages()
  {
    return this.messages;
  }

  protected final void error(
    final String id,
    final Object... arguments)
  {
    this.logger.error("{}", this.messages.format(id, arguments));
  }

  protected final void info(
    final String id,
    final Object... arguments)
  {
    this.logger.info("{}", this.messages.format(id, arguments));
  }
}
