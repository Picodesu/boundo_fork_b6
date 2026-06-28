/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.info

import com.madness.collision.unit.api_viewing.list.AppListService
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.get
import com.madness.collision.unit.api_viewing.tag.app.toExpressible

sealed interface ExpIcon {
    data class Res(val id: Int) : ExpIcon
    data class App(val packageName: String) : ExpIcon
    data class Text(val value: CharSequence) : ExpIcon
}

// ExpressedTag
interface ExpTag {
    val icon: ExpIcon
    val rank: String
}

data class CompactExpTag(
    override val icon: ExpIcon,
    override val rank: String,
) : ExpTag

internal data class FullExpTag(
    val id: String,
    val label: String,
    val desc: String?,
    override val icon: ExpIcon,
    override val rank: String,
    val activated: Boolean,
) : ExpTag


internal fun AppTagInfo.toCompactTag(res: AppTagInfo.Resources): CompactExpTag? {
    return CompactExpTag(icon = getIcon(res) ?: return null, rank = rank)
}

internal fun AppTagInfo.toExpTagOrNull(res: AppTagInfo.Resources): FullExpTag? {
    val tag = this
    val desc = tag.desc?.checkResultDesc?.invoke(res)
    val (expVal, express) = tag.toExpressible().setRes(res).run {
        expressValueOrNull() to express()
    }
    // normal label or dynamic label
    val labelOrDynamic = when {
        tag.label.isDynamic -> getDynamicLabel(res)
        // use express value as tag label
        expVal != null -> AppTagInfo.Label(string = expVal)
        express -> tag.label.run { full ?: normal }
        else -> tag.label.normal
    }
    return FullExpTag(
        id = id,
        label = labelOrDynamic.get(res.context)?.toString() ?: return null,
        desc = desc?.get(res.context)?.toString(),
        icon = getIcon(res) ?: return null,
        rank = rank,
        activated = express
    )
}

private fun AppTagInfo.getDynamicLabel(res: AppTagInfo.Resources): AppTagInfo.Label? {
    val str = requisites?.firstNotNullOfOrNull { res.dynamicRequisiteLabels[it.id] }
    if (str == null) return null
    // replace package installer with real name
    val pkgRegex = """[\w.]+""".toRegex()
    val labelString = if (str.matches(pkgRegex)) {
        AppListService().getInstallerName(res.context, str)
    } else {
        str
    }
    return AppTagInfo.Label(string = labelString)
}

private fun AppTagInfo.getIcon(res: AppTagInfo.Resources): ExpIcon? {
    // support dynamic icon
    return when {
        icon.drawableResId != null /*|| icon.drawable != null*/ -> ExpIcon.Res(icon.drawableResId)
        icon.text != null -> ExpIcon.Text(icon.text.get(res.context) ?: "")
        icon.pkgName != null ->
            ExpIcon.App(icon.pkgName)
        icon.isDynamic -> requisites
            ?.firstNotNullOfOrNull { res.dynamicRequisiteIconKeys[it.id] }
            ?.let { k -> ExpIcon.App(k) }
        else -> null
    }
}
