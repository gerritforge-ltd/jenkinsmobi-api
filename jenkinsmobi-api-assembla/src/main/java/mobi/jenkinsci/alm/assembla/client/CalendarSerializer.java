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
package mobi.jenkinsci.alm.assembla.client;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CalendarSerializer implements JsonSerializer<Calendar>,
    JsonDeserializer<Calendar> {
  private static final SimpleDateFormat calendarFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final Pattern ASSEMBLA_TZ_PATTERN = Pattern.compile("\\d\\d\\:\\d\\d\\:\\d\\d([\\+\\-]\\d\\d\\:\\d\\d)");
  private static final Pattern DATEFMT_TZ_PATTERN = Pattern.compile("\\d\\d\\:\\d\\d\\:\\d\\d([\\+\\-]\\d\\d\\\\d\\d)");

  @Override
  public Calendar deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    Calendar cal = Calendar.getInstance();
    String calString = json.getAsString();
    calString = fixTZ(calString); 
    try {
      cal.setTime(calendarFormat.parse(calString));
    } catch (ParseException e) {
      throw new JsonParseException("Unparseable date " + calString, e);
    }
    return cal;
  }

  private String fixTZ(String calString) {
    if (calString.endsWith("Z")) {
      return calString.substring(0, calString.length() - 1) + "+0000";
    }
    
    Matcher tzMatch = ASSEMBLA_TZ_PATTERN.matcher(calString);
    if(tzMatch.find()) {
      return calString.substring(0, tzMatch.start(1)) + tzMatch.group(1).replaceFirst(":", "");
    }

    tzMatch = DATEFMT_TZ_PATTERN.matcher(calString);
    if(!tzMatch.find()) {
      return calString + "+0000";
    }
    
    return calString;
  }

  @Override
  public JsonElement serialize(Calendar src, Type typeOfSrc,
      JsonSerializationContext context) {
    return new JsonPrimitive(calendarFormat.format(src.getTime()));
  }
}