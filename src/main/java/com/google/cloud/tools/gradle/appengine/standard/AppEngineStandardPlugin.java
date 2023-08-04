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

import com.google.cloud.tools.appengine.operations.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.gradle.appengine.core.AppEngineCorePluginConfiguration;
import com.google.cloud.tools.gradle.appengine.core.CloudSdkOperations;
import com.google.cloud.tools.gradle.appengine.core.DeployAllTask;
import com.google.cloud.tools.gradle.appengine.core.DeployExtension;
import com.google.cloud.tools.gradle.appengine.core.DeployTargetResolver;
import com.google.cloud.tools.gradle.appengine.core.DeployTask;
import com.google.cloud.tools.gradle.appengine.core.ToolsExtension;
import com.google.cloud.tools.gradle.appengine.util.GradleCompatibility;
import com.google.common.base.Strings;
import java.io.File;
import javax.inject.Inject;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.tasks.bundling.War;

/** Plugin definition for App Engine standard environments. */
public abstract class AppEngineStandardPlugin implements Plugin<Project> {

  public static final String APP_ENGINE_STANDARD_TASK_GROUP = "App Engine Standard environment";
  public static final String EXPLODE_WAR_TASK_NAME = "explodeWar";
  public static final String STAGE_TASK_NAME = "appengineStage";
  public static final String RUN_TASK_NAME = "appengineRun";
  public static final String START_TASK_NAME = "appengineStart";
  public static final String STOP_TASK_NAME = "appengineStop";

  public static final String STAGED_APP_DIR_NAME = "staged-app";
  public static final String DEV_APP_SERVER_OUTPUT_DIR_NAME = "dev-appserver-out";

  public static final String STAGE_EXTENSION = "stage";
  public static final String RUN_EXTENSION = "run";

  private Project project;
  private CloudSdkOperations cloudSdkOperations;
  private AppEngineStandardExtension appengineExtension;
  private AppEngineCorePluginConfiguration appEngineCorePluginConfiguration;
  private RunExtension runExtension;
  private StageStandardExtension stageExtension;
  private File explodedWarDir;

  @Inject
  protected abstract FileOperations getFileOperations();

  @Override
  public void apply(Project project) {
    this.project = project;
    project.getPluginManager().apply(WarPlugin.class);
    appengineExtension =
        project.getExtensions().create("appengine", AppEngineStandardExtension.class);
    appengineExtension.createSubExtensions(getFileOperations());

    appEngineCorePluginConfiguration = new AppEngineCorePluginConfiguration();
    appEngineCorePluginConfiguration.configureCoreProperties(
        project, appengineExtension, APP_ENGINE_STANDARD_TASK_GROUP, true);

    explodedWarDir = new File(project.getBuildDir(), "exploded-" + project.getName());

    configureExtensions();

    createExplodedWarTask();
    createStageTask();
    createRunTasks();
  }

  private void configureExtensions() {

    // create the run extension and set defaults.
    runExtension = appengineExtension.getRun();
    runExtension.setStartSuccessTimeout(20);
    runExtension.setServices(explodedWarDir);
    runExtension.setServerVersion("1");

    // create the stage extension and set defaults.
    stageExtension = appengineExtension.getStage();
    File defaultStagedAppDir = new File(project.getBuildDir(), STAGED_APP_DIR_NAME);
    stageExtension.setSourceDirectory(explodedWarDir);
    stageExtension.setStagingDirectory(defaultStagedAppDir);

    project.afterEvaluate(
        project -> {
          // tools extension required to initialize cloudSdkOperations
          ToolsExtension tools = appengineExtension.getTools();
          try {
            cloudSdkOperations =
                new CloudSdkOperations(tools.getCloudSdkHome(), null, tools.getVerbosity());
          } catch (CloudSdkNotFoundException ex) {
            // this should be caught in AppEngineCorePluginConfig before it can ever reach here.
            throw new GradleException("Could not find CloudSDK: ", ex);
          }

          DeployExtension deploy = appengineExtension.getDeploy();
          if (deploy.getAppEngineDirectory() == null) {
            deploy.setAppEngineDirectory(
                new File(stageExtension.getStagingDirectory(), "WEB-INF/appengine-generated"));
          }

          DeployAllTask deployAllTask =
              (DeployAllTask)
                  project
                      .getTasks()
                      .getByName(AppEngineCorePluginConfiguration.DEPLOY_ALL_TASK_NAME);
          deployAllTask.setStageDirectory(stageExtension.getStagingDirectory());
          deployAllTask.setDeployExtension(deploy);

          DeployTask deployTask =
              (DeployTask)
                  project.getTasks().getByName(AppEngineCorePluginConfiguration.DEPLOY_TASK_NAME);
          deployTask.setDeployConfig(deploy);
          deployTask.setAppYaml(stageExtension.getStagingDirectory().toPath().resolve("app.yaml"));

          // configure the runExtension's project parameter
          // assign the run projectId to the deploy projectId if none is specified
          if (Strings.isNullOrEmpty(runExtension.getProjectId())) {
            runExtension.setProjectId(deploy.getProjectId());
          }
          runExtension.setDeployTargetResolver(new DeployTargetResolver(cloudSdkOperations));
        });
  }

  private void createExplodedWarTask() {
    project
        .getTasks()
        .create(
            EXPLODE_WAR_TASK_NAME,
            ExplodeWarTask.class,
            explodeWar -> {
              explodeWar.setExplodedAppDirectory(explodedWarDir);
              explodeWar.dependsOn(WarPlugin.WAR_TASK_NAME);
              explodeWar.setGroup(APP_ENGINE_STANDARD_TASK_GROUP);
              explodeWar.setDescription("Explode a war into a directory");

              project.afterEvaluate(
                  project -> {
                    War war = (War) project.getTasks().getByPath(WarPlugin.WAR_TASK_NAME);
                    explodeWar.setWarFile(GradleCompatibility.getArchiveFile(war));
                  });
            });
    project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(EXPLODE_WAR_TASK_NAME);
  }

  private void createStageTask() {
    project
        .getTasks()
        .withType(StageStandardTask.class)
        .whenTaskAdded(
            stageStandardTask ->
                project.afterEvaluate(
                    ignored -> stageStandardTask.setAppCfg(cloudSdkOperations.getAppcfg())));

    StageStandardTask stageTask =
        project
            .getTasks()
            .create(
                STAGE_TASK_NAME,
                StageStandardTask.class,
                stageTask1 -> {
                  stageTask1.setGroup(APP_ENGINE_STANDARD_TASK_GROUP);
                  stageTask1.setDescription(
                      "Stage an App Engine standard environment application for deployment");
                  stageTask1.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME);

                  project.afterEvaluate(
                      project -> {
                        stageTask1.setStageStandardExtension(stageExtension);
                      });
                });

    // All deployment tasks depend on the stage task.
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_TASK_NAME)
        .dependsOn(stageTask);
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_CRON_TASK_NAME)
        .dependsOn(stageTask);
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_DISPATCH_TASK_NAME)
        .dependsOn(stageTask);
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_DOS_TASK_NAME)
        .dependsOn(stageTask);
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_INDEX_TASK_NAME)
        .dependsOn(stageTask);
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_QUEUE_TASK_NAME)
        .dependsOn(stageTask);
    project
        .getTasks()
        .getByName(AppEngineCorePluginConfiguration.DEPLOY_ALL_TASK_NAME)
        .dependsOn(stageTask);
  }

  private void createRunTasks() {
    project
        .getTasks()
        .create(
            RUN_TASK_NAME,
            DevAppServerRunTask.class,
            runTask -> {
              runTask.setGroup(APP_ENGINE_STANDARD_TASK_GROUP);
              runTask.setDescription("Run an App Engine standard environment application locally");
              runTask.dependsOn(project.getTasks().findByName(BasePlugin.ASSEMBLE_TASK_NAME));

              project.afterEvaluate(
                  project -> {
                    runTask.setRunConfig(runExtension);
                    runTask.setDevServers(cloudSdkOperations.getDevServers());
                  });
            });

    project
        .getTasks()
        .create(
            START_TASK_NAME,
            DevAppServerStartTask.class,
            startTask -> {
              startTask.setGroup(APP_ENGINE_STANDARD_TASK_GROUP);
              startTask.setDescription(
                  "Run an App Engine standard environment application locally in the background");
              startTask.dependsOn(project.getTasks().findByName(BasePlugin.ASSEMBLE_TASK_NAME));

              project.afterEvaluate(
                  project -> {
                    startTask.setRunConfig(runExtension);
                    startTask.setDevServers(cloudSdkOperations.getDevServers());
                    startTask.setDevAppServerLoggingDir(
                        new File(project.getBuildDir(), DEV_APP_SERVER_OUTPUT_DIR_NAME));
                  });
            });

    project
        .getTasks()
        .create(
            STOP_TASK_NAME,
            DevAppServerStopTask.class,
            stopTask -> {
              stopTask.setGroup(APP_ENGINE_STANDARD_TASK_GROUP);
              stopTask.setDescription(
                  "Stop a locally running App Engine standard environment application");

              project.afterEvaluate(
                  project -> {
                    stopTask.setRunConfig(runExtension);
                    stopTask.setDevServers(cloudSdkOperations.getDevServers());
                  });
            });
  }
}
