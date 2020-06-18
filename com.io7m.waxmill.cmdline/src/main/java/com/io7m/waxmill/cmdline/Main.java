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

package com.io7m.waxmill.cmdline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.io7m.waxmill.cmdline.internal.WXMBriefUsageFormatter;
import com.io7m.waxmill.cmdline.internal.WXMCommandHelp;
import com.io7m.waxmill.cmdline.internal.WXMCommandRoot;
import com.io7m.waxmill.cmdline.internal.WXMCommandType;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddAHCIDisk;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddLPC;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddVirtioDisk;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddVirtioNetworkDevice;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMDefine;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMExport;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMImport;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMList;
import com.io7m.waxmill.cmdline.internal.WXMCommandVersion;
import com.io7m.waxmill.cmdline.internal.WXMStringBuilderConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Main command line entry point.
 */

public final class Main implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final Map<String, WXMCommandType> commands;
  private final JCommander commander;
  private final String[] args;
  private int exitCode;

  public Main(
    final String[] inArgs)
  {
    this.args =
      Objects.requireNonNull(inArgs, "Command line arguments");

    final WXMCommandRoot r = new WXMCommandRoot();
    this.commander = new JCommander(r);
    this.commander.setProgramName("waxmill");

    this.commands =
      Map.ofEntries(
        Map.entry(
          "help",
          new WXMCommandHelp(this.commander)),
        Map.entry(
          "version",
          new WXMCommandVersion()),
        Map.entry(
          "vm-add-ahci-disk",
          new WXMCommandVMAddAHCIDisk()),
        Map.entry(
          "vm-add-lpc-device",
          new WXMCommandVMAddLPC()),
        Map.entry(
          "vm-add-virtio-disk",
          new WXMCommandVMAddVirtioDisk()),
        Map.entry(
          "vm-add-virtio-network-device",
          new WXMCommandVMAddVirtioNetworkDevice()),
        Map.entry(
          "vm-define",
          new WXMCommandVMDefine()),
        Map.entry(
          "vm-import",
          new WXMCommandVMImport()),
        Map.entry(
          "vm-export",
          new WXMCommandVMExport()),
        Map.entry(
          "vm-list",
          new WXMCommandVMList())
      );

    for (final var entry : this.commands.entrySet()) {
      this.commander.addCommand(entry.getKey(), entry.getValue());
    }
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final Main cm = new Main(args);
    cm.run();
    System.exit(cm.exitCode());
  }

  private static void logExceptionFriendly(
    final Throwable e)
  {
    if (e == null) {
      return;
    }

    LOG.error("{}: {}", e.getClass().getCanonicalName(), e.getMessage());
    if (LOG.isDebugEnabled()) {
      final var trace = new StringBuilder(256);
      final String lineSeparator = System.lineSeparator();
      trace.append(lineSeparator);
      final var elements = e.getStackTrace();
      for (final var element : elements) {
        trace.append("  at ");
        trace.append(element.getModuleName());
        trace.append('/');
        trace.append(element.getClassName());
        trace.append('.');
        trace.append(element.getMethodName());
        trace.append('(');
        trace.append(element.getFileName());
        trace.append(':');
        trace.append(element.getLineNumber());
        trace.append(')');
        trace.append(lineSeparator);
      }
      LOG.debug("Stacktrace of {}: {}", e.getClass().getCanonicalName(), trace);
    }

    final var causes = e.getSuppressed();
    if (causes.length > 0) {
      for (final var cause : causes) {
        logExceptionFriendly(cause);
      }
    }
    logExceptionFriendly(e.getCause());
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exitCode;
  }

  @Override
  public void run()
  {
    try {
      this.commander.parse(this.args);

      final String cmd = this.commander.getParsedCommand();
      if (cmd == null) {
        final var console = new WXMStringBuilderConsole();
        this.commander.setUsageFormatter(new WXMBriefUsageFormatter(this.commander));
        this.commander.setConsole(console);
        this.commander.usage();
        System.err.println(console.builder());
        this.exitCode = 1;
        return;
      }

      final WXMCommandType command = this.commands.get(cmd);
      final WXMCommandType.Status status = command.execute();
      this.exitCode = status.exitCode();
    } catch (final ParameterException e) {
      LOG.error("{}", e.getMessage());
      this.exitCode = 1;
    } catch (final Exception e) {
      logExceptionFriendly(e);
      this.exitCode = 1;
    }
  }

  @Override
  public String toString()
  {
    return String.format(
      "[Main 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
