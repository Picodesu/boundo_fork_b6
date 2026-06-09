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

package com.madness.collision.unit.api_viewing.tag.app

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import io.cliuff.boundo.art.apk.dex.AsyncDexLibSuperFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Service to run in a separate process to isolate native exceptions or heavy memory load. */
class TagRequisiteService : Service() {
    private var tagThread: HandlerThread? = null
    private var tagHandler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        tagThread = HandlerThread("TagReqHandler")
            .apply { start() }
    }

    override fun onDestroy() {
        tagThread?.quitSafely()
        tagHandler = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        val handler = tagHandler ?: kotlin.run {
            val looper = tagThread?.looper ?: Looper.getMainLooper()
            TagRequisiteHandler(looper).also { tagHandler = it }
        }
        return Messenger(handler).binder
    }
}

class TagRequisiteHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            1 -> {
                msg.peekData()?.let {
                    val apks = msg.data.getStringArrayList("apks").orEmpty()
                    val names = msg.data.getStringArrayList("names").orEmpty()
                    val result = runCatching { findSuperclass(apks, names) }
                        .onFailure(Throwable::printStackTrace)
                        .getOrDefault(null)

                    val reply = Message.obtain(null, 1)
                    reply.data = Bundle().apply { putStringArrayList("value", result) }
                    try {
                        msg.replyTo?.send(reply)
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun findSuperclass(apks: List<String>, names: List<String>): ArrayList<String> {
        val finder = AsyncDexLibSuperFinder()
        return runBlocking(Dispatchers.IO) { finder.resolve(apks, names.toSet()) }.let(::ArrayList)
            .also { Log.d("TagRequisiteService", "Found ${it.size} superclass.") }
    }
}
