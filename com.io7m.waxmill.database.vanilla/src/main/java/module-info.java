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

import com.io7m.waxmill.database.api.WXMVirtualMachineDatabaseProviderType;
import com.io7m.waxmill.database.vanilla.WXMVirtualMachineDatabases;
import com.io7m.waxmill.parser.api.WXMVirtualMachineParserProviderType;
import com.io7m.waxmill.serializer.api.WXMVirtualMachineSerializerProviderType;

/**
 * FreeBSD BHyve Manager (Database vanilla implementation)
 */

module com.io7m.waxmill.database.vanilla
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.waxmill.client.api;
  requires com.io7m.waxmill.database.api;
  requires com.io7m.waxmill.exceptions;
  requires com.io7m.waxmill.locks;
  requires com.io7m.waxmill.machines;
  requires com.io7m.waxmill.parser.api;
  requires com.io7m.waxmill.serializer.api;

  uses WXMVirtualMachineParserProviderType;
  uses WXMVirtualMachineSerializerProviderType;

  provides WXMVirtualMachineDatabaseProviderType
    with WXMVirtualMachineDatabases;

  exports com.io7m.waxmill.database.vanilla;
}