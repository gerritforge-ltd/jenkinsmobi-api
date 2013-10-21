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
package mobi.jenkinsci.alm.trello.nodes;

import java.util.GregorianCalendar;
import java.util.HashMap;

public class Card extends TrelloObject {
  public class CheckItemState {
    public String idCheckItem;
    public String state;
  }
  
  public HashMap<String, String> badges;
  public CheckItemState[] checkItemStates;
  public boolean closed;
  public GregorianCalendar dateLastActivity;
  public String desc;
  public String due;
  public String idBoard;
  public String[] idChecklists;
  public String idList;
  public String[] idMembers;
  public String[] idMembersVoted;
  public int idShort;
  public String idAttachmentCover;
  public boolean manualCoverAttachment;
  public String[] labels;
  public float pos;
  public String shortUrl;
  public boolean subscribed;
  public String url;
}
