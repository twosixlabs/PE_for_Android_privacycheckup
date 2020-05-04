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

import org.apache.commons.codec.digest.DigestUtils;

import java.security.InvalidKeyException;

public abstract class AbstractStackCache {
    private static int putAttempts = 0;
    private static int putHits = 0;
    private static int putMisses = 0;

    private static AbstractStackCache currentCache = null;

    protected AbstractStackCache() {
        AbstractStackCache.currentCache = this;
    }

    /**
     *
     * @param elt
     * @return True if the stack trace element is in the cache
     */
    public boolean isInCache(StackTraceElement elt) {
        return isInCache(getDigest(elt));
    }

    /**
     * Put a stack trace element in the cache
     * @param elt
     * @return The cache key corresponding to this element
     */
    public String putInCache(StackTraceElement elt) {
        putAttempts++;
        String key = getDigest(elt);

        if(!isInCache(elt)) {
            String value = elt.toString();

            putInCache(key, value);

            putMisses++;

        } else {
            putHits++;

        }

        return key;
    }

    @Override
    public String toString() {
        return String.format("size=%d, attempts=%d, hits=%d, misses=%d", getCacheSize(),putAttempts, putHits, putMisses);
    }

    /**
     *
     * @param key
     * @return The value from the cache, given a known key
     * @throws InvalidKeyException If the key was not found in the ache
     */
    public abstract String getFromCache(String key) throws InvalidKeyException;

    protected abstract boolean isInCache(String key);

    protected abstract void putInCache(String key, String value);

    protected abstract int getCacheSize();

    protected final String getDigest(StackTraceElement elt) {
        String eltString = elt.toString();
        String digest = DigestUtils.md5Hex(eltString);

        return digest;
    }

    public static AbstractStackCache getCurrentCache() {
        return currentCache;
    }
}
