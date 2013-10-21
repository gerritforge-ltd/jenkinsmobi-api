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

package mobi.jenkinsci.ci.client;

public class UrlParser {

	public static String getQueryPath(String url) {

		String result = null;
		// http://xxxxx:yy/...
		// https://xxxxx:yy/...
		if (url.startsWith("https")) {

			url = url.substring(8); //remove https:// (8 ch len)
		} else {
			url = url.substring(7); //remove http:// (7 ch len)
		}

		// now it is: xxxxx:yy/...
		int slashIndex = url.indexOf('/');

		if (slashIndex != -1) {

			result = url.substring(slashIndex);
		} else {

			result = "/";
		}

		return result;
	}

	public static String getDomainName(String url) {

		String result = null;
		// http://xxxxx:yy/...
		// https://xxxxx:yy/...
		if (url.startsWith("https")) {

			url = url.substring(8);
		} else {
			url = url.substring(7);
		}

		// now it is: xxxxx:yy/...
		int pointIndex = url.indexOf(':');
		int slashIndex = url.indexOf('/');

		if (pointIndex != -1) {
			// no point
			result = url.substring(0, pointIndex);

		} else if (slashIndex == -1) {
			// no slash and no point
			result = url;
		} else {
			// slash and no point
			result = url.substring(0, slashIndex);
		}

		return result;
	}

	public static int getPort(String url) {

		int result = 80;
		// http://xxxxx:yy/...
		// https://xxxxx:yy/...
		boolean isHttpsProtocol = false;
		if (url.startsWith("https")) {
			url = url.substring(8);
			isHttpsProtocol = true;
		} else {
			url = url.substring(7);
			isHttpsProtocol = false;
		}

		// now it is: xxxxx:yy/...

		int pointIndex = url.indexOf(':');
		int slashIndex = url.indexOf('/');

		// String resular = "[http]|[https]://[a-zA-Z0-1_\\-\\.]*[:]*"

		if (pointIndex != -1) {
			if (slashIndex == -1) {
				// no slash and :
				String portStr = url.substring(pointIndex + 1);
				result = Integer.parseInt(portStr);
			} else {
				String portStr = url.substring(pointIndex + 1, slashIndex);
				result = Integer.parseInt(portStr);
			}
		} else {

			if (isHttpsProtocol) {

				result = 443;
			} else {

				result = 80;
			}
		}

		return result;
	}

	public static String getProtocol(String url) {

		if (url.startsWith("https")) {

			return "https";
		} else {

			return "http";
		}
	}
}
