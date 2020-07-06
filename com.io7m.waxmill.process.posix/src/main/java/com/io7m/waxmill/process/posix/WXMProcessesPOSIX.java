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

package com.io7m.waxmill.process.posix;

import com.io7m.waxmill.process.api.WXMProcessDescription;
import com.io7m.waxmill.process.api.WXMProcessesType;
import com.io7m.waxmill.process.posix.internal.WXMProcessMessages;
import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.ProcessBuilder.Redirect.PIPE;

/**
 * A JNA POSIX implementation of the process creator.
 */

public final class WXMProcessesPOSIX implements WXMProcessesType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMProcessesPOSIX.class);

  private final WXMProcessMessages messages;
  private final CLibraryType library;

  private WXMProcessesPOSIX(
    final WXMProcessMessages inMessages,
    final CLibraryType inLibrary)
  {
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.library =
      Objects.requireNonNull(inLibrary, "library");
  }

  private interface CLibraryType extends Library
  {
    int execve(
      String executable,
      String[] argv,
      String[] env
    );

    String strerror(
      int errno);
  }

  /**
   * @return A new process creator.
   */

  public static WXMProcessesType create()
  {
    final CLibraryType library =
      Native.load("c", CLibraryType.class);
    final var messages =
      WXMProcessMessages.create();

    return new WXMProcessesPOSIX(messages, library);
  }

  @Override
  public void processReplaceCurrent(
    final WXMProcessDescription description)
    throws IOException
  {
    Objects.requireNonNull(description, "description");

    final var descEnvironment = description.environment();
    final var environment = new String[descEnvironment.size()];
    var envIndex = 0;
    for (final var entry : descEnvironment.entrySet()) {
      environment[envIndex] =
        String.format("%s=%s", entry.getKey(), entry.getValue());
      ++envIndex;
    }

    final List<String> descArguments = description.arguments();
    final var arguments = new String[descArguments.size() + 1];

    var argIndex = 1;
    final var executable = description.executable();
    arguments[0] = executable.toString();
    for (final var argument : descArguments) {
      arguments[argIndex] = argument;
      ++argIndex;
    }

    LOG.debug("execute {} {}", executable, descArguments);
    this.library.execve(arguments[0], arguments, environment);

    final var errorCode = Native.getLastError();
    final var errorMessage = this.library.strerror(errorCode);
    throw new IOException(this.errorNoExec(
      description,
      errorCode,
      errorMessage));
  }

  private String errorNoExec(
    final WXMProcessDescription description,
    final int errorCode,
    final String errorMessage)
  {
    return this.messages.format(
      "errorCannotExecute",
      description.executable().toString(),
      String.join(" ", description.arguments()),
      errorMessage,
      Integer.valueOf(errorCode)
    );
  }

  @Override
  public Process processStart(
    final WXMProcessDescription description)
    throws IOException
  {
    Objects.requireNonNull(description, "description");

    final var builder = new ProcessBuilder();
    builder.environment().clear();
    builder.environment().putAll(description.environment());
    builder.redirectOutput(PIPE);
    builder.redirectError(PIPE);
    builder.redirectInput(PIPE);
    builder.redirectErrorStream(true);

    final var command = new ArrayList<String>();
    command.add(description.executable().toString());
    command.addAll(description.arguments());
    builder.command(command);

    LOG.debug("execute {}", command);
    return builder.start();
  }

  @Override
  public void processStartAndWait(
    final WXMProcessDescription description)
    throws IOException
  {
    final var process = this.processStart(description);
    final int result;
    try {
      result = process.waitFor();
    } catch (final InterruptedException e) {
      throw new IOException(e);
    }

    if (result != 0) {
      throw new IOException(this.messages.format(
        "errorCommandFailed",
        description.executable(),
        String.join(" ", description.arguments()),
        Integer.valueOf(result)
      ));
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMProcessesPOSIX 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
