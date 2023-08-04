/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.cloud.tools.gradle.appengine.core;

import com.google.cloud.tools.managedcloudsdk.ConsoleListener;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;

public class DownloadCloudSdkTaskConsoleListener implements ConsoleListener {
  private Logger logger;

  public DownloadCloudSdkTaskConsoleListener(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void console(String rawString) {
    // Cloud SDK installation is somewhat verbose, so we'd like to log it at the LIFECYCLE level.
    // Gradle's logging api doesn't let us log without adding a newline at the end of the message,
    // so we need to use System.out.print() to display rawString in its original format. The problem
    // is that Gradle redirects standard output to its logging system at the QUIET level. So, in
    // order to print to LIFECYCLE without adding a newline, we just check that our desired level
    // is enabled before trying to print.
    if (logger.isEnabled(LogLevel.LIFECYCLE)) {
      System.out.print(rawString);
    }
  }
}
