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

import com.io7m.claypot.core.CLPApplicationConfiguration;
import com.io7m.claypot.core.CLPCommandConstructorType;
import com.io7m.claypot.core.CLPCommandType;
import com.io7m.claypot.core.Claypot;
import com.io7m.claypot.core.ClaypotType;
import com.io7m.waxmill.cmdline.internal.WXMCommandSchema;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddAHCIDisk;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddAHCIOptical;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddE1000NetworkDevice;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddFramebufferDevice;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddLPC;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddPassthru;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddVirtioDisk;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMAddVirtioNetworkDevice;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMConsole;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMDefine;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMDelete;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMDeleteBootConfigurations;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMDeleteDevice;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMExport;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMIDOf;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMImport;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMKill;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMList;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMListWithName;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMRealize;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMRun;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMSet;
import com.io7m.waxmill.cmdline.internal.WXMCommandVMUpdateBootConfigurations;
import com.io7m.waxmill.cmdline.internal.WXMCommandVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Stream;

/**
 * Main command line entry point.
 */

public final class Main implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private final String[] args;
  private final ClaypotType claypot;

  public Main(
    final String[] inArgs)
  {
    this.args =
      Objects.requireNonNull(inArgs, "Command line arguments");

    final List<CLPCommandConstructorType> commands =
      List.of(
        WXMCommandSchema::new,
        WXMCommandVMAddAHCIDisk::new,
        WXMCommandVMAddAHCIOptical::new,
        WXMCommandVMAddE1000NetworkDevice::new,
        WXMCommandVMAddFramebufferDevice::new,
        WXMCommandVMAddLPC::new,
        WXMCommandVMAddPassthru::new,
        WXMCommandVMAddVirtioDisk::new,
        WXMCommandVMAddVirtioNetworkDevice::new,
        WXMCommandVMConsole::new,
        WXMCommandVMDefine::new,
        WXMCommandVMDelete::new,
        WXMCommandVMDeleteBootConfigurations::new,
        WXMCommandVMDeleteDevice::new,
        WXMCommandVMExport::new,
        WXMCommandVMIDOf::new,
        WXMCommandVMImport::new,
        WXMCommandVMKill::new,
        WXMCommandVMList::new,
        WXMCommandVMListWithName::new,
        WXMCommandVMRealize::new,
        WXMCommandVMRun::new,
        WXMCommandVMSet::new,
        WXMCommandVMUpdateBootConfigurations::new,
        WXMCommandVersion::new
      );

    final var configuration =
      CLPApplicationConfiguration.builder()
        .setLogger(LOG)
        .setProgramName("waxmill")
        .setCommands(commands)
        .setDocumentationURI(URI.create(
          "https://www.io7m.com/software/waxmill/documentation/"))
        .build();

    this.claypot = Claypot.create(configuration);
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

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.claypot.exitCode();
  }

  @Override
  public void run()
  {
    this.claypot.execute(this.args);
  }

  /**
   * @return The names of the available commands
   */

  public Stream<String> commandNames()
  {
    return this.commands()
      .keySet()
      .stream();
  }

  /**
   * @return The available commands
   */

  public SortedMap<String, CLPCommandType> commands()
  {
    return this.claypot.commands();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[Main 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }

  /**
   * @return The exception that caused the exit
   */

  public Optional<Exception> exitCause()
  {
    return this.claypot.exitCause();
  }
}
