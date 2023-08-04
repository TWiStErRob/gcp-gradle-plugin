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

package com.google.cloud.tools.gradle.appengine.appyaml;

import com.google.cloud.tools.appengine.AppEngineException;
import com.google.cloud.tools.appengine.operations.AppYamlProjectStaging;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;

/** Stage App Engine app.yaml based applications for deployment. */
public abstract class StageAppYamlTask extends DefaultTask {

  @Inject
  public abstract FileOperations getFileOperations();

  private StageAppYamlExtension appYamlExtension;

  @Nested
  public StageAppYamlExtension getStagingExtension() {
    return appYamlExtension;
  }

  public void setStagingConfig(StageAppYamlExtension stagingConfig) {
    this.appYamlExtension = stagingConfig;
  }

  /** Task entrypoint : Stage the app.yaml based application. */
  @TaskAction
  public void stageAction() throws AppEngineException {
    getFileOperations().delete(appYamlExtension.getStagingDirectory());
    getFileOperations().mkdir(appYamlExtension.getStagingDirectory().getAbsolutePath());

    AppYamlProjectStaging staging = new AppYamlProjectStaging();
    staging.stageArchive(appYamlExtension.toAppYamlProjectStageConfiguration());
  }
}
