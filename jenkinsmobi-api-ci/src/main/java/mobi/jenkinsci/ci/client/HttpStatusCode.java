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

public interface HttpStatusCode {

	public static String STATUS_ABORTED_FOR_TIMEOUT_DESC = "Connection timeout";
	public static final int STATUS_ABORTED_FOR_TIMEOUT = 504; //Gateway Timeout
	
	public static String STATUS_200 = "OK";
	public static String STATUS_500 = "Internal server error";

	  public final static int HTTP_OK = 200;
	  public final static int HTTP_INTERNAL_ERROR = 500;
	
	//400 -> 417
	public static String[] HTTP_ERROR_DESC_400_417 = {
			"Bad Request Syntax error in the client's request.",
			"Unauthorized", "Payment Required", "Forbidden", "Resource Not Found",
			"Method Not Allowed", "Not Acceptable",
			"Proxy Authentication Required", "Request Timeout", "Conflict",
			"URI no longer exists", "Length Required", "Precondition Failed",
			"Request Entity Too Large", "Request-URI Too Long",
			"Unsupported Media Type", "Requested Range Not Satisfiable",
			"Expectation Failed" };

}
