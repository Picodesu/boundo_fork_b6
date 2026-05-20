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

package com.madness.collision.unit.api_viewing.ui.info

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.repo.ArtTagRepoImpl
import com.madness.collision.unit.api_viewing.data.repo.ArtTagRepository
import com.madness.collision.unit.api_viewing.list.AppInfoUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppInfoViewModel : ViewModel() {
    private var tagRepo: ArtTagRepository? = null
    private val mutUiState: MutableStateFlow<AppInfoUiState>
    val uiState: StateFlow<AppInfoUiState>

    private var initJob: Job? = null
    private var tagExpJob: Job? = null

    init {
        val state = AppInfoUiState(tags = emptyList())
        mutUiState = MutableStateFlow(state)
        uiState = mutUiState.asStateFlow()
    }

    fun init(context: Context) {
        if (initJob != null) return
        initJob = viewModelScope.launch(Dispatchers.Default) {
            tagRepo = ArtTagRepoImpl(context.applicationContext)
        }
    }

    fun expressTags(app: ApiViewingApp) {
        tagExpJob?.cancel()
        tagExpJob = viewModelScope.launch(Dispatchers.Default) {
            initJob?.join()
            val tagRepo = tagRepo ?: return@launch
            tagRepo.express(app)
                .onEach { tags ->
                    mutUiState.update { currValue ->
                        currValue.copy(tags = tags)
                    }
                }
                .launchIn(this)
        }
    }
}
