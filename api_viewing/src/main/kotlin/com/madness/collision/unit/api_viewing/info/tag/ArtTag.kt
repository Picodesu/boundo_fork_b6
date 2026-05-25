/*
 * Copyright 2026 Clifford Liu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.madness.collision.unit.api_viewing.info.tag

import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo as Tag

/**
 * Changelog 2026
 * 04.29 standalone tag group enums
 */

/** App tag group. */
sealed interface ArtTagGroup {
    val id: String
}

enum class TechnologyTag(override val id: String) : ArtTagGroup {

    Kotlin(Tag.ID_TECH_KOTLIN),
    JetpackCompose(Tag.ID_TECH_X_COMPOSE),
    ComposeMultiplatform(Tag.ID_TECH_COMPOSE_CMP),

    Cordova(Tag.ID_TECH_CORDOVA),
    Flutter(Tag.ID_TECH_FLUTTER),
    MAUI(Tag.ID_TECH_MAUI),
    ReactNative(Tag.ID_TECH_REACT_NATIVE),
    Xamarin(Tag.ID_TECH_XAMARIN),
}

enum class SystemTag(override val id: String) : ArtTagGroup {

    GooglePlayStore(Tag.ID_APP_INSTALLER_PLAY),
    PackageInstaller(Tag.ID_APP_INSTALLER),

    Hidden(Tag.ID_APP_HIDDEN),
    System(Tag.ID_APP_SYSTEM),
    SystemCore(Tag.ID_APP_SYSTEM_CORE),
    SystemModule(Tag.ID_APP_SYSTEM_MODULE),
}

enum class PackageTag(override val id: String) : ArtTagGroup {

    AppCategory(Tag.ID_APP_CATEGORY),
    TypeInstantApp(Tag.ID_TYPE_INSTANT),
    TypeOverlay(Tag.ID_TYPE_OVERLAY),
    TypeWebApk(Tag.ID_TYPE_WEB_APK),

    AAB(Tag.ID_PKG_AAB),
    Abi64Bit(Tag.ID_PKG_64BIT),
    AdaptiveIcons(Tag.ID_APP_ADAPTIVE_ICON),
    PredictiveBack(Tag.ID_APP_PREDICTIVE_BACK),
}

enum class MessagingTag(override val id: String) : ArtTagGroup {

    Firebase(Tag.ID_MSG_FCM),

    Honor(Tag.ID_MSG_HONOR),
    Huawei(Tag.ID_MSG_HUAWEI),
    Meizu(Tag.ID_MSG_MEIZU),
    Oppo(Tag.ID_MSG_OPPO),
    Vivo(Tag.ID_MSG_VIVO),
    Xiaomi(Tag.ID_MSG_XIAOMI),

    Ali(Tag.ID_MSG_ALI),
    Baidu(Tag.ID_MSG_BAIDU),
    Getui(Tag.ID_MSG_GETUI),
    Jpush(Tag.ID_MSG_JPUSH),
    TPNS(Tag.ID_MSG_TPNS),
    Upush(Tag.ID_MSG_UPUSH),
}
