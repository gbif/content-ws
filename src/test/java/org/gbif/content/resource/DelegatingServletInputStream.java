/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.content.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    assertNotNull(sourceStream, "Source InputStream must not be null");
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
    // NOP
  }
}
