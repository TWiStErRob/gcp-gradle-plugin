/*
 * Copyright 2019 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.gradle.appengine.core;

import com.google.cloud.tools.appengine.configuration.DeployConfiguration;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeployExtensionTest {

  @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();

  @Mock private DeployTargetResolver deployTargetResolver;
  private Project testProject;

  @Before
  public void setUp() {
    Mockito.when(deployTargetResolver.getProject("test-project-id"))
        .thenReturn("processed-project-id");
    Mockito.when(deployTargetResolver.getVersion("test-version")).thenReturn("processed-version");
    testProject = ProjectBuilder.builder().withProjectDir(testProjectDir.getRoot()).build();
  }

  @Test
  public void testToDeployConfiguration_allValuesSet() {
    FileOperations fileOperations =
        ((ProjectInternal) testProject).getServices().get(FileOperations.class);
    DeployExtension testExtension = new DeployExtension(fileOperations);
    testExtension.setDeployTargetResolver(deployTargetResolver);

    testExtension.setBucket("test-bucket");
    testExtension.setGcloudMode("beta");
    testExtension.setImageUrl("test-img-url");
    testExtension.setProjectId("test-project-id");
    testExtension.setPromote(true);
    testExtension.setServer("test-server");
    testExtension.setStopPreviousVersion(true);
    testExtension.setVersion("test-version");

    List<Path> projects = ImmutableList.of(Paths.get("project1"), Paths.get("project2"));
    DeployConfiguration config = testExtension.toDeployConfiguration(projects);

    Assert.assertEquals(projects, config.getDeployables());
    Assert.assertEquals("test-bucket", config.getBucket());
    Assert.assertEquals("beta", config.getGcloudMode());
    Assert.assertEquals("test-img-url", config.getImageUrl());
    Assert.assertEquals("processed-project-id", config.getProjectId());
    Assert.assertEquals(Boolean.TRUE, config.getPromote());
    Assert.assertEquals("test-server", config.getServer());
    Assert.assertEquals(Boolean.TRUE, config.getStopPreviousVersion());
    Assert.assertEquals("processed-version", config.getVersion());

    Mockito.verify(deployTargetResolver).getProject("test-project-id");
    Mockito.verify(deployTargetResolver).getVersion("test-version");
    Mockito.verifyNoMoreInteractions(deployTargetResolver);
  }

  @Test
  public void testToDeployConfiguration_onlyRequiredValuesSet() {
    FileOperations fileOperations =
        ((ProjectInternal) testProject).getServices().get(FileOperations.class);
    DeployExtension testExtension = new DeployExtension(fileOperations);
    testExtension.setDeployTargetResolver(deployTargetResolver);

    testExtension.setProjectId("test-project-id");
    testExtension.setVersion("test-version");

    List<Path> projects = ImmutableList.of(Paths.get("project1"), Paths.get("project2"));
    DeployConfiguration config = testExtension.toDeployConfiguration(projects);

    Assert.assertEquals("processed-project-id", config.getProjectId());
    Assert.assertEquals("processed-version", config.getVersion());

    Assert.assertNull(config.getBucket());
    Assert.assertNull(config.getGcloudMode());
    Assert.assertNull(config.getImageUrl());
    Assert.assertNull(config.getPromote());
    Assert.assertNull(config.getServer());
    Assert.assertNull(config.getStopPreviousVersion());

    Mockito.verify(deployTargetResolver).getProject("test-project-id");
    Mockito.verify(deployTargetResolver).getVersion("test-version");
    Mockito.verifyNoMoreInteractions(deployTargetResolver);
  }
}
