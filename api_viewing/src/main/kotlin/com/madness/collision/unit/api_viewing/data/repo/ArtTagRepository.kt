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

package com.madness.collision.unit.api_viewing.data.repo

import android.content.Context
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.info.AppInfo
import com.madness.collision.unit.api_viewing.info.ExpressedTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Changelog 2026
 * 05.14 tag repo, proxy AppInfo.expressTags
 */

/** App tag repository. */
internal interface ArtTagRepository {
    /** @return a flow of updated tag list. */
    fun express(app: ApiViewingApp): Flow<List<ExpressedTag>>
}

internal class ArtTagRepoImpl(private val context: Context) : ArtTagRepository {

    override fun express(app: ApiViewingApp): Flow<List<ExpressedTag>> {
        return channelFlow {
            AppInfo.expressTags(app, context) { tags ->
                trySend(tags)
            }
        }
            .flowOn(Dispatchers.IO)
            .buffer(capacity = Channel.CONFLATED)
    }
}
