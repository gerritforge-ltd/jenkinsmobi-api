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

import java.io.IOException;

public class LoginFailedException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int reasonCode = 0;
	private String reasonMsg = null;

	public LoginFailedException(String msg, int lastHttpStatusCode,
			String lastHttpStatusMsg) {

		super(msg + " - " + lastHttpStatusMsg);
		this.reasonCode = lastHttpStatusCode;
		this.reasonMsg = lastHttpStatusMsg;
	}

	public LoginFailedException() {
    // TODO Auto-generated constructor stub
  }

  public int getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(int reasonCode) {
		this.reasonCode = reasonCode;
	}

	public String getReasonMsg() {
		return reasonMsg;
	}

	public void setReasonMsg(String reasonMsg) {
		this.reasonMsg = reasonMsg;
	}
}
