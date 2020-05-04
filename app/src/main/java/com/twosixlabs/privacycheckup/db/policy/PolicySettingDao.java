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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class PolicySettingDao {

    @Query("SELECT * FROM policysetting")
    public abstract List<PolicySetting> getAll();

    @Query("SELECT * FROM policysetting WHERE package_name IN (:packages)")
    public abstract List<PolicySetting> loadAllWithPackageName(String[] packages);

    @Query("SELECT * FROM policysetting WHERE permission IN (:permissions)")
    public abstract List<PolicySetting> loadAllWithPermission(String[] permissions);

    @Query("SELECT * FROM policysetting WHERE package_name = :packageName AND permission = :permission")
    public abstract PolicySetting getPolicy(String packageName, String permission);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract long insert(PolicySetting setting);

    @Delete
    public abstract void delete(PolicySetting setting);

    @Query("DELETE FROM policysetting WHERE id = :id")
    abstract void deleteById(int id);

    @Query("UPDATE policysetting SET policy_decision = :decision WHERE package_name = :packageName AND permission = :permission")
    public abstract void updatePolicy(String packageName, String permission, String decision);

    @Transaction
    public int upsert(PolicySetting setting) {
        int id = (int) insert(setting);
        if (id == -1) {
            updatePolicy(setting.packageName, setting.permission, setting.policyDecision);
            return getPolicy(setting.packageName, setting.permission).id;
        }
        return id;
    }
}
