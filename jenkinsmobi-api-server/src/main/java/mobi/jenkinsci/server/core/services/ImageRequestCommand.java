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
package mobi.jenkinsci.server.core.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.RawBinaryNode;
import mobi.jenkinsci.server.Config;

public class ImageRequestCommand implements RequestCommand {

  private final Config config;

  @Inject
  public ImageRequestCommand(final Config config) {
    this.config = config;
  }

  @Override
  public boolean canProcess(final HttpServletRequest request) {
    return getRequestImageFilename(request) != null;
  }

  @Override
  public AbstractNode process(final Account account,
      final HttpServletRequest request) throws IOException {
    final String resourceName = getRequestImageFilename(request);
    return getImageNode(resourceName, config.getMimeType(resourceName));
  }


  private String getRequestImageFilename(final HttpServletRequest request) {
    return request.getParameter("image");
  }

  protected AbstractNode getImageNode(final String imageFileName,
      final String mimeType) throws IOException {
    RawBinaryNode result;

    final File resourceFile =
        config.getFile(config.getPluginsHome(), imageFileName);

    if (resourceFile != null && resourceFile.exists()) {
      result = newImageNodeFromFile(mimeType, resourceFile);
    } else {
      result = newImageNodeFromClasspath(imageFileName, mimeType);
    }

    return result;
  }

  private RawBinaryNode newImageNodeFromClasspath(final String imageFileName,
      final String mimeType) throws ResourceNotFoundException {
    RawBinaryNode result;
    final ClassLoader thisClassLoader = getClass().getClassLoader();
    final InputStream resourceAsStream =
        thisClassLoader.getResourceAsStream(imageFileName);
    if (resourceAsStream == null) {
      throw new ResourceNotFoundException("Cannot find image " + imageFileName
          + " in plugin classpath");
    }

    result = new RawBinaryNode();
    result.setData(resourceAsStream);
    result.setHttpContentType(mimeType);
    return result;
  }

  private RawBinaryNode newImageNodeFromFile(final String mimeType,
      final File resourceFile) throws ResourceNotFoundException {
    RawBinaryNode result;
    result = new RawBinaryNode();
    result.setSize(resourceFile.length());
    result.setData(getImageInputStream(resourceFile));
    result.setHttpContentType(mimeType);
    return result;
  }

  private FileInputStream getImageInputStream(final File resourceFile)
      throws ResourceNotFoundException {
    try {
      return new FileInputStream(resourceFile);
    } catch (final FileNotFoundException e) {
      throw new ResourceNotFoundException("Cannot file image file "
          + resourceFile + " from file-system");
    }
  }
}
