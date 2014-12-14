/*
 * This file is part of FTB Launcher.
 *
 * Copyright © 2012-2014, FTB Launcher Contributors <https://github.com/Slowpoke101/FTBLaunch/>
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

import javax.swing.SwingWorker;

import net.ftb.gui.LaunchFrame;
import net.ftb.log.Logger;
import net.ftb.util.Benchmark;
import net.ftb.util.DownloadUtils;

public class AuthlibDLWorker
  extends SwingWorker<Boolean, Void>
{
  protected String status;
  protected String reqVersion;
  protected File binDir;
  protected String authlibVersion;
  protected URL jarURLs;
  
  public AuthlibDLWorker(String DLFolder, String authver)
  {
    this.binDir = new File(DLFolder);
    this.authlibVersion = authver;
    this.status = "";
  }
  
  protected Boolean doInBackground()
  {
    Benchmark.start("Authlib");
    Logger.logDebug("Loading Authlib...");
    if (!this.binDir.exists()) {
      this.binDir.mkdirs();
    }
    if (!downloadJars())
    {
      Logger.logError("Authlib Download Failed");
      if (!new File(this.binDir + File.separator + "authlib-" + this.authlibVersion + ".jar").exists()) {
        return Boolean.valueOf(false);
      }
      Logger.logInfo("Local Authlib copy exists: trying to load it anyway");
    }
    setStatus("Adding Authlib to Classpath");
    Logger.logInfo("Adding Authlib to Classpath");
    return Boolean.valueOf(addToClasspath(this.binDir + File.separator + "authlib-" + this.authlibVersion + ".jar"));
  }
  
  protected boolean addToClasspath(String location)
  {
    File f = new File(location);
    try
    {
      if (f.exists())
      {
        addURL(f.toURI().toURL());
        getClass();Class.forName("com.mojang.authlib.exceptions.AuthenticationException");
        getClass();Class.forName("com.mojang.authlib.Agent");
        getClass();Class.forName("com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService");
        getClass();Class.forName("com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication");
      }
      else
      {
        Logger.logError("Authlib file does not exist");
      }
    }
    catch (Throwable t)
    {
      Logger.logError(t.getMessage(), t);
      return false;
    }
    LaunchFrame.canUseAuthlib = true;
    Benchmark.logBenchAs("Authlib", "Authlib DL Worker Init");
    return true;
  }
  
  public void addURL(URL u)
    throws IOException
  {
    URLClassLoader sysloader = (URLClassLoader)getClass().getClassLoader();
    Class sysclass = URLClassLoader.class;
    try
    {
      Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
      method.setAccessible(true);
      method.invoke(sysloader, new Object[] { u });
    }
    catch (Throwable t)
    {
      Logger.logWarn(t.getMessage(), t);
      throw new IOException("Error, could not add URL to system classloader");
    }
  }
  
  protected boolean downloadJars()
  {
    try
    {
      this.jarURLs = new URL("https://libraries.minecraft.net/com/mojang/authlib/" + this.authlibVersion + "/authlib-" + this.authlibVersion + ".jar");
    }
    catch (MalformedURLException e)
    {
      Logger.logError(e.getMessage(), e);
      return false;
    }
    double totalDownloadSize = 0.0D;double totalDownloadedSize = 0.0D;
    int[] fileSizes = new int[1];
    String hash = "";
    for (int i = 0; i < 1; i++) {
      try
      {
        HttpURLConnection conn = (HttpURLConnection)this.jarURLs.openConnection();
        conn.setRequestProperty("Cache-Control", "no-transform");
        conn.setRequestMethod("HEAD");
        conn.connect();
        hash = conn.getHeaderField("ETag").replace("\"", "");
        fileSizes[i] = conn.getContentLength();
        conn.disconnect();
        totalDownloadSize += fileSizes[i];
      }
      catch (Exception e)
      {
        Logger.logWarn("Authlib checksum download failed", e);
        return false;
      }
    }
    boolean downloadSuccess = false;
    if ((hash != null) && (!hash.equals("")) && (new File(this.binDir, getFilename(this.jarURLs)).exists())) {
      try
      {
        if (hash.toLowerCase().equals(DownloadUtils.fileMD5(new File(this.binDir, getFilename(this.jarURLs))).toLowerCase()))
        {
          Logger.logInfo("Local Authlib Version is good, skipping Download");
          return true;
        }
      }
      catch (Exception e1) {}
    }
    int attempt = 0;
    int attempts = 5;
    while ((!downloadSuccess) && (attempt < 5)) {
      try
      {
        attempt++;
        Logger.logDebug("Connecting.. Try " + attempt + " of " + 5 + " for: " + this.jarURLs.toURI());
        URLConnection dlConnection = this.jarURLs.openConnection();
        if ((dlConnection instanceof HttpURLConnection))
        {
          dlConnection.setRequestProperty("Cache-Control", "no-cache, no-transform");
          dlConnection.connect();
        }
        String jarFileName = getFilename(this.jarURLs);
        if (new File(this.binDir, jarFileName).exists()) {
          new File(this.binDir, jarFileName).delete();
        }
        InputStream dlStream = dlConnection.getInputStream();
        FileOutputStream outStream;
        try
        {
          outStream = new FileOutputStream(new File(this.binDir, jarFileName));
        }
        catch (Exception e)
        {
          downloadSuccess = false;
          Logger.logError("Error while opening authlib file for writing. Check your FTB installation location write access", e);
          break;
        }
        setStatus("Downloading " + jarFileName + "...");
        byte[] buffer = new byte[24000];
        
        int currentDLSize = 0;
        int readLen;
        while ((readLen = dlStream.read(buffer, 0, buffer.length)) != -1)
        {
          outStream.write(buffer, 0, readLen);
          currentDLSize += readLen;
          totalDownloadedSize += readLen;
          int prog = (int) ((totalDownloadedSize / totalDownloadSize) * 100);
          if (prog > 100) {
            prog = 100;
          } else if (prog < 0) {
            prog = 0;
          }
          setProgress(prog);
        }
        dlStream.close();
        outStream.close();
        if (((dlConnection instanceof HttpURLConnection)) && ((currentDLSize == fileSizes[0]) || (fileSizes[0] <= 0))) {
          downloadSuccess = true;
        }
      }
      catch (Exception e)
      {
        downloadSuccess = false;
        Logger.logWarn("Connection failed, trying again", e);
      }
    }
    return downloadSuccess;
  }
  
  protected String getFilename(URL url)
  {
    String string = url.getFile();
    if (string.contains("?")) {
      string = string.substring(0, string.indexOf('?'));
    }
    return string.substring(string.lastIndexOf('/') + 1);
  }
  
  protected void setStatus(String newStatus)
  {
    String oldStatus = this.status;
    this.status = newStatus;
    firePropertyChange("status", oldStatus, newStatus);
  }
  
  public String getStatus()
  {
    return this.status;
  }
}