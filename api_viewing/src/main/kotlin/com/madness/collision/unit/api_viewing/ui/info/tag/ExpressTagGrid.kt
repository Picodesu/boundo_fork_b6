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

package com.madness.collision.unit.api_viewing.ui.info.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madness.collision.ui.theme.MetaAppTheme
import com.madness.collision.ui.theme.PreviewAppTheme
import com.madness.collision.unit.api_viewing.R
import com.madness.collision.unit.api_viewing.info.ExpIcon
import com.madness.collision.unit.api_viewing.info.tag.ArtTagGroup
import com.madness.collision.unit.api_viewing.info.tag.MessagingTag
import com.madness.collision.unit.api_viewing.info.tag.SystemTag
import com.madness.collision.unit.api_viewing.info.tag.TechnologyTag
import com.madness.collision.unit.api_viewing.ui.comp.rememberAppIcon
import com.madness.collision.util.dev.PreviewCombinedColorLayout
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.util.EnumMap

/**
 * Changelog 2026
 * 05.16 standalone groups, tag grid, lazy list rows, preview
 */

/** Expressed app tag. */
@Immutable
class ExpressTag(
    val id: String,
    val label: String,
    val desc: String?,
    val icon: ExpIcon?,
    val activated: Boolean,
)

enum class ExpressGroup {
    None, Technology, Messaging
}

object ExpressGroupTag {
    val Groups: Map<ExpressGroup, Set<String>> = getExpressGroupTagMap()

    private fun getExpressGroupTagMap(): Map<ExpressGroup, Set<String>> {
        val groups = buildMap {
            put(ExpressGroup.Technology, TechnologyTag.entries)
            put(ExpressGroup.Messaging, MessagingTag.entries)
        }
        val idMap = groups.mapValues { (_, tags) ->
            tags.mapTo(HashSet(tags.size), ArtTagGroup::id)
        }
        return EnumMap(idMap)
    }
}


fun LazyListScope.expressTagItems(
    tags: List<ExpressTag>,
    groups: Map<ExpressGroup, Set<String>> = ExpressGroupTag.Groups,
) {
    // non-empty tags: layout group grid into rows
    // empty tags: placeholder rows for static nonCompItemCount in LibChecking
    val gRows = groups.keys.chunked(2)
    // tags for a single group, empty when not yet expressed
    val gGrid = gRows.map { row ->
        row.associateWith { g ->
            val ids = groups[g].orEmpty()
            tags.filter { it.id in ids }
                .sortedByDescending(ExpressTag::activated)
        }
    }

    for (i in gRows.indices) {
        item(key = gRows[i], contentType = "TagGroupRow") {
            // multiple groups in a row
            if (tags.isNotEmpty())  // skip if empty (item placeholder)
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (g in gRows[i]) {
                    val expTags = gGrid[i][g].orEmpty()
                    if (expTags.isNotEmpty()) {
                        key(g) {
                            ExpressTagGroup(
                                modifier = Modifier.weight(1f),
                                name = g.name(),
                                tags = expTags,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (gRows[i].size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun ExpressGroup.name() =
    when (this) {
        ExpressGroup.Technology -> stringResource(R.string.av_info_tag_group_tech)
        ExpressGroup.Messaging -> stringResource(R.string.av_info_tag_group_msg)
        ExpressGroup.None -> "Others"
    }

@Composable
private fun ExpressTagGroup(
    name: String,
    tags: List<ExpressTag>,
    modifier: Modifier = Modifier,
) {
    val renderTags = remember(tags) { tags.take(3) }

    Card(
        modifier = modifier,
        shape = AbsoluteSmoothCornerShape(20.dp, 60),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (t in renderTags) {
                    key(t.id) {
                        TagIcon(icon = t.icon, activated = t.activated)
                    }
                }
            }
            Text(
                text = name,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TagIcon(
    icon: ExpIcon?,
    activated: Boolean,
    modifier: Modifier = Modifier,
) {
    val img = when (icon) {
        is ExpIcon.Res -> icon.id
        is ExpIcon.App -> rememberAppIcon(icon.packageName)
        is ExpIcon.Text -> null
        null -> null
    }
    if (img != null) {
        val colorFilter = remember(activated) {
            if (activated) return@remember null
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        }
        AsyncImage(
            modifier = modifier.size(27.dp),
            model = img,
            contentDescription = null,
            colorFilter = colorFilter,
        )
    } else {
        Icon(
            modifier = modifier.size(27.dp),
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
                .copy(alpha = if (activated) 0.75f else 0.4f),
        )
    }
}

private fun previewTags() = mapOf(
    TechnologyTag.Kotlin to true,
    TechnologyTag.JetpackCompose to true,
    TechnologyTag.ComposeMultiplatform to false,
    MessagingTag.Firebase to true,
    MessagingTag.Oppo to false,
    MessagingTag.Xiaomi to false,
    SystemTag.GooglePlayStore to true,
    SystemTag.System to false,
).map { (t, activated) ->
    ExpressTag(t.id, t.name, "", null, activated)
}

@Composable
@PreviewCombinedColorLayout
private fun TagGridPreview() {
    val mockTags = previewTags()
    val mockGroups = ExpressGroupTag.Groups +
            (ExpressGroup.None to setOf(SystemTag.GooglePlayStore.id, SystemTag.System.id))

    PreviewAppTheme {
        Surface(color = MetaAppTheme.colorScheme.surfaceNeutral) {
            LazyColumn {
                expressTagItems(tags = mockTags, groups = mockGroups)
            }
        }
    }
}
