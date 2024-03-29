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
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.waxmill.xml.WXMSchemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

import static com.io7m.claypot.core.CLPCommandType.Status.FAILURE;
import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "schema" command.
 */

@Parameters(commandDescription = "Export schemas.")
public final class WXMCommandSchema extends CLPAbstractCommand
{
  private static final Logger LOG =
    LoggerFactory.getLogger(WXMCommandSchema.class);

  private final WXMMessages messages;

  @Parameter(
    names = "--id",
    description = "The schema identifier")
  private URI schemaId;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public WXMCommandSchema(
    final CLPCommandContextType inContext)
  {
    super(inContext);
    this.messages = WXMMessages.create();
  }

  @Override
  public String extendedHelp()
  {
    return this.messages.format("schemaHelp");
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final var schemas =
      Map.ofEntries(
        Map.entry(
          WXMSchemas.configSchemaV1p0Namespace(),
          WXMSchemas.configSchemaV1p0()
        ),
        Map.entry(
          WXMSchemas.vmSchemaV1p0Namespace(),
          WXMSchemas.vmSchemaV1p0()
        )
      );

    if (this.schemaId == null) {
      for (final var schema : schemas.keySet()) {
        System.out.println(schema);
      }
      return SUCCESS;
    }

    final var schema = schemas.get(this.schemaId);
    if (schema == null) {
      LOG.error(
        "{}",
        this.messages.format("errorSchemaNonexistent", this.schemaId)
      );
      return FAILURE;
    }

    final var location = schema.location();
    try (var stream = location.openStream()) {
      stream.transferTo(System.out);
    }
    System.out.println();
    System.out.flush();
    return SUCCESS;
  }

  @Override
  public String name()
  {
    return "schema";
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMCommandSchema 0x%s]",
      Long.toUnsignedString(System.identityHashCode(this), 16)
    );
  }
}
