// Copyright (C) 2013 GerritForge www.gerritforge.com
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package mobi.jenkinsci.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class RawBinaryNode extends AbstractNode {
  private static final Logger log = Logger.getLogger(RawBinaryNode.class);

  private long size;

  public long getSize() {
    return size;
  }

  public void setSize(final long size) {
    this.size = size;
  }

  @Override
  public InputStream getDownloadedObjectData() {
    if (data == null) {
      return null;
    }

    if (data instanceof InputStream) {
      return (InputStream) data;
    } else if (data instanceof String) {
      return IOUtils.toInputStream((String) data);
    } else if (data instanceof byte[]) {
      return new ByteArrayInputStream((byte[]) data);
    } else {
      log.error("Invalid data object type for serialisation: "
          + data.getClass());
      return null;
    }
  }

  @Override
  public String getDownloadedObjectType() {
    return "RawResource";
  }

  public void setData(final Object data) {
    this.data = data;
  }

  public void setHttpContentType(final String httpContentType) {
    this.httpContentType = httpContentType;
  }

  @Override
  public void toStream(final OutputStream out) {
  }
}
