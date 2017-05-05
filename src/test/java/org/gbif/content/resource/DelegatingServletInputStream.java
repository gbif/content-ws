package org.gbif.content.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.junit.Assert;

/**
 * Utility class to be used in test cases.
 */
class DelegatingServletInputStream extends ServletInputStream {

  private final InputStream sourceStream;

  /**
   * Create a DelegatingServletInputStream for the given source stream.
   * @param sourceStream the source stream (never {@code null}
   */
  DelegatingServletInputStream(InputStream sourceStream) {
    Assert.assertNotNull("Source InputStream must not be null", sourceStream);
    this.sourceStream = sourceStream;
  }

  @Override
  public int read() throws IOException {
    return sourceStream.read();
  }

  @Override
  public void close() throws IOException {
    super.close();
    sourceStream.close();
  }

  @Override
  public boolean isFinished() {
    return Boolean.FALSE;
  }

  @Override
  public boolean isReady() {
    return Boolean.TRUE;
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    //NOP
  }
}
