/*
 * Copyright 2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.Maxr1998.xposed.maxlock.util

import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException

class Ref<out T : Any> internal constructor(obj: T) {
    private val weakRef = WeakReference(obj)

    operator fun invoke(): T {
        return weakRef.get() ?: throw CancellationException()
    }
}

fun <T : Any> T.asReference() = Ref(this)