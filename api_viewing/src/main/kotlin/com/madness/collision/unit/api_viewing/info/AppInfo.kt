/*
 * Copyright 2022 Clifford Liu
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

import android.content.Context
import android.text.format.Formatter
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.AppPackage
import com.madness.collision.unit.api_viewing.info.tag.SystemTag
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.util.os.OsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileNotFoundException
import java.util.TreeSet
import kotlin.io.path.Path
import kotlin.io.path.name

internal object AppInfo {

    private fun tagsByStage(app: ApiViewingApp, context: Context, reqForAll: Boolean = false) = flow {
        val ensureRequisitesAsync = when {
            reqForAll -> AppTag::ensureRequisitesForAllAsync
            else -> AppTag::ensureRequisitesAsync
        }
        val emptyRes = AppTagInfo.Resources(context, app)
        // inflate tags that do not have requisites or are satisfied first
        val directTagIds = AppTagManager.tags.mapNotNull { (id, tag) ->
            id.takeIf { tag.requisites == null || tag.requisites.all { it.checker(emptyRes) } }
        }
        emit(directTagIds to emptyRes)

        // ensure resources and inflate requisite tags
        val inflatedTagIds = directTagIds.toHashSet()
        val res = ensureRequisitesAsync(context, app) { _, tagIds, res ->
            inflatedTagIds.addAll(tagIds)
            emit(tagIds to res)
        }
        // inflate any tag left (should be none)
        val leftTagIds = (AppTagManager.tags.keys - inflatedTagIds)
        if (leftTagIds.isNotEmpty()) emit(leftTagIds to res)
    }

    /** Tags to display in app info. */
    fun getDetailedExpTags(app: ApiViewingApp, context: Context): Flow<List<FullExpTag>> {
        val mapToExp = { tags: Collection<String>, res: AppTagInfo.Resources ->
            // Tag inflating: selected and expressed true (no anti-ed tag icon support yet).
            tags.mapNotNull(AppTagManager.tags::get)
                // make sure all requisites are satisfied
                .filter { tag -> tag.requisites.orEmpty().all { req -> req.checker(res) } }
                .mapNotNull { tag -> tag.toExpTagOrNull(res) }
                // hide inactivated package installer tags
                .filter { expTag ->
                    when (expTag.id) {
                        SystemTag.GooglePlayStore.id -> expTag.activated
                        SystemTag.PackageInstaller.id -> expTag.activated
                        else -> true
                    }
                }
        }
        val aggregateTags = TreeSet<FullExpTag>(compareBy(ExpTag::rank))
        return tagsByStage(app, context, reqForAll = true)
            .map { (tags, res) -> mapToExp(tags, res) }
            .map { tags -> aggregateTags.addAll(tags); aggregateTags.toList() }
            .flowOn(Dispatchers.Default)
    }

    /** Tags to display in app list. */
    fun getExpTags(app: ApiViewingApp, context: Context): Flow<List<ExpTag>> {
        val mapToExp = { tags: Collection<String>, res: AppTagInfo.Resources ->
            // Tag inflating: selected and expressed true (no anti-ed tag icon support yet).
            tags.mapNotNull(AppTagManager.tags::get)
                // make sure all requisites are satisfied
                .filter { tag -> tag.requisites.orEmpty().all { req -> req.checker(res) } }
                // selected and expressed true
                .filter { tag -> with(AppTag) { tag.selExpressed(res) } }
                .mapNotNull { tag -> tag.toCompactTag(res) }
        }
        val aggregateTags = TreeSet(compareBy(ExpTag::rank))
        return tagsByStage(app, context)
            .map { (tags, res) -> mapToExp(tags, res) }
            .map { tags -> aggregateTags.addAll(tags); aggregateTags.toList() }
            .flowOn(Dispatchers.Default)
    }

    fun getApkSizeList(pkg: AppPackage, context: Context): List<Pair<String, String?>> {
        val baseName = if (OsUtils.satisfy(OsUtils.O)) Path(pkg.basePath).name else File(pkg.basePath).name
        val parentPath = pkg.basePath.replaceFirst(baseName, "")
        val sizes = pkg.apkPaths.map { path ->
            val apk = File(path)
            val access = runCatching { apk.canRead() || throw FileNotFoundException(path) }
                .onFailure(Throwable::printStackTrace)
                .getOrDefault(false)
            val fileName = path.replaceFirst(parentPath, "")
            if (access) (fileName to apk.length()) else (fileName to null)
        }
        val totalBytes = sizes.sumOf { it.second ?: 0 }.takeIf { it > 0 }
        val totalSize = totalBytes?.let { Formatter.formatFileSize(context, it) }
        val itemSizeList = sizes.map apkSize@{ (name, bytes) ->
            bytes ?: return@apkSize name to null
            name to Formatter.formatFileSize(context, bytes)
        }
        return buildList(itemSizeList.size + 1) {
            add("" to totalSize)
            addAll(itemSizeList)
        }
    }
}