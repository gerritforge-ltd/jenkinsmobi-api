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
package mobi.jenkinsci.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.jenkinsci.commons.Config;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeaderElement;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class ProxyUtil {
  private static final String CSS_BACKGROUND_IMAGE_PATTERN =
      "(background-image|background):" + "[^;\\x28\\x29url]*" + "url" + "\\x28"
          + "([^d][^a][^t][^a][^:][^\\x29]+)" + "\\x29";
  private static final Logger log = Logger.getLogger(ProxyUtil.class);
  protected static final Hashtable<String, String> MIME_TYPES =
      new Hashtable<String, String>();
  static {
    MIME_TYPES.put("gif", "image/gif");
    MIME_TYPES.put("jpg", "image/jpeg");
    MIME_TYPES.put("jpeg", "image/jpeg");
    MIME_TYPES.put("png", "image/png");
    MIME_TYPES.put("css", "text/css");
  }
  private static final ProxyUtil staticInstance = new ProxyUtil();

  @Inject
  Config config;

  @Inject
  HttpClientFactory httpClientFactory;

  public static ProxyUtil getInstance() {
    return staticInstance;
  }

  public String proxyfy(final String userAgent, final String pluginName,
      final String url, final String requestUrl, final String decorate,
      final InputStream inStream, final Object ctx) throws IOException {
    final StringWriter writer = new StringWriter();
    IOUtils.copy(inStream, writer, "UTF-8");
    final String resultString = writer.toString();
    return proxyfy(userAgent, pluginName, url, requestUrl, decorate,
        resultString, ctx);
  }

  private String proxyfy(final String userAgent, final String pluginName,
      final String url, final String requestUrl, final String decorate,
      String htmlPage, final Object ctx) throws IOException {
    final long startTime = System.currentTimeMillis();
    log.debug("Proxying results for URL " + url);
    htmlPage = resolveJs(userAgent, pluginName, url, htmlPage, ctx);
    htmlPage = resolveCss(userAgent, pluginName, url, htmlPage, ctx);
    htmlPage = resolveImages(userAgent, pluginName, url, htmlPage, ctx);
    htmlPage = replaceLinks(requestUrl + "?web=", url, htmlPage);

    if (decorate != null) {
      if ("html".equals(decorate)) {
        htmlPage =
            "<html><head><title></title></head><body><pre>" + htmlPage
                + "</pre></body></html>";
      }
    }
    log.debug("Proxy completed in " + (System.currentTimeMillis() - startTime));
    return htmlPage;
  }

  private String resolveImages(final String userAgent, final String pluginName,
      final String url, String resultString, final Object ctx) {
    resultString =
        resolveImages(userAgent, pluginName, Pattern.compile(
            "<(img|image)[^>]*src=[\"']([^\"']+\\.[a-zA-Z]+)[\"'][^>]*/>",
            Pattern.DOTALL), 2, url, resultString, ctx);
    resultString =
        resolveImages(userAgent, pluginName,
            Pattern.compile(CSS_BACKGROUND_IMAGE_PATTERN, Pattern.DOTALL), 2,
            url, resultString, ctx);
    return resultString;
  }

  private String resolveImages(final String userAgent, final String pluginName,
      final Pattern imgLinkPattern, final int index, final String url,
      final String resultString, final Object ctx) {
    log.debug("Resolving images for URL " + url);
    final StringBuilder outString = new StringBuilder();
    int currPos = 0;
    final Matcher matcher = imgLinkPattern.matcher(resultString);
    while (matcher.find(currPos)) {
      final int start = matcher.start(index);
      final int end = matcher.end(index);
      final String imagePath = matcher.group(index);
      if (isAlreadyDataEncoded(imagePath)) {
        continue;
      }

      outString.append(resultString.substring(currPos, start));
      try {
        outString.append(retrieveImage(userAgent, pluginName, url, imagePath,
            ctx));
      } catch (final Exception e) {
        log.warn("Cannot retrieve image '" + imagePath + "'", e);
      }
      currPos = end;
    }
    outString.append(resultString.substring(currPos));
    log.debug(outString.length() + " Base64 of images included for URL " + url);
    return outString.toString();
  }

  private boolean isAlreadyDataEncoded(final String imagePath) {
    return imagePath.startsWith("data:");
  }

  private String retrieveImage(final String userAgent, final String pluginName,
      final String url, final String imagePath, final Object ctx)
      throws Exception {
    log.debug("Resolving image " + imagePath + " in WebPage " + url);
    final StringBuilder outImage = new StringBuilder();
    final HashMap<String, HeaderElement[]> imageHeader =
        new HashMap<String, HeaderElement[]>();
    final byte[] imageBlob =
        downloadContent(userAgent, pluginName, url, imagePath, imageHeader, ctx);
    outImage.append("data:");
    final HeaderElement[] contentType = imageHeader.get("Content-Type");
    outImage.append(contentType[0].getName());
    outImage.append(";base64,");
    outImage.append(Base64.encodeBase64String(imageBlob).replaceAll("\r", "")
        .replaceAll("\n", ""));
    return outImage.toString();
  }

  private String resolveCss(final String userAgent, final String pluginName,
      final String url, final String resultString, final Object ctx)
      throws IOException {
    log.debug("Resolving CSS for URL " + url);
    final StringBuilder outString = new StringBuilder();
    int currPos = 0;
    final Pattern cssLinkPattern =
        Pattern.compile("<link" + "[^>]*" + "rel=[\"']stylesheet[\"']"
            + "[^>]*" + "href=[\"']([^>\"']*)[\"']" + "[^>]*" + "/>",
            Pattern.DOTALL);
    final Matcher matcher = cssLinkPattern.matcher(resultString);
    while (matcher.find(currPos)) {
      final int start = matcher.start();
      final int end = matcher.end();
      outString.append(resultString.substring(currPos, start));
      final String cssUrl = matcher.group(1);
      String cssText = retrieveCss(userAgent, pluginName, url, cssUrl, ctx);
      cssText =
          resolveImages(userAgent, pluginName,
              Pattern.compile(CSS_BACKGROUND_IMAGE_PATTERN, Pattern.DOTALL), 2,
              getBasePathUrl(resolveRelativeUrl(url, cssUrl)), cssText, ctx);
      outString.append(cssText);
      currPos = end;
    }

    outString.append(resultString.substring(currPos));
    log.debug(outString.length() + " CSS chars included for URL " + url);
    return outString.toString();
  }

  private String resolveJs(final String userAgent, final String pluginName,
      final String url, final String resultString, final Object ctx)
      throws IOException {
    log.debug("Resolving JavaScript for URL " + url);
    final StringBuilder outString = new StringBuilder();
    int currPos = 0;
    final Pattern linkPattern =
        Pattern
            .compile(
                "<script>?[^>]*src=[\"\\']([^>\"\\']*)[\"\\']([^>]*/>|[^>]*>[ \r\n]*</script>)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    final Matcher matcher = linkPattern.matcher(resultString);
    while (matcher.find(currPos)) {
      final int start = matcher.start();
      final int end = matcher.end();
      outString.append(resultString.substring(currPos, start));
      final String cssUrl = matcher.group(1);
      final String jsText = retrieveJs(userAgent, pluginName, url, cssUrl, ctx);
      outString.append(jsText);
      currPos = end;
    }

    outString.append(resultString.substring(currPos));
    log.debug(outString.length() + " JavaScript chars included for URL " + url);
    return outString.toString();
  }

  private String retrieveJs(final String userAgent, final String pluginName,
      final String url, final String linkUrl, final Object ctx)
      throws IOException {
    return retrieveMarkup(userAgent, "<script type=\"text/javascript\">\n",
        "</script>", pluginName, url, linkUrl, ctx);
  }

  private String retrieveCss(final String userAgent, final String pluginName,
      final String url, final String linkUrl, final Object ctx)
      throws IOException {
    return retrieveMarkup(userAgent, "<style>\n", "</style>", pluginName, url,
        linkUrl, ctx);
  }

  private String retrieveMarkup(final String userAgent, final String header,
      final String footer, final String pluginName, final String url,
      final String linkUrl, final Object ctx) throws IOException {
    final StringBuilder expandedMarkup = new StringBuilder(header);
    try {
      expandedMarkup.append(new String(downloadContent(userAgent, pluginName,
          url, linkUrl, null, ctx)));
    } catch (final IOException e) {
      log.warn("Cannot download " + linkUrl + ": skipping", e);
    }
    expandedMarkup.append("\n");
    expandedMarkup.append(footer);
    return expandedMarkup.toString();
  }

  private byte[] downloadContent(final String userAgent,
      final String pluginName, final String url, String linkUrl,
      final HashMap<String, HeaderElement[]> contentHeaders, final Object ctx)
      throws IOException {
    byte[] content = null;
    final long startTime = System.currentTimeMillis();
    boolean local = false;
    try {
      if (!linkUrl.startsWith("http")) {
        linkUrl = resolveRelativeUrl(url, linkUrl);
      }
      if (pluginName != null) {
        final byte[] localContent =
            getLocalContent(pluginName, linkUrl, contentHeaders);
        if (localContent != null) {
          local = true;
          return content = localContent;
        }
      }
      return content = retrieveUrl(userAgent, linkUrl, contentHeaders);
    } finally {
      if (content != null) {
        log.debug("DOWNLOAD " + linkUrl + " from baseUrl:" + url + " ("
            + content.length / 1024 + "kB): retrieved from "
            + (local ? "LOCAL" : "REMOTE") + " in "
            + (System.currentTimeMillis() - startTime) + " msec");
      } else {
        log.error("Content NOT FOUND");
      }
    }
  }

  private byte[] getLocalContent(final String pluginName, String linkUrl,
      final HashMap<String, HeaderElement[]> contentHeaders) {
    final int schemeEnd = linkUrl.indexOf("://");
    if (schemeEnd >= 0) {
      linkUrl = linkUrl.substring(linkUrl.indexOf('/', schemeEnd + 4));
    }
    final File resourceFile =
        config.getFile(config.getPluginsHome(), pluginName, linkUrl);
    final String resourceFileName = resourceFile.getAbsolutePath();

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      final FileInputStream in = new FileInputStream(resourceFile);
      IOUtils.copy(in, out);
      in.close();
      out.close();

      if (contentHeaders != null) {
        final String fileExt =
            resourceFileName.substring(resourceFileName.lastIndexOf('.') + 1)
                .toLowerCase();
        contentHeaders.put("Content-Type",
            new HeaderElement[] {new BasicHeaderElement(
                MIME_TYPES.get(fileExt), null)});
      }

      return out.toByteArray();
    } catch (final IOException e) {
      return null;
    }
  }

  private String resolveRelativeUrl(String url, String linkUrl)
      throws IOException {
    linkUrl = getStringWithoutQuotes(linkUrl.trim());

    if (isRelativeBackUrl(linkUrl)) {
      return resolveRelativeUrl(getParentUrl(url), getChildUrl(linkUrl));
    } else if (isBindingSlashNeeded(url, linkUrl)) {
      url = url + "/" + linkUrl;
    } else if (isAbsoluteUrl(linkUrl)) {
      final int schemeEnd = url.indexOf("://") + 3;
      final int firstSlash = url.indexOf('/', schemeEnd);
      url = (firstSlash > 0 ? url.substring(0, firstSlash) : url) + linkUrl;
    } else {
      url = url + linkUrl;
    }


    return url;
  }

  private String getStringWithoutQuotes(String linkUrl) {
    if (linkUrl.startsWith("'") || linkUrl.startsWith("\"")) {
      linkUrl = linkUrl.substring(1);
    }
    if (linkUrl.endsWith("'") || linkUrl.endsWith("\"")) {
      linkUrl = linkUrl.substring(0, linkUrl.length() - 1);
    }
    return linkUrl;
  }

  private String getChildUrl(String url) {
    if (url.startsWith("/")) {
      url = url.substring(1);
    }

    return url.substring(url.indexOf('/') + 1);
  }

  private String getParentUrl(String url) {
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    url = url.substring(0, url.lastIndexOf('/') + 1);
    return url;
  }

  public String getBasePathUrl(String url) {
    if (isFileUrl(url)) {
      url = url.substring(0, url.lastIndexOf('/') + 1);
    }
    return url;
  }

  private boolean isFileUrl(final String url) {
    return url.lastIndexOf('.') > url.lastIndexOf('/');
  }

  private boolean isAbsoluteUrl(final String linkUrl) {
    return linkUrl.startsWith("/");
  }

  private boolean isBindingSlashNeeded(final String url, final String linkUrl) {
    return !url.endsWith("/") && !isAbsoluteUrl(linkUrl);
  }

  private boolean isRelativeBackUrl(final String linkUrl) {
    return linkUrl.startsWith("..");
  }

  protected byte[] retrieveUrl(final String userAgent, final String linkUrl,
      final HashMap<String, HeaderElement[]> contentHeaders) throws IOException {
    final HttpClient client = httpClientFactory.getHttpClient();
    final HttpGet get = new HttpGet(linkUrl);
    if (userAgent != null) {
      get.setHeader("User-Agent", userAgent);
    }
    final HttpResponse response = client.execute(get);
    try {
      final int status = response.getStatusLine().getStatusCode();
      if (status != HttpURLConnection.HTTP_OK) {
        throw new IOException("HTTP-GET " + linkUrl + " returned status "
            + status);
      }

      if (contentHeaders != null) {
        for (final Header header : response.getAllHeaders()) {
          contentHeaders.put(header.getName(), header.getElements());
        }
      }

      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final InputStream content = response.getEntity().getContent();
      IOUtils.copy(content, out);
      content.close();
      return out.toByteArray();
    } finally {
      get.releaseConnection();
    }
  }


  public String replaceLinks(final String newLinkPrefix,
      final String remoteBasePath, String content) {
    content =
        replaceLinks("(href|src|action)[ ]*=[ ]*\"([^\"]*)\"", 2,
            newLinkPrefix, remoteBasePath, content);
    content =
        replaceLinks("(\\Q" + remoteBasePath + "\\E[^\"']*)", 1, newLinkPrefix,
            remoteBasePath, content);
    if (remoteBasePath.startsWith("http")) {
      try {
        final String remotePath = new URL(remoteBasePath).getPath();
        content =
            replaceLinks("(\\Q" + remotePath + "\\E[^\"']*)", 1, newLinkPrefix,
                remoteBasePath, content);
      } catch (final MalformedURLException e) {
        throw new IllegalArgumentException("Invalid remote basePath URI "
            + remoteBasePath, e);
      }
    }
    return content;
  }

  public String replaceLinks(final String linkPattern, final int linkIndex,
      final String newLinkPrefix, final String remoteBasePath,
      final String content) {
    log.debug("Rewriting links to basePath " + remoteBasePath);
    final boolean isUrlEncodingNeeded = newLinkPrefix.indexOf('?') >= 0;
    final Pattern pattern =
        Pattern.compile(linkPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    final Matcher m = pattern.matcher(content);

    // determines if the link is absolute
    final StringBuffer buffer = new StringBuffer();
    int lastEnd = 0;
    while (m.find()) {
      final String link = m.group(linkIndex).trim();
      if (link.startsWith("data")) {
        continue;
      }

      // is the link relative?
      if (!link.toLowerCase().startsWith("http")) {
        // create relative URL
        String newURI = null;
        if (isAbsoluteUrl(link)) {
          URI remoteURI;
          try {
            remoteURI = new URI(remoteBasePath);
          } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI syntax "
                + remoteBasePath, e);
          }
          final String addressBaseHost =
              remoteURI.getScheme() + "://" + remoteURI.getHost().toString()
                  + ":" + getPort(remoteURI);
          newURI =
              newLinkPrefix
                  + encode(isUrlEncodingNeeded, addressBaseHost + link);
        } else {
          newURI =
              newLinkPrefix
                  + encode(isUrlEncodingNeeded, remoteBasePath + link);
        }
        // append the string between links
        buffer.append(content.substring(lastEnd, m.start(linkIndex)));

        // append new link
        buffer.append(newURI.trim());

        // update cursor
        lastEnd = m.end(linkIndex);
      }
    }
    // append the end of file
    buffer.append(content.substring(lastEnd));
    return buffer.toString();
  }

  private int getPort(final URI remoteURI) {
    final int port = remoteURI.getPort();
    if (port > 0) {
      return port;
    } else {
      return remoteURI.getScheme().equalsIgnoreCase("http") ? 80 : (remoteURI
          .getScheme().equalsIgnoreCase("https") ? 443 : 0);
    }
  }

  private String encode(final boolean escapeRequired, final String string) {
    try {
      return escapeRequired ? URLEncoder.encode(string, "UTF-8") : string;
    } catch (final UnsupportedEncodingException e) {
      // Condition that will never occur as UTF-8 is hardcoded here
      e.printStackTrace();
      return string;
    }
  }
}
