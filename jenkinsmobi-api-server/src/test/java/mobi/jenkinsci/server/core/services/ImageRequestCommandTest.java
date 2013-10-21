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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import mobi.jenkinsci.commons.Account;
import mobi.jenkinsci.exceptions.ResourceNotFoundException;
import mobi.jenkinsci.model.AbstractNode;
import mobi.jenkinsci.model.RawBinaryNode;

import mobi.jenkinsci.server.Config;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImageRequestCommandTest {

  private static final String IMAGE_REQUEST_CONSUMER_TEST_CLASS_RESOURCE_NAME =
      ImageRequestCommandTest.class.getCanonicalName().replaceAll("\\.", "/");
  private static final String IMAGE_NAME_FROM_CLASSPATH =
      IMAGE_REQUEST_CONSUMER_TEST_CLASS_RESOURCE_NAME + ".png";
  private static final String IMAGE_NAME_FROM_FILESYSTEM = "test.png";

  private static final String AN_IMAGE_FILE_NAME = "image.png";
  private static final String TMP_PREFIX = ImageRequestCommand.class
      .getSimpleName();
  private static final String IMAGE_TEMP_FILE_CONTENT = "abcd1234";

  @Mock
  private Config mockConfig;

  @Mock
  private HttpServletRequest mockRequest;

  @Mock
  private Account mockAccount;

  @InjectMocks
  private ImageRequestCommand imageRequestConsumer;

  private File tmpFile;

  @Before
  public void setUp() {
    when(mockConfig.getMimeType(any(String.class))).thenReturn("image/png");
  }

  @After
  public void tearDown() {
    if (tmpFile != null && tmpFile.exists()) {
      tmpFile.delete();
    }
  }

  @Test
  public void imageConsumerShouldConsumeRequestWithImageParameter() {
    when(mockRequest.getParameter("image")).thenReturn(AN_IMAGE_FILE_NAME);

    assertThat(imageRequestConsumer.canProcess(mockRequest), is(true));
  }

  @Test
  public void imageConsumerShouldNotConsumeOtherRequest() {
    assertThat(imageRequestConsumer.canProcess(mockRequest), is(false));
  }

  @Test
  public void imageConsumerShouldReturnNotNullImageNodeFromClassPath()
      throws IOException {
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_CLASSPATH);

    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);
    assertThat(imageNode, is(notNullValue()));
  }

  private void mockRequestParameterToReturnTestImageName(final String imageName) {
    when(mockRequest.getParameter("image")).thenReturn(imageName);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void imageConsumerShouldThrowResourceNotFoundWhenImageNotFoundInClassPath()
      throws IOException {
    mockRequestParameterToReturnTestImageName("wrong"
        + IMAGE_NAME_FROM_CLASSPATH);

    imageRequestConsumer.process(mockAccount, mockRequest);
  }

  @Test
  public void imageConsumerShouldReturnNotNullImageNodeFromFilesystem()
      throws IOException {
    // given
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_FILESYSTEM);
    mockConfigToReturnFileFromFilesystem(IMAGE_NAME_FROM_FILESYSTEM,
        createImageFile());

    // when
    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);

    // then
    assertThat(imageNode, is(notNullValue()));
  }

  @Test(expected = ResourceNotFoundException.class)
  public void imageConsumerShouldThrowResourceNotFoundExceptionWhenNullFileReturnedForImageOnFilesystem()
      throws IOException {
    // given
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_FILESYSTEM);

    imageRequestConsumer.process(mockAccount, mockRequest);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void imageConsumerShouldThrowResourceNotFoundExceptionWhenNotExistentFileReturnedForImageOnFilesystem()
      throws IOException {
    // given
    mockRequestParameterToReturnTestImageName("wrong"
        + IMAGE_NAME_FROM_FILESYSTEM);
    mockConfigToReturnFileFromFilesystem(IMAGE_NAME_FROM_FILESYSTEM, new File(
        "wrong" + IMAGE_NAME_FROM_FILESYSTEM));

    imageRequestConsumer.process(mockAccount, mockRequest);
  }

  private void mockConfigToReturnFileFromFilesystem(final String fileName,
      final File file) throws IOException {
    when(mockConfig.getFile(any(File.class), eq(fileName))).thenReturn(file);
  }

  private File createImageFile() throws IOException {
    tmpFile = File.createTempFile(TMP_PREFIX, IMAGE_NAME_FROM_FILESYSTEM);
    final FileWriter out = new FileWriter(tmpFile);
    out.write(IMAGE_TEMP_FILE_CONTENT);
    out.close();
    return tmpFile;
  }

  @Test
  public void imageReturnedFromClassPathShouldBeRawStreamNode()
      throws IOException {
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_CLASSPATH);

    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);
    assertThat(imageNode, is(instanceOf(RawBinaryNode.class)));
  }

  @Test
  public void imageReturnedFromFilesystemShouldBeRawStreamNode()
      throws IOException {
    // given
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_FILESYSTEM);
    mockConfigToReturnFileFromFilesystem(IMAGE_NAME_FROM_FILESYSTEM,
        createImageFile());

    // when
    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);

    // then
    assertThat(imageNode, is(instanceOf(RawBinaryNode.class)));
  }

  @Test
  public void imageReturnedFromClassPathShouldHavePngContentType()
      throws IOException {
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_CLASSPATH);

    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);
    assertThat(imageNode, hasProperty("httpContentType", equalTo("image/png")));
  }

  @Test
  public void imageStreamShouldBeEqualsToResourceStreamFromClassPath()
      throws IOException {
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_CLASSPATH);

    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);
    assertThat(imageNode,
        hasProperty("downloadedObjectData", instanceOf(InputStream.class)));
    Assert.assertTrue(IOUtils.contentEquals(
        imageNode.getDownloadedObjectData(), ImageRequestCommand.class
            .getClassLoader().getResource(IMAGE_NAME_FROM_CLASSPATH)
            .openStream()));
  }

  @Test
  public void imageStreamShouldBeEqualsToResourceStreamFromFilesystem()
      throws IOException {
    mockRequestParameterToReturnTestImageName(IMAGE_NAME_FROM_FILESYSTEM);
    final File imageFromFilesystem = createImageFile();
    mockConfigToReturnFileFromFilesystem(IMAGE_NAME_FROM_FILESYSTEM,
        imageFromFilesystem);

    final AbstractNode imageNode =
        imageRequestConsumer.process(mockAccount, mockRequest);
    assertThat(imageNode,
        hasProperty("downloadedObjectData", instanceOf(InputStream.class)));
    Assert.assertTrue(IOUtils.contentEquals(
        imageNode.getDownloadedObjectData(), new FileInputStream(
            imageFromFilesystem)));
  }
}
