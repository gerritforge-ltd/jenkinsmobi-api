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
package mobi.jenkinsci.ci.model;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class JobCalendar extends GregorianCalendar {
  private static final long serialVersionUID = 1L;
  private long daysSinceEpoch;
  private long weeksSinceEpoch;
  
  public JobCalendar(long timeMsec) {
    super();
    setTimeInMillis(timeMsec);
    daysSinceEpoch = timeMsec / (24*3600*1000L);
    weeksSinceEpoch = (daysSinceEpoch-3 /* 1st of Jan 1970 was a Thursday, 3 days to end of the week */) / 7;
  }

  public long compareDays(JobCalendar nowCal) {
    return this.daysSinceEpoch - nowCal.daysSinceEpoch;
  }

  public long compareWeeks(JobCalendar nowCal) {
    return this.weeksSinceEpoch - nowCal.weeksSinceEpoch;
  }

  public int compareMonths(JobCalendar nowCal) {
    int thisYear = get(Calendar.YEAR);
    int thisMonth = get(Calendar.MONTH);
    
    int nowCalYear = nowCal.get(Calendar.YEAR);
    int nowCalMonth = nowCal.get(Calendar.MONTH);
    
    return (thisYear * 12 + thisMonth) - (nowCalYear * 12 + nowCalMonth);
  }

}
