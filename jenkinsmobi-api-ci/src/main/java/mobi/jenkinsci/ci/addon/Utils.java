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

package mobi.jenkinsci.ci.addon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import mobi.jenkinsci.ci.client.JenkinsConfig;
import mobi.jenkinsci.model.ItemNode;

import com.google.common.base.Objects;

public class Utils {
  
  private static final HashMap<String, String> ICONS_BY_LABEL = new HashMap<String, String>();

  static {
    ICONS_BY_LABEL.put("*", "icons/disabled.png");
    ICONS_BY_LABEL.put("blue", "icons/stable.png");
    ICONS_BY_LABEL.put("success", "icons/stable.png");
    ICONS_BY_LABEL.put("yellow", "icons/unstable.png");
    ICONS_BY_LABEL.put("red", "icons/alert.png");
    ICONS_BY_LABEL.put("aborted", "icons/disabled.png");
  }
  
  public static String parseHudsonDate(String elementValue) {

    // 2010-07-21T17:03:04.685020Z

    StringBuilder result = new StringBuilder();

    result.append(elementValue.substring(0, 10));
    result.append(" ");
    result.append(elementValue.substring(11, 23));

    return result.toString();
  }

  public static String convertStreamToString(InputStream is) throws IOException {
    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     */
    if (is != null) {
      StringBuilder sb = new StringBuilder();
      String line;

      try {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(is, "UTF-8"));
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
      } finally {
        is.close();
      }
      return sb.toString();
    } else {
      return "";
    }
  }

  public static String fixUrl(String _url, JenkinsConfig config) {

    String result = _url;

    if (config.isOverrideHudsonUrl()) {
      int protocolType = -1;
      boolean applyHostnameFix = false;

      if (result.startsWith("http://")) {

        protocolType = 0;
      } else if (result.startsWith("https://")) {

        protocolType = 1;
      }

      applyHostnameFix = protocolType > -1;

      if (applyHostnameFix) {

        if (!result.startsWith(config.getUrl())) {

          int range = -1;
          int len = -1;

          if (config.getSuffix() != null && config.getSuffix().length() > 0) {

            range = result.indexOf("/" + config.getSuffix() + "/");
            len = config.getSuffix().length() + 2;

          } else if (config.getPortNumber() != 0) {

            range = result.indexOf(":" + config.getPortNumber() + "/");
            len = ("" + config.getPortNumber()).length() + 2;
          } else {

            range = result.indexOf(config.getUrl() + "/");
            len = config.getUrl().length() + 1;
          }

          if (range > -1 && len > -1) {

            result = result.substring(range + len);
            result = config.getUrl() + "/" + result;

          }

        }
      }
    }

    return result;
  }
  
  public static void setIconByLabel(ItemNode node, String label) {
    String iconName =
        Objects.firstNonNull(
            ICONS_BY_LABEL.get(label.toLowerCase().split("_")[0]),
            ICONS_BY_LABEL.get("*"));
    node.setIcon(node.getPath() + "?image=" + iconName);
  }

}
