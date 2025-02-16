/*
 * This work was authored by Two Six Labs, LLC and is sponsored by a
 * subcontract agreement with Raytheon BBN Technologies Corp. under Prime
 * Contract No. FA8750-16-C-0006 with the Air Force Research Laboratory (AFRL).

 * The Government has unlimited rights to use, modify, reproduce, release,
 * perform, display, or disclose computer software or computer software
 * documentation marked with this legend. Any reproduction of technical data,
 * computer software, or portions thereof marked with this legend must also
 * reproduce this marking.

 * (C) 2020 Two Six Labs, LLC.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twosixlabs.privacycheckup.stackcache;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.InvalidKeyException;

public class SharedPrefsStackCache extends AbstractStackCache {
    private static final String SHARED_PREFS_NAME = "SharedPrefsStackCache";
    private SharedPreferences sp = null;

    public SharedPrefsStackCache(Context c) {
        super();
        sp = c.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public String getFromCache(String key) throws InvalidKeyException {
        String value = sp.getString(key, null);

        if(value == null) {
            throw new InvalidKeyException("SharedPrefsStackCache key " + key + " not found");
        }

        return value;
    }

    @Override
    protected boolean isInCache(String key) {
        return sp.contains(key);
    }

    @Override
    protected void putInCache(String key, String value) {
        sp.edit().putString(key, value).apply();
    }

    @Override
    protected int getCacheSize() {
        return sp.getAll().size();
    }
}
