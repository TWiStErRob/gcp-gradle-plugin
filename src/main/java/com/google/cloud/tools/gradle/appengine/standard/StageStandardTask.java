/*
 * Copyright 2016 Google LLC. All Rights Reserved.
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

package com.google.cloud.tools.gradle.appengine.standard;

import javax.inject.Inject;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.operations.AppCfg;
import com.google.cloud.tools.gradle.appengine.core.CloudSdkOperations;
import org.gradle.api.DefaultTask;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

/** Stage App Engine Standard Environment applications for deployment. */
public abstract class StageStandardTask extends DefaultTask {

  private StageStandardExtension stageStandardExtension;
  private AppCfg appCfg;

  @Inject
  public abstract FileOperations getFileOperations();

  @Nested
  public StageStandardExtension getStageStandardExtension() {
    return stageStandardExtension;
  }

  public void setStageStandardExtension(StageStandardExtension stageStandardExtension) {
    this.stageStandardExtension = stageStandardExtension;
  }

  public void setAppCfg(AppCfg appCfg) {
    this.appCfg = appCfg;
  }

  /** Task entrypoint : stage the standard app. */
  @TaskAction
  public void stageAction() throws AppEngineException {
    getFileOperations().delete(stageStandardExtension.getStagingDirectory());
    appCfg
        .newStaging(CloudSdkOperations.getDefaultHandler(getLogger()))
        .stageStandard(stageStandardExtension.toStageStandardConfiguration());
  }
}
