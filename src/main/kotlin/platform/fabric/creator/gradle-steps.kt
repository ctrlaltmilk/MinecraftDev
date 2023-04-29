/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.creator

import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addGradleGitignore
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.addGradleWrapperProperties
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.gitEnabled
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.util.MinecraftTemplates
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class FabricGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> FabricGradleFilesStep(parent).nextStep(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}

class FabricGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val mcVersion = data.getUserData(FabricVersionChainStep.MC_VERSION_KEY) ?: return
        val yarnVersion = data.getUserData(FabricVersionChainStep.YARN_VERSION_KEY) ?: return
        val loaderVersion = data.getUserData(FabricVersionChainStep.LOADER_VERSION_KEY) ?: return
        val loomVersion = "1.1-SNAPSHOT"
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val apiVersion = data.getUserData(FabricVersionChainStep.API_VERSION_KEY)
        val officialMappings = data.getUserData(FabricVersionChainStep.OFFICIAL_MAPPINGS_KEY) ?: false

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "VERSION" to buildSystemProps.version,
            "MC_VERSION" to mcVersion,
            "YARN_MAPPINGS" to yarnVersion,
            "LOADER_VERSION" to loaderVersion,
            "LOOM_VERSION" to loomVersion,
            "JAVA_VERSION" to javaVersion,
        )

        if (apiVersion != null) {
            assets.addTemplateProperties("API_VERSION" to apiVersion)
        }

        if (officialMappings) {
            assets.addTemplateProperties("OFFICIAL_MAPPINGS" to "true")
        }

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.FABRIC_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.FABRIC_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.FABRIC_SETTINGS_GRADLE_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)

        if (gitEnabled) {
            assets.addGradleGitignore(project)
        }
    }
}
