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

import java.io.InputStream;
import java.io.OutputStream;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractNode {

  protected Object data;
  protected String downloadedObjectType;
  @Getter
  protected String httpContentType;
  @Setter
  @Getter
  protected String httpCharacterEncoding = "UTF-8";
  @Getter
  @Setter
  protected String eTag;
  @Getter
  @Setter
  protected boolean cacheable = true;
  @Getter
  @Setter
  protected boolean leaf;

  public abstract InputStream getDownloadedObjectData();

  public abstract String getDownloadedObjectType();

  public abstract void toStream(OutputStream out);
}
