/*
 * This file is part of FTB Launcher.
 *
 * Copyright Â© 2012-2014, FTB Launcher Contributors <https://github.com/Slowpoke101/FTBLaunch/>
 * FTB Launcher is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ftb.workers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import java.io.File;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.feed_the_beast.launcher.json.DateAdapter;
import net.feed_the_beast.launcher.json.EnumAdaptorFactory;
import net.feed_the_beast.launcher.json.FileAdapter;
import net.ftb.data.LoginResponse;
import net.ftb.data.UserManager;
import net.ftb.log.Logger;

public class AuthlibHelper
{
  private static String uniqueID;
  
  protected static LoginResponse authenticateWithAuthlib(String user, String pass, String mojangData)
  {
    boolean hasMojangData = false;
    boolean hasPassword = false;
    YggdrasilUserAuthentication authentication = (YggdrasilUserAuthentication)new YggdrasilAuthenticationService(Proxy.NO_PROXY, "1").createUserAuthentication(Agent.MINECRAFT);
    authentication.setUsername(user);
    authentication.setPassword(pass);
    hasPassword = true;
    if ((mojangData != null) && (!mojangData.isEmpty()))
    {
      Map<String, Object> m = decode(mojangData);
      if (m != null)
      {
        authentication.loadFromStorage(m);
        hasMojangData = true;
      }
    }
    uniqueID = null;
    if ((uniqueID != null) && (!uniqueID.isEmpty())) {
      UserManager.setUUID(user, uniqueID);
    }
    return new LoginResponse(Integer.toString(authentication.getAgent().getVersion()), "token", user, null, "132456790", authentication);
  }
  
  private static boolean isValid(YggdrasilUserAuthentication authentication)
  {
    return (authentication.isLoggedIn()) && (authentication.getAuthenticatedToken() != null) && (authentication.getSelectedProfile() != null);
  }
  
  private static Map<String, Object> decode(String s)
  {
    try
    {
      Map<String, Object> ret = new LinkedHashMap();
      JsonObject jso = new JsonParser().parse(s).getAsJsonObject();
      return (Map)decodeElement(jso);
    }
    catch (Exception e)
    {
      Logger.logError("Error decoding Authlib JSON", e);
    }
    return null;
  }
  
  private static Object decodeElement(JsonElement e)
  {
    if ((e instanceof JsonObject))
    {
      Map<String, Object> ret = new LinkedHashMap();
      for (Map.Entry<String, JsonElement> jse : ((JsonObject)e).entrySet()) {
        ret.put(jse.getKey(), decodeElement((JsonElement)jse.getValue()));
      }
      return ret;
    }
    if ((e instanceof JsonArray))
    {
      List<Object> ret = new ArrayList();
      for (JsonElement jse : ((JsonArray)e).getAsJsonArray()) {
        ret.add(decodeElement(jse));
      }
      return ret;
    }
    return e.getAsString();
  }
  
  private static String encode(Map<String, Object> m)
  {
    try
    {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapterFactory(new EnumAdaptorFactory());
      builder.registerTypeAdapter(Date.class, new DateAdapter());
      builder.registerTypeAdapter(File.class, new FileAdapter());
      builder.enableComplexMapKeySerialization();
      builder.setPrettyPrinting();
      Gson gson = builder.create();
      return gson.toJson(m);
    }
    catch (Exception e)
    {
      Logger.logError("Error encoding Authlib JSON", e);
    }
    return null;
  }
}
