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
public interface PrivateDataRequestDao {

    @Query("SELECT * FROM privatedatarequest")
    List<PrivateDataRequest> getAll();

    @Query("SELECT * FROM privatedatarequest WHERE package_name IN (:packages)")
    List<PrivateDataRequest> loadAllByPackageName(String[] packages);

    @Query("SELECT * FROM privatedatarequest WHERE permission IN (:permissions)")
    List<PrivateDataRequest> loadAllByPermission(String[] permissions);

    @Query("SELECT * FROM privatedatarequest WHERE purpose IN (:purposes)")
    List<PrivateDataRequest> loadAllByPurpose(String[] purposes);

    @Query("SELECT * FROM privatedatarequest WHERE pal IN (:pals)")
    List<PrivateDataRequest> loadAllByPal(String[] pals);

    @Query("SELECT * FROM privatedatarequest WHERE timestamp >= :start AND timestamp <= :end")
    List<PrivateDataRequest> loadAllInRange(Date start, Date end);

    @Query("SELECT * FROM privatedatarequest WHERE package_name IN (:packages) AND timestamp >=:start AND timestamp <= :end")
    List<PrivateDataRequest> loadAllWithPackageNameInRange(String[] packages, Date start, Date end);

    @Query("SELECT * FROM privatedatarequest WHERE permission IN (:permissions) AND timestamp >=:start AND timestamp <= :end")
    List<PrivateDataRequest> loadAllWithPermissionInRange(String[] permissions, Date start, Date end);

    @Query("SELECT * FROM privatedatarequest WHERE purpose IN (:purpose) AND timestamp >=:start AND timestamp <= :end")
    List<PrivateDataRequest> loadAllWithPurposeInRange(String[] purpose, Date start, Date end);

    @Query("SELECT * FROM privatedatarequest WHERE pal IN (:pals) AND timestamp >=:start AND timestamp <= :end")
    List<PrivateDataRequest> loadAllWithPalInRange(String[] pals, Date start, Date end);

    @Query("SELECT * from privatedatarequest WHERE allowed = :allowed")
    List<PrivateDataRequest> loadAllByAllowed(boolean allowed);

    @Insert
    void insert(PrivateDataRequest request);

    @Delete
    void delete(PrivateDataRequest request);

    @Update
    void update(PrivateDataRequest request);

}

