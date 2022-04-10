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

import com.io7m.waxmill.client.api.WXMClientProviderType;
import com.io7m.waxmill.parser.api.WXMBootConfigurationParserProviderType;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserProviderType;
import com.io7m.waxmill.serializer.api.WXMVirtualMachineSerializerProviderType;

/**
 * FreeBSD BHyve Manager (Command-line).
 */

module com.io7m.waxmill.cmdline
{
  requires static com.io7m.immutables.style;
  requires static org.immutables.value;
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires ch.qos.logback.classic;
  requires com.io7m.claypot.core;
  requires com.io7m.junreachable.core;
  requires com.io7m.jxe.core;
  requires com.io7m.waxmill.client.api;
  requires com.io7m.waxmill.machines;
  requires com.io7m.waxmill.parser.api;
  requires com.io7m.waxmill.process.api;
  requires com.io7m.waxmill.serializer.api;
  requires com.io7m.waxmill.strings.api;
  requires com.io7m.waxmill.xml;
  requires jcommander;
  requires org.apache.commons.io;
  requires org.slf4j;

  opens com.io7m.waxmill.cmdline.internal to jcommander;

  uses WXMBootConfigurationParserProviderType;
  uses WXMClientProviderType;
  uses WXMVirtualMachineParserProviderType;
  uses WXMVirtualMachineSerializerProviderType;

  exports com.io7m.waxmill.cmdline;
}
