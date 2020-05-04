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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface DangerousPermissionRequestDao {

    @Query("SELECT * FROM dangerouspermissionrequest")
    List<DangerousPermissionRequest> getAll();

    @Query("SELECT * FROM dangerouspermissionrequest WHERE package_name IN (:packages)")
    List<DangerousPermissionRequest> loadAllWithPackageName(String[] packages);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE permission IN (:permissions)")
    List<DangerousPermissionRequest> loadAllWithPermission(String[] permissions);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE purpose IN (:purposes)")
    List<DangerousPermissionRequest> loadAllWithPurpose(String[] purposes);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE timestamp >= :start AND timestamp <= :end")
    List<DangerousPermissionRequest> loadAllInRange(Date start, Date end);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE package_name IN (:packages) AND timestamp >=:start AND timestamp <= :end")
    List<DangerousPermissionRequest> loadAllWithPackageNameInRange(String[] packages, Date start, Date end);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE package_name IN (:packages) AND timestamp >=:start AND timestamp <= :end AND allowed = :allowed")
    List<DangerousPermissionRequest> loadAllAllowedWithPackageNameInRange(String[] packages, Date start, Date end, boolean allowed);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE permission IN (:permissions) AND timestamp >=:start AND timestamp <= :end")
    List<DangerousPermissionRequest> loadAllWithPermissionInRange(String[] permissions, Date start, Date end);

    @Query("SELECT * FROM dangerouspermissionrequest WHERE purpose IN (:purpose) AND timestamp >=:start AND timestamp <= :end")
    List<DangerousPermissionRequest> loadAllWithPurposeInRange(String[] purpose, Date start, Date end);

    @Query("SELECT * from dangerouspermissionrequest WHERE allowed = :allowed")
    List<DangerousPermissionRequest> loadAllByAllowed(boolean allowed);

    @Insert
    void insert(DangerousPermissionRequest request);

    @Delete
    void delete(DangerousPermissionRequest request);

    @Update
    void update(DangerousPermissionRequest request);

}
