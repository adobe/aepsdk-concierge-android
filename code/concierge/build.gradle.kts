/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project
val mavenEdgeVersion: String by project
val mavenEdgeIdentityVersion: String by project
val material3Version = "1.2.0"

aepLibrary {
    namespace = "com.adobe.marketing.mobile.concierge"
    enableSpotless = true
    enableCheckStyle = true
    enableDokkaDoc = true
    compose = true

    publishing {
        gitRepoName = "aepsdk-concierge-android"
        addCoreDependency(mavenCoreVersion)
        addEdgeDependency(mavenEdgeVersion)
        addEdgeIdentityDependency(mavenEdgeIdentityVersion)

        addMavenDependency("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", BuildConstants.Versions.KOTLIN)
        addMavenDependency("androidx.compose.ui", "ui-tooling-preview", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.compose.material3", "material3", material3Version)
        addMavenDependency("androidx.compose.ui", "ui", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.activity", "activity-compose", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.compose.material", "material-icons-core", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.compose.material", "material-icons-extended", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.lifecycle", "lifecycle-runtime-compose", "2.7.0")
        addMavenDependency("androidx.appcompat", "appcompat", BuildConstants.Versions.ANDROIDX_APPCOMPAT)
        addMavenDependency("androidx.compose.runtime", "runtime", BuildConstants.Versions.COMPOSE)
        addMavenDependency("androidx.activity", "activity-compose", BuildConstants.Versions.ANDROIDX_ACTIVITY_COMPOSE)
        addMavenDependency("androidx.compose.material3", "material3", material3Version)
    }
}

dependencies {
    // COMPOSE_RUNTIME, COMPOSE_MATERIAL, ANDROIDX_ACTIVITY_COMPOSE, COMPOSE_UI_TOOLING
    implementation("androidx.compose.ui:ui-tooling-preview:${BuildConstants.Versions.COMPOSE}")
    implementation("androidx.compose.material3:material3:$material3Version")
    implementation("androidx.compose.ui:ui:${BuildConstants.Versions.COMPOSE}")
    implementation("androidx.activity:activity-compose:${BuildConstants.Versions.COMPOSE}")
    
    // Material Icons for UI components
    implementation("androidx.compose.material:material-icons-core:${BuildConstants.Versions.COMPOSE}")
    implementation("androidx.compose.material:material-icons-extended:${BuildConstants.Versions.COMPOSE}")
    
    // Lifecycle compose for collectAsStateWithLifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // AEP SDK
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    implementation("com.adobe.marketing.mobile:edge:$mavenEdgeVersion")
    implementation("com.adobe.marketing.mobile:edgeidentity:$mavenEdgeIdentityVersion")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation(BuildConstants.Dependencies.MOCKK)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(BuildConstants.Dependencies.ROBOLECTRIC)
    testImplementation(BuildConstants.Dependencies.ESPRESSO_CORE)
    implementation("androidx.test.ext:junit:1.1.5")

}