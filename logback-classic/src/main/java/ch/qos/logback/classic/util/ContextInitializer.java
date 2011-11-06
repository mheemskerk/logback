/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.gaffer.GafferUtil;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;

// contributors
// Ted Graham, Matt Fowles, see also http://jira.qos.ch/browse/LBCORE-32

/**
 * This class contains logback's logic for automatic configuration
 *
 * @author Ceki Gulcu
 */
public class ContextInitializer {

  final public static String GROOVY_AUTOCONFIG_FILE = "logback.groovy";
  final public static String AUTOCONFIG_FILE = "logback.xml";
  final public static String TEST_AUTOCONFIG_FILE = "logback-test.xml";
  final public static String CONFIG_FILE_PROPERTY = "logback.configurationFile";
  final public static String STATUS_LISTENER_CLASS = "logback.statusListenerClass";
  final public static String SYSOUT = "SYSOUT";

  final LoggerContext loggerContext;

  public ContextInitializer(LoggerContext loggerContext) {
    this.loggerContext = loggerContext;
  }

  public void configureByResource(URL url) throws JoranException {
    if (url == null) {
      throw new IllegalArgumentException("URL argument cannot be null");
    }
    if (url.toString().endsWith("groovy")) {
      if (EnvUtil.isGroovyAvailable()) {
        // avoid directly referring to GafferConfigurator so as to avoid
        // loading  groovy.lang.GroovyObject . See also http://jira.qos.ch/browse/LBCLASSIC-214
        GafferUtil.runGafferConfiguratorOn(loggerContext, this, url);
      } else {
        StatusManager sm = loggerContext.getStatusManager();
        sm.add(new ErrorStatus("Groovy classes are not available on the class path. ABORTING INITIALIZATION.",
                loggerContext));
      }
    }
    if (url.toString().endsWith("xml")) {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(loggerContext);
      configurator.doConfigure(url);
    }
  }

  void joranConfigureByResource(URL url) throws JoranException {
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(loggerContext);
    configurator.doConfigure(url);
  }

  private URL findConfigFileURLFromSystemProperties(ClassLoader classLoader, boolean updateStatus) {
    String logbackConfigFile = OptionHelper.getSystemProperty(CONFIG_FILE_PROPERTY);
    if (logbackConfigFile != null) {
      URL result = null;
      try {
        result = new URL(logbackConfigFile);
        return result;
      } catch (MalformedURLException e) {
        // so, resource is not a URL:
        // attempt to get the resource from the class path
        result = Loader.getResource(logbackConfigFile, classLoader);
        if (result != null) {
          return result;
        }
        File f = new File(logbackConfigFile);
        if (f.exists() && f.isFile()) {
          try {
            result = f.toURI().toURL();
            return result;
          } catch (MalformedURLException e1) {
          }
        }
      } finally {
        if (updateStatus) {
          statusOnResourceSearch(logbackConfigFile, classLoader, result);
        }
      }
    }
    return null;
  }

  public URL findURLOfDefaultConfigurationFile(boolean updateStatus) {
    ClassLoader myClassLoader = Loader.getClassLoaderOfObject(this);
    URL url = findConfigFileURLFromSystemProperties(myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    url = getResource(GROOVY_AUTOCONFIG_FILE, myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    url = getResource(TEST_AUTOCONFIG_FILE, myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    return getResource(AUTOCONFIG_FILE, myClassLoader, updateStatus);
  }

  private URL getResource(String filename, ClassLoader myClassLoader, boolean updateStatus) {
    URL url = Loader.getResource(filename, myClassLoader);
    if (updateStatus) {
      statusOnResourceSearch(filename, myClassLoader, url);
    }
    return url;
  }

  public void autoConfig() throws JoranException {
    StatusListenerConfigHelper.installIfAsked(loggerContext);
    URL url = findURLOfDefaultConfigurationFile(true);
    if (url != null) {
      configureByResource(url);
    } else {
      BasicConfigurator.configure(loggerContext);
    }
  }

  private void multiplicityWarning(String resourceName, ClassLoader classLoader) {
    List<URL> urlList = null;
    StatusManager sm = loggerContext.getStatusManager();
    try {
      urlList = Loader.getResourceOccurenceCount(resourceName, classLoader);
    } catch (IOException e) {
      sm.add(new ErrorStatus("Failed to get url list for resource [" + resourceName + "]",
              loggerContext, e));
    }
    if (urlList != null && urlList.size() > 1) {
      sm.add(new WarnStatus("Resource [" + resourceName + "] occurs multiple times on the classpath.",
              loggerContext));
      for (URL url : urlList) {
        sm.add(new WarnStatus("Resource [" + resourceName + "] occurs at [" + url.toString() + "]",
                loggerContext));
      }
    }
  }

  private void statusOnResourceSearch(String resourceName, ClassLoader classLoader, URL url) {
    StatusManager sm = loggerContext.getStatusManager();
    if (url == null) {
      sm.add(new InfoStatus("Could NOT find resource [" + resourceName + "]",
              loggerContext));
      if(classLoader == null) {
        sm.add(new InfoStatus("Could NOT search for resource [" + resourceName + "] via ClassLoader since SecurityManager did not allow access.",
                loggerContext));
      }
    } else {
      sm.add(new InfoStatus("Found resource [" + resourceName + "] at [" + url.toString() + "]",
              loggerContext));
      multiplicityWarning(resourceName, classLoader);
    }
  }

}
