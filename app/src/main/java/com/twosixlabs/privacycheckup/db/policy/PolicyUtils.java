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

import android.content.Context;

import com.twosixlabs.privacycheckup.db.DatabaseClient;

import java.util.List;

public class PolicyUtils {

    private static final String TAG = "PolicyUtils";

    /**
     * Policy decision to allow a request
     */
    public static final String POLICY_ALLOW = "allow";

    /**
     * Policy decision to deny a request
     */
    public static final String POLICY_DENY = "deny";

    /**
     * Policy decision to deny the request if it is made in the background, otherwise allow it
     */
    public static final String POLICY_DENY_BG = "deny_background";

    /**
     * Get all PolicySettings (Normal android apps maing standard permission requests)
     * @param context Context used to access database
     * @return list of all policies
     */
    public static List<PolicySetting> getAllPolicies(Context context) {
        return DatabaseClient.getInstance(context).getAppDatabase()
                .policySettingDao().getAll();
    }

    /**
     * Get all PurposePolicySettings (PE Android apps making standard permission requests)
     * @param context Context used to access database
     * @return list of all policies
     */
    public static List<PurposePolicySetting> getAllPurposePolicies(Context context) {
        return DatabaseClient.getInstance(context).getAppDatabase()
                .purposePolicySettingDao().getAll();
    }

    /**
     * Get all PalPolicySettings (PE Android apps making uPAL requests)
     * @param context Context used to access database
     * @return list of all policies
     */
    public static List<PalPolicySetting> getAllPalPolicies(Context context) {
        return DatabaseClient.getInstance(context).getAppDatabase()
                .palPolicySettingDao().getAll();
    }


    /**
     * Get the policy for an onDangerousPermission check. If purpose is specified, tries to query
     * PurposePolicySetting table. If not found, or the purpose was null, falls back to PolicySetting table
     * @param context Context used to access the database
     * @param packageName package to get policy for
     * @param permission permission to get policy for
     * @param purpose purpose to get policy for, can be null
     * @return The policy, one of {@value #POLICY_ALLOW}, {@value #POLICY_DENY}, {@value POLICY_DENY_BG}
     *         or null if the policy is not found
     */
    public static String checkDangerousPermissionRequestPolicy(Context context, String packageName, String permission, String purpose) {
        PolicySetting setting = null;
        if (purpose != null) {
            setting = DatabaseClient.getInstance(context).getAppDatabase()
                    .purposePolicySettingDao().getPolicy(packageName, permission, purpose);
        }
        if (setting == null) {
            setting = DatabaseClient.getInstance(context).getAppDatabase()
                    .policySettingDao().getPolicy(packageName, permission);
        }
        return setting != null ? setting.policyDecision : null;
    }

    /**
     * Get the policy for a uPAL module request. Returns null if the policy does not exist
     * @param context Context used to access database
     * @param packageName package to get policy for
     * @param permission permission to get policy for
     * @param purpose purpose to get policy for
     * @param pal pal to get policy for
     * @return The policy, one of {@value #POLICY_ALLOW}, {@value #POLICY_DENY}
     * or null if the policy is not found
     */
    public static String checkPrivateDataRequestPolicy(Context context, String packageName, String permission, String purpose, String pal) {
        PolicySetting setting = null;
        if (purpose != null) {
            setting = DatabaseClient.getInstance(context).getAppDatabase()
                    .palPolicySettingDao().getPolicy(packageName, permission, purpose, pal);
        }
        if (setting == null) {
            setting = DatabaseClient.getInstance(context).getAppDatabase()
                    .palPolicySettingDao().getPolicy(packageName, permission, pal);
        }
        return setting != null ? setting.policyDecision : null;
    }

    /**
     * Set the policy for the package/permission pair. Updates existing entry, or creates a new one
     * if needed
     * @param context context used to access database
     * @param packageName requesting package name
     * @param permission requested permission
     * @param decision Policy decision. Should be {@value #POLICY_ALLOW}, {@value #POLICY_DENY}. or {@value POLICY_DENY_BG}
     * @return id of new or updated entry
     */
    public static int setPolicy(Context context, String packageName, String permission, String decision) {
        PolicySetting setting = new PolicySetting();
        setting.packageName = packageName;
        setting.permission = permission;
        setting.policyDecision = decision;
        return DatabaseClient.getInstance(context).getAppDatabase().policySettingDao().upsert(setting);
    }

    /**
     * Set the policy for the package/permission/purpose combo. Updates existing entry, or creates a new one
     * if needed
     * @param context context used to access database
     * @param packageName requesting package name
     * @param permission requested permission
     * @param purpose requested purpose
     * @param decision Policy decision. Should be {@value #POLICY_ALLOW}, {@value #POLICY_DENY}. or {@value POLICY_DENY_BG}
     * @return id of new or updated entry
     */
    public static int setPurposePolicy(Context context, String packageName, String permission, String purpose, String decision) {
        PurposePolicySetting setting = new PurposePolicySetting();
        setting.packageName = packageName;
        setting.permission = permission;
        setting.purpose = purpose;
        setting.policyDecision = decision;
        return DatabaseClient.getInstance(context).getAppDatabase().purposePolicySettingDao().upsert(setting);
    }

    /**
     * Set the policy for a pal request. Updates existing entry, or creates a new one if needed
     * @param context context used to access database
     * @param packageName requesting package name
     * @param permission requested permission
     * @param purpose requested purpose
     * @param pal requested pal
     * @param decision Policy decision, either {@value #POLICY_ALLOW} or {@value #POLICY_DENY}
     * @return id of new or updated entry
     */
    public static int setPalPolicy(Context context, String packageName, String permission, String purpose, String pal, String decision) {
        PalPolicySetting setting = new PalPolicySetting();
        setting.packageName = packageName;
        setting.permission = permission;
        setting.purpose = purpose;
        setting.pal = pal;
        setting.policyDecision = decision;
        return DatabaseClient.getInstance(context).getAppDatabase().palPolicySettingDao().upsert(setting);
    }

    public static void deletePolicy(Context context, int id) {
        DatabaseClient.getInstance(context).getAppDatabase().policySettingDao().deleteById(id);
    }

    public static void deletePalPolicy(Context context, int id) {
        DatabaseClient.getInstance(context).getAppDatabase().palPolicySettingDao().deleteById(id);
    }

}
