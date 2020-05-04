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

package com.twosixlabs.privacycheckup.db.requests;

import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.twosixlabs.privacycheckup.stackcache.AbstractStackCache;

import java.security.InvalidKeyException;
import java.util.Date;

@Entity
public class DangerousPermissionRequest {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "package_name")
    public String packageName;

    @ColumnInfo(name = "permission")
    public String permission;

    @ColumnInfo(name = "purpose")
    public String purpose;

    @ColumnInfo(name = "stack_trace")
    public String stackTrace;

    @ColumnInfo(name = "is_privileged")
    public boolean isPrivileged;

    @ColumnInfo(name = "is_system")
    public boolean isSystem;

    @ColumnInfo(name = "is_background")
    public boolean isBackground;

    @ColumnInfo(name = "calling_component")
    public ComponentName callingComponent;

    @ColumnInfo(name = "top_activity")
    public ComponentName topActivity;

    @ColumnInfo(name = "allowed")
    public boolean allowed;

    @ColumnInfo(name = "timestamp")
    public Date timestamp;

    @NonNull
    @Override
    public String toString() {
        String stackString = stackTrace;
        AbstractStackCache cache = AbstractStackCache.getCurrentCache();
        if(cache != null && stackString != null) {
            String[] keys = stackString.split(",");
            StringBuffer buffer = new StringBuffer();
            for(String key : keys) {
                String value = key;
                try {
                    value = cache.getFromCache(key);
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } finally {
                    buffer.append(value + ",");
                }
            }

            stackString = buffer.toString();
        }


        return "Request{" +
                "id=" + id  +
                ", package=" + packageName +
                ", permission=" + permission +
                ", purpose=" + purpose +
                ", st=" + stackString +
                ", is_privileged=" + isPrivileged +
                ", is_system=" + isSystem +
                ", is_background=" + isBackground +
                ", calling_component=" + callingComponent +
                ", top_activity=" + topActivity +
                ", allowed=" + allowed +
                ", timestamp=" + timestamp.getTime() + "}";
    }

    public String getShortString() {
        return packageName + ", " + permission + ", bg=" + isBackground;
    }
}
