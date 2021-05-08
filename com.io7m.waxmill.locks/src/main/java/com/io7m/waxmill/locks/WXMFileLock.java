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

package com.io7m.waxmill.locks;

import com.io7m.waxmill.exceptions.WXMException;
import com.io7m.waxmill.exceptions.WXMExceptions;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * A file lock.
 */

public final class WXMFileLock implements AutoCloseable
{
  private final FileChannel channel;
  private final FileLock lock;

  private WXMFileLock(
    final FileChannel inChannel,
    final FileLock inLock)
  {
    this.channel =
      Objects.requireNonNull(inChannel, "channel");
    this.lock =
      Objects.requireNonNull(inLock, "lock");
  }

  /**
   * Acquire a lock on the given path.
   *
   * @param lockFile The lock file
   *
   * @return A lock
   *
   * @throws IOException On I/O errors
   */

  public static WXMFileLock acquire(
    final Path lockFile)
    throws IOException
  {
    final var channel = FileChannel.open(lockFile, CREATE, WRITE);
    try {
      final var lock = channel.lock();
      return new WXMFileLock(channel, lock);
    } catch (final IOException e) {
      try {
        channel.close();
      } catch (final IOException ex) {
        e.addSuppressed(ex);
      }
      throw e;
    }
  }

  @Override
  public void close()
    throws WXMException
  {
    final var exceptions = new WXMExceptions();

    try {
      this.lock.close();
    } catch (final IOException e) {
      exceptions.add(e);
    }

    try {
      this.channel.close();
    } catch (final IOException e) {
      exceptions.add(e);
    }

    exceptions.throwIfRequired();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[WXMFileLock 0x%s]",
      Long.toUnsignedString(
        System.identityHashCode(this),
        16)
    );
  }
}
