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

package com.io7m.waxmill.tests.cmdline;

import com.io7m.waxmill.client.api.WXMClientConfiguration;
import com.io7m.waxmill.cmdline.MainExitless;
import com.io7m.waxmill.tests.WXMTestDirectories;
import com.io7m.waxmill.xml.WXMClientConfigurationSerializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WXMCommandHelpTest
{
  private Path directory;
  private Path configFile;
  private Path configFileTmp;
  private Path vmDirectory;
  private Path zfsDirectory;
  private WXMClientConfiguration configuration;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory = WXMTestDirectories.createTempDirectory();
    this.configFile = this.directory.resolve("config.xml");
    this.configFileTmp = this.directory.resolve("config.xml.tmp");
    this.vmDirectory = this.directory.resolve("vmDirectory");
    this.zfsDirectory = this.directory.resolve("zfsDirectory");
    Files.createDirectories(this.vmDirectory);

    this.configuration =
      WXMClientConfiguration.builder()
        .setVirtualMachineConfigurationDirectory(this.vmDirectory)
        .setVirtualMachineRuntimeDirectory(this.zfsDirectory)
        .build();

    new WXMClientConfigurationSerializers()
      .serialize(
        this.configFile,
        this.configFileTmp,
        this.configuration
      );
  }

  @Test
  public void helpOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help"
      }
    );
  }

  @Test
  public void helpHelpOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "help"
      }
    );
  }

  @Test
  public void helpVMListOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-list"
      }
    );
  }

  @Test
  public void helpVMAddVirtioDiskOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-add-virtio-disk"
      }
    );
  }

  @Test
  public void helpVMAddVirtioNetworkDeviceOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-add-virtio-network-device"
      }
    );
  }

  @Test
  public void helpVMAddAHCIDiskOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-add-ahci-disk"
      }
    );
  }

  @Test
  public void helpVMAddLPCOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-add-lpc-device"
      }
    );
  }

  @Test
  public void helpSchemaOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "schema"
      }
    );
  }

  @Test
  public void helpVMRunOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-run"
      }
    );
  }

  @Test
  public void helpVMDeleteOK()
    throws IOException
  {
    MainExitless.main(
      new String[]{
        "help",
        "vm-delete"
      }
    );
  }
}
