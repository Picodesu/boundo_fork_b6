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

package io.cliuff.boundo.art.apk.dex

import java.util.zip.ZipEntry

class NestedDexEntryTransformer(
    val inner: DexEntryTransformer,
    val outer: DexEntryTransformer,
): DexEntryTransformer {

    override suspend fun apply(entry: ZipEntry): ZipEntry? {
        val e = outer.apply(entry)
        if (e == null) return null
        val result = inner.apply(e)
        // abort processing and release outer transformer
        if (result == null) outer.postProcessing(entry)
        return result
    }

    override suspend fun postProcessing(entry: ZipEntry) {
        inner.postProcessing(entry)
        outer.postProcessing(entry)
    }
}

class IntensiveDexEntryTransformer(
    private val sizeLimit: Long, asyncDexLimit: Int): DexEntryTransformer {

    private val limiter = LimitDexEntryTransformer(asyncDexLimit)

    override suspend fun apply(entry: ZipEntry): ZipEntry? {
        return if (entry.size >= sizeLimit) limiter.apply(entry) else entry
    }

    override suspend fun postProcessing(entry: ZipEntry) {
        if (entry.size >= sizeLimit) limiter.postProcessing(entry)
    }
}

class CollectDexEntryTransformer : DexEntryTransformer {
    private val entrySizes = mutableMapOf<String, Long>()

    override suspend fun apply(entry: ZipEntry): ZipEntry {
        entrySizes[entry.name] = entry.size
        return entry
    }

    override suspend fun postProcessing(entry: ZipEntry) {
    }

    fun getEntrySizes(): Map<String, Long> {
        return entrySizes.toMap()
    }

    fun getLogMessage(apkPath: String): String {
        val dexSizes = getEntrySizes()
        return if (dexSizes.isNotEmpty()) {
            val (avg, max) = dexSizes.values.run { average() to max() }
            dexSizes.entries.joinToString(
                prefix = "${dexSizes.size} DEX (avg. $avg, max. $max) in $apkPath:\n",
                transform = { (n, size) -> "$n ($size bytes)" }
            )
        } else {
            "No DEX in $apkPath"
        }
    }
}
