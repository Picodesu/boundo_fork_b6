/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing

import android.content.Context
import android.content.SharedPreferences
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.tag.app.AppTagInfo
import com.madness.collision.unit.api_viewing.tag.app.AppTagManager
import com.madness.collision.unit.api_viewing.tag.app.toExpressible
import com.madness.collision.unit.api_viewing.util.PrefUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object AppTag {

    class TagStateMap(private val tags: Map<String, TriStateSelectable>) : Map<String, TriStateSelectable> by tags {
        override fun get(key: String): TriStateSelectable {
            return tags[key] ?: TriStateSelectable(key, TriStateSelectable.STATE_DESELECTED)
        }
    }

    private val displayingTagsPrivate = mutableMapOf<String, TriStateSelectable>()
    private val displayingTags: TagStateMap = TagStateMap(displayingTagsPrivate)
    private val tagReqMutex: MutableMap<String, Mutex> = hashMapOf()

    // todo some requisites should be checked only once when loading settings
    suspend fun ensureRequisites(context: Context, app: ApiViewingApp): AppTagInfo.Resources {
        val res = AppTagInfo.Resources(context, app)
        getGroupedRequisites().forEach { (p, tagIds) ->
            val (reqId, requisite) = p
            if (requisite.checker(res)) return@forEach // continue
            if (tagIds.all { displayingTags[it].isDeselected }) return@forEach // continue
            val mutex = tagReqMutex[reqId] ?: Mutex()
            mutex.withLock {
                tagReqMutex[reqId] = mutex
                requisite.run { if (!checker(res)) loader(res) }
                tagReqMutex.remove(reqId, mutex)
            }
        }
        return res
    }

    // for all tags
    suspend fun ensureRequisitesForAllAsync(context: Context, app: ApiViewingApp, perRequisite: (
    suspend (requisite: AppTagInfo.Requisite, tagIds: List<String>, res: AppTagInfo.Resources) -> Unit
    )?): AppTagInfo.Resources {
        return ensureRequisitesAsync(context, app, perRequisite, null)
    }

    suspend fun ensureRequisitesAsync(context: Context, app: ApiViewingApp, perRequisite: (
    suspend (requisite: AppTagInfo.Requisite, tagIds: List<String>, res: AppTagInfo.Resources) -> Unit
    )?): AppTagInfo.Resources {
        return ensureRequisitesAsync(context, app, perRequisite) { tagIds ->
            tagIds.any { displayingTags[it].isDeselected.not() }
        }
    }

    /**
     * Resource loading: selected or anti-selected (anti any tag requires resource loading to confirm).
     *
     * If requisite is checked, include it in [perRequisite] but without duplicate loading,
     * Also when requisite tags are all deselected (not checking, i.e., not selected or anti-selected).
     */
    private suspend fun ensureRequisitesAsync(context: Context, app: ApiViewingApp, perRequisite: (
    suspend (requisite: AppTagInfo.Requisite, tagIds: List<String>, res: AppTagInfo.Resources) -> Unit
    )?, filter: ((tagIds: List<String>) -> Boolean)?): AppTagInfo.Resources = coroutineScope {
        val res = AppTagInfo.Resources(context, app)
        val (checked, unchecked) = getGroupedRequisites().partition { (p, tagIds) ->
            p.second.checker(res) || (filter != null && filter(tagIds).not())
        }
        // use flow to order results by exec speed
        val uncheckedFlow = channelFlow {
        for ((p, tagIds) in unchecked) {
            val (reqId, requisite) = p
            launch(Dispatchers.Default) {
                val mutex = tagReqMutex[reqId] ?: Mutex()
                mutex.withLock {
                    tagReqMutex[reqId] = mutex
                    requisite.run { if (!checker(res)) loader(res) }
                    tagReqMutex.remove(reqId, mutex)
                }
                send(requisite to tagIds)
            }
        }
        }
        if (perRequisite != null) {
            checked.forEach { (p, tagIds) ->
                val (_, requisite) = p
                perRequisite(requisite, tagIds, res)
            }
            uncheckedFlow
                .onEach { (req, tagIds) -> perRequisite(req, tagIds, res) }
                .catch { it.printStackTrace() }
                .collect()
        } else {
            uncheckedFlow
                .catch { it.printStackTrace() }
                .collect()
        }
        res
    }

    // Direct tags that have no requisites are excluded
    private fun getGroupedRequisites(): List<Pair< Pair<String, AppTagInfo.Requisite>, List<String> >> {
        return AppTagManager.tags.flatMap m@{ (tagId, tagInfo) ->
            val requisites = tagInfo.requisites ?: return@m emptyList()
            requisites.map { requisite -> tagId to requisite }
        }.groupBy { (_, requisite) -> // group requisites of the same ID together
            requisite.id // this key is only used for grouping
        }.map { (reqId, tagIdList) ->
            // (requisiteId-requisite) - List{tagId}
            (reqId to tagIdList[0].second) to tagIdList.map { it.first }
        }
    }

    fun AppTagInfo.selExpressed(res: AppTagInfo.Resources): Boolean {
        return displayingTags[id].isSelected && express(res)
    }

    // Filtering: selected matches expressing, or ExpressibleTag.express().
    suspend fun filterTags(context: Context, app: ApiViewingApp): Boolean {
        val res = ensureRequisitesAsync(context, app, null)
        return AppTagManager.tags.any { it.value.express(res) }
    }

    private fun AppTagInfo.express(res: AppTagInfo.Resources): Boolean {
        val state = displayingTags[id]
        if (state.isDeselected) return false
        return toExpressible().setRes(res).apply { if (state.isAntiSelected) anti() }.express()
    }

    /**
     * @return true if changed, false if not changed or null if lazy and changed
     */
    private fun Map<String, TriStateSelectable>.changed(key: String, isLazy: Boolean): Boolean? {
        val newValue = this[key]
        val oldValue = displayingTags[key]
        if (newValue == null) {
            displayingTagsPrivate.remove(key)
        } else {
            displayingTagsPrivate[key] = newValue
        }
        val isChanged = newValue?.state != oldValue.state
        if (isLazy && isChanged) return null
        return isChanged
    }

    fun loadTagSettings(tagSettings: Map<String, TriStateSelectable>, isLazy: Boolean): Boolean {
        var isChanged = false
        AppTagInfo.IdGroup.BUILT_IN.forEach {
            isChanged = (tagSettings.changed(it, isLazy) ?: return true) || isChanged
        }
        return isChanged
    }

    fun loadTagSettings(prefSettings: SharedPreferences, isLazy: Boolean): Boolean {
        val tagSettings = prefSettings.getStringSet(PrefUtil.AV_TAGS, HashSet())!!
        val triStateMap = tagSettings.associateWith {
            TriStateSelectable(it, true)
        }
        return loadTagSettings(triStateMap, isLazy)
    }

    fun getTagSettings(): Map<String, TriStateSelectable> {
        return LinkedHashMap(displayingTags)
    }
}
