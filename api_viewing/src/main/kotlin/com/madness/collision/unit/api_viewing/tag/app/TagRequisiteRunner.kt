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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Application-level singleton provider, use component-level runner instead where possible. */
internal object TagRequisiteRunnerProvider {
    private var runner: TagRequisiteRunner? = null
    private val runnerMutex = Mutex()

    suspend fun getRunner(context: Context): TagRequisiteRunner {
        runner?.let { return it }
        runnerMutex.withLock {
            val reqRunner = TagRequisiteRunner(context.applicationContext).init()
            // bind service synchronously
            try {
                withTimeout(1500) {
                    reqRunner.bindAndGetService(context.applicationContext)
                }
            } catch (e: TimeoutCancellationException) {
                e.printStackTrace()
            } catch (e: Exception) {
                // bind was unsuccessful (e.g. SecurityException)
                e.printStackTrace()
            }
            return reqRunner.also { runner = it }
        }
    }
}

class TagRequisiteRunner(private val context: Context) {

    typealias ConnCallback = () -> Unit

    private var serviceMessenger: Messenger? = null
    private val serviceConnCallbacks = ArrayDeque<ConnCallback>()
    private val serviceConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // IPC by a messenger
            serviceMessenger = service?.let(::Messenger)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
            serviceConnCallbacks.forEach { callback -> callback() }
        }
    }
    private var replyThread: HandlerThread? = null

    fun init(): TagRequisiteRunner {
        if (replyThread != null) return this
        replyThread = HandlerThread("TagReqRunner")
            .apply { start() }
        return this
    }

    fun initAndBind(lifecycle: Lifecycle): TagRequisiteRunner {
        if (lifecycle.currentState < Lifecycle.State.INITIALIZED) return this
        init()
        try {
            val intent = Intent(context, TagRequisiteService::class.java)
            context.bindService(intent, serviceConn, Service.BIND_AUTO_CREATE)
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    context.unbindService(serviceConn)
                    replyThread?.quitSafely()
                    replyThread = null
                }
            })
        } catch (e: SecurityException) {
            e.printStackTrace()
            context.unbindService(serviceConn)
            replyThread?.quitSafely()
            replyThread = null
        }
        return this
    }

    /** Unbind the connection after use. */
    suspend fun bindAndGetService(context: Context): Pair<ServiceConnection, Messenger?> =
        suspendCancellableCoroutine { cont ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    serviceConn.onServiceConnected(name, service)
                    if (cont.isActive) cont.resume(this to service?.let(::Messenger))
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    serviceConn.onServiceDisconnected(name)
                }
            }
            var isInvoked = false
            try {
                val intent = Intent(context, TagRequisiteService::class.java)
                context.bindService(intent, conn, Service.BIND_AUTO_CREATE)
                isInvoked = true
            } catch (e: SecurityException) {
                context.unbindService(conn)
                cont.resumeWithException(e)
            }
            cont.invokeOnCancellation {
                if (isInvoked) context.unbindService(conn)
            }
        }

    private inline fun <T> withConnCallback(noinline onDisconnected: () -> Unit, block: () -> T): T {
        try {
            serviceConnCallbacks.add(onDisconnected)
            return block()
        } finally {
            serviceConnCallbacks.remove(onDisconnected)
        }
    }

    /** Connect to existing service if present, otherwise bind anew and unbind afterward. */
    private suspend inline fun <T> bindService(
        noinline onDisconnected: () -> Unit, block: (Messenger) -> T): T? {
        val messenger = serviceMessenger
        if (messenger != null) {
            return withConnCallback(onDisconnected) { block(messenger) }
        } else {
            try {
                val (conn, service) = withTimeout(1500) { bindAndGetService(context) }
                return withConnCallback(onDisconnected) {
                    service?.let(block).also { context.unbindService(conn) }
                }
            } catch (e: TimeoutCancellationException) {
                e.printStackTrace()
                return null
            } catch (e: Exception) {
                // bind was unsuccessful (e.g. SecurityException)
                e.printStackTrace()
                return null
            }
        }
    }

    /** Better wrap in a [withTimeout] call. */
    suspend fun findSuperclass(apks: List<String>, names: Set<String>): Set<String>? {
        var msgContinuation: CancellableContinuation<*>? = null
        return bindService(
            onDisconnected = {
                if (msgContinuation?.isActive == true) {
                    msgContinuation?.resumeWithException(
                        IllegalStateException("service connection disconnected unexpectedly!"))
                }
            },
        ) { messenger ->
            suspendCancellableCoroutine { cont ->
                msgContinuation = cont
                val msg = Message.obtain(null, 1)
                msg.data = Bundle().apply {
                    putStringArrayList("apks", ArrayList(apks))
                    putStringArrayList("names", ArrayList(names))
                }
                val looper = replyThread?.looper ?: Looper.getMainLooper()
                msg.replyTo = Messenger(Handler(looper) { reply ->
                    if (!cont.isActive) return@Handler true
                    when (reply.what) {
                        1 -> cont.resume(reply.data.getStringArrayList("value")?.toSet().orEmpty())
                        else -> cont.resumeWithException(IllegalStateException("what ${reply.what}?"))
                    }
                    true
                })
                try {
                    if (cont.isActive) {
                        messenger.send(msg)
                    }
                } catch (e: RemoteException) {
                    cont.resumeWithException(e)
                }
            }
        }
    }
}
