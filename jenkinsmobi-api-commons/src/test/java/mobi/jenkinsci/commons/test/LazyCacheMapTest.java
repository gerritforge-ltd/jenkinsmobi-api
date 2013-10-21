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
package mobi.jenkinsci.commons.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import mobi.jenkinsci.net.LazyCacheMap;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

public class LazyCacheMapTest {

  public static class DateFormatCache extends LazyCacheMap<Long, String> {
    
    public static class Loader implements LazyCacheMap.Loader<Long, String>{
      @Override
      public String load(Long key) throws RemoteException {
        return new SimpleDateFormat().format(new Date(key));
      }
    }

    @Inject
    public DateFormatCache(long cacheTTL) {
      super(new Loader(), cacheTTL);
    }
  }

  @Inject
  DateFormatCache dateFormatCache;
  
  @Test 
  public void dateFormatCacheNotNull() {
    assertThat(dateFormatCache, is(notNullValue()));
  }
}
