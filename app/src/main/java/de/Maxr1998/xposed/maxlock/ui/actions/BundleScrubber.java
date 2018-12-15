/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui.actions;

import android.content.Intent;
import android.os.Bundle;

/**
 * Helper class to scrub Bundles of invalid extras. This is a workaround for an Android bug:
 * <http://code.google.com/p/android/issues/detail?id=16006>.
 */
public final class BundleScrubber {

    /**
     * Scrubs Intents for private serializable subclasses in the Intent extras. If the Intent's extras contain
     * a private serializable subclass, the Bundle is cleared. The Bundle will not be set to null. If the
     * Bundle is null, has no extras, or the extras do not contain a private serializable subclass, the Bundle
     * is not mutated.
     *
     * @param intent {@code Intent} to scrub. This parameter may be mutated if scrubbing is necessary. This
     *               parameter may be null.
     * @return true if the Intent was scrubbed, false if the Intent was not modified.
     */
    public static boolean scrub(final Intent intent) {
        return intent != null && scrub(intent.getExtras());
    }

    /**
     * Scrubs Bundles for private serializable subclasses in the extras. If the Bundle's extras contain a
     * private serializable subclass, the Bundle is cleared. If the Bundle is null, has no extras, or the
     * extras do not contain a private serializable subclass, the Bundle is not mutated.
     *
     * @param bundle {@code Bundle} to scrub. This parameter may be mutated if scrubbing is necessary. This
     *               parameter may be null.
     * @return true if the Bundle was scrubbed, false if the Bundle was not modified.
     */
    public static boolean scrub(final Bundle bundle) {
        if (null == bundle) {
            return false;
        }

        /*
         * Note: This is a hack to work around a private serializable classloader attack
         */
        try {
            // if a private serializable exists, this will throw an exception
            bundle.containsKey(null);
        } catch (final Exception e) {
            bundle.clear();
            return true;
        }
        return false;
    }
}