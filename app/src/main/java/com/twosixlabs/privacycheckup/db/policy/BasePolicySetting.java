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

package com.twosixlabs.privacycheckup.db.policy;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Base policy setting for a particular permission.
 */
@Entity(indices = {@Index(value = {"permission"}, unique = true)})
public class BasePolicySetting {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "policy_decision")
    public String policyDecision;

    @ColumnInfo(name = "permission")
    public String permission;

    @NonNull
    @Override
    public String toString() {
        return "BasePolicyDecision{" +
                "id=" + id  +
                ", permission=" + permission +
                ", policy_decision=" + policyDecision + "}";
    }
}