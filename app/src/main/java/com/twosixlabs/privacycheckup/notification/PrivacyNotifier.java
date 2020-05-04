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

package com.twosixlabs.privacycheckup.notification;

import android.app.policy.PolicyManagerService;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;

import com.twosixlabs.privacycheckup.PrivacyCheckupService;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class PrivacyNotifier {
    private static final String TAG = PrivacyNotifier.class.getSimpleName();

    private static final String APP_PREFS_NAME = "PrivacyCheckupService.AppPrefs";

    // When the last notification occurred
    private static final String APP_PREFS_KEY_LAST_NOTIFICATION_TIME = "pref.global_last_time";

    // Preference for how much time between global notifications
    private static final String APP_PREFS_KEY_INTERVAL_MINS = "pref.global_interval_mins";
    private static final int APP_PREFS_DEFAULT_INTERVAL_MINS = 60 * 24;

    // Preference for the size of the binning window
    private static final String APP_PREFS_KEY_BIN_MINS = "pref.window_mins";
    private static final int APP_PREFS_DEFAULT_BIN_MINS = 1;

    // Preference for how many top permissions should be shown in the notification
    private static final String APP_PREFS_KEY_PERMS_COUNT = "pref.top_perms_count";
    private static final int APP_PREFS_DEFAULT_PERMS_COUNT = 3;

    private PolicyManagerService policyManager = null;
    private UsageStatsHelper usageStatsHelper = null;
    private SharedPreferences prefs = null;

    /**
     * Create an object to build privacy channel notifications
     * @param policyManager Calling policy manager
     */
    public PrivacyNotifier(PolicyManagerService policyManager) {
        this.policyManager = policyManager;
        this.usageStatsHelper = new UsageStatsHelper(policyManager);
        this.prefs = policyManager.getSharedPreferences(APP_PREFS_NAME, PrivacyCheckupService.MODE_PRIVATE);
    }

    /**
     *
     * @param rangeStart When the statistics begin
     * @return A notification title in the form of "Since MMMM dd yyyy hh:mm a"
     */
    public String generateTitle(Date rangeStart) {
        final SimpleDateFormat FORMAT = new SimpleDateFormat("MMMM dd yyyy hh:mm a");
        String title = String.format("Since %s", FORMAT.format(rangeStart));
        return title;
    }

    /**
     *
     * @param packageName App package name
     * @param topN Show the topN most used permissions
     * @param rangeStart When the statistics begin
     * @param rangeEnd When the statistics end
     * @return A notification body listing the most used permissions in the time range, binned
     * in windows the size of getBinWidthMins()
     */
    public String generateBody(String packageName, int topN, Date rangeStart, Date rangeEnd) {
        int binningMinutes = getBinWidthMins();
        long binningMillis = binningMinutes * 60l * 1000l;

        // Get the permission usage stats
        AppPermissionStats stats = new AppPermissionStats(packageName,
                rangeStart,
                rangeEnd,
                binningMillis,
                policyManager);
        Map<String, Integer> foregroundRequests = stats.getPermissionBinCounts(false);
        Map<String, Integer> backgroundRequests = stats.getPermissionBinCounts(true);

        Map<String, Integer> allRequests = new TreeMap<>(foregroundRequests);
        for(String perm : backgroundRequests.keySet()) {
            int value = backgroundRequests.get(perm);
            if(allRequests.containsKey(perm)) {
                value += allRequests.get(perm);
            }

            allRequests.put(perm, value);
        }

        // Build the body text
        StringBuffer sb = new StringBuffer();

        String summary = topSummary(allRequests, topN);
        if(summary != null) {
            sb.append("This app accessed the following resources [x] times:");
            sb.append(summary);

            sb.append("\n\nTap here to find out more");
        } else {
            sb.append("This app did not access sensitive resources");
        }

        return sb.toString();
    }

    /**
     *
     * @return True if Usage access is granted to this policy manager
     */
    public boolean hasUsagePermissions() {
        return usageStatsHelper.hasUsagePermissions();
    }

    /**
     * Issue a pop-up notification to request Usage access
     */
    public void requestUsagePermissions() {
        usageStatsHelper.requestUsagePermissions();
    }

    /**
     * Update the last notification time to the current time
     */
    public void setLastNotificationTimeToNow() {
        setLastNotificationTimeMillis(System.currentTimeMillis());
    }

    /**
     * Set the last notification time to the specified value
     * @param timestampMillis
     */
    public void setLastNotificationTimeMillis(long timestampMillis) {
        Log.i(TAG, "Setting last global notification time to " + timestampMillis);
        prefs.edit().putLong(APP_PREFS_KEY_LAST_NOTIFICATION_TIME, timestampMillis).apply();
    }

    /**
     *
     * @return The timestamp of the last saved notification time, in milliseconds
     */
    public long getLastNotificationTimeMillis() {
        if(!prefs.contains(APP_PREFS_KEY_LAST_NOTIFICATION_TIME)) {
            setLastNotificationTimeMillis(0l);
        }

        return prefs.getLong(APP_PREFS_KEY_LAST_NOTIFICATION_TIME, 0l);
    }

    /**
     *
     * @return True if it's been longer than the interval time since the last recorded notification
     */
    public boolean isTimeToNotify() {
        long currentTimeMillis = System.currentTimeMillis();
        long lastNotificationTimeMillis = getLastNotificationTimeMillis();
        long intervalMillis = getNotificationIntervalMins() * 60l * 1000l;

        Date nextNotification = new Date(lastNotificationTimeMillis + intervalMillis);
        Log.v(TAG, "Will be time to notify again after " + nextNotification.toString());

        return currentTimeMillis - lastNotificationTimeMillis >= intervalMillis;
    }

    /**
     * Set the minimum number of minutes between notifications
     * @param minutes
     */
    public void setNotificationIntervalMins(int minutes) {
        Log.i(TAG, "Setting global notification interval to " + minutes + " minutes");
        prefs.edit().putInt(APP_PREFS_KEY_INTERVAL_MINS, minutes).apply();
    }

    /**
     *
     * @return The minimum number of minutes between notifications
     */
    public int getNotificationIntervalMins() {
        if(!prefs.contains(APP_PREFS_KEY_INTERVAL_MINS)) {
            setNotificationIntervalMins(APP_PREFS_DEFAULT_INTERVAL_MINS);
        }

        return prefs.getInt(APP_PREFS_KEY_INTERVAL_MINS, APP_PREFS_DEFAULT_INTERVAL_MINS);
    }

    /**
     * Specify the width of the binning window, in minutes
     * @param binningMinutes
     */
    public void setBinWidthMins(int binningMinutes) {
        Log.i(TAG, "Setting bin width to " + binningMinutes + " minutes");
        prefs.edit().putInt(APP_PREFS_KEY_BIN_MINS, binningMinutes).apply();
    }

    /**
     *
     * @return The specified width of the binning window, in minutes
     */
    public int getBinWidthMins() {
        if(!prefs.contains(APP_PREFS_KEY_BIN_MINS)) {
            setBinWidthMins(APP_PREFS_DEFAULT_BIN_MINS);
        }

        return prefs.getInt(APP_PREFS_KEY_BIN_MINS, APP_PREFS_DEFAULT_BIN_MINS);
    }

    /**
     * Set the maximum number of top permissions to be shown in the privacy notification body
     * @param permissionCount
     */
    public void setTopPermissionCount(int permissionCount) {
        Log.i(TAG, "Top permission count preference to " + permissionCount);
        prefs.edit().putInt(APP_PREFS_KEY_PERMS_COUNT, permissionCount).apply();
    }

    /**
     *
     * @return The maximum number of permissions to show in the privacy notification body (all
     * nth-place ties are shown)
     */
    public int getTopPermissionCount() {
        if(!prefs.contains(APP_PREFS_KEY_PERMS_COUNT)) {
            setTopPermissionCount(APP_PREFS_DEFAULT_PERMS_COUNT);
        }

        return prefs.getInt(APP_PREFS_KEY_PERMS_COUNT, APP_PREFS_DEFAULT_PERMS_COUNT);
    }

    /**
     * Draw a random app, with probabilities proportional to the amount of foreground time it had
     * as reported by UsageStats. Excludes this policy manager, the launcher, and system apps.
     * @param rangeStart Beginning of the time range for foreground time stats
     * @param rangeEnd End of the time range for foreground time stats
     * @return App package name
     */
    public String getRandomAppWeightedByUsage(Date rangeStart, Date rangeEnd) {
        String randomApp = null;

        Map<String, Double> probabilities = usageStatsHelper.getAppProbabilities(rangeStart, rangeEnd);
        if(!probabilities.isEmpty()) {
            Random rng = new Random();
            double randomValue = rng.nextDouble();
            Log.v(TAG, "randomValue=" + randomValue);

            double accumulator = 0.0d;
            for(String app : probabilities.keySet()) {
                accumulator += probabilities.get(app);

                if(randomValue <= accumulator) {
                    Log.v(TAG, String.format("%f =< %f, returning %s", randomValue, accumulator, app));
                    randomApp = app;
                    break;
                } else {
                    Log.v(TAG, String.format("%f > %f, continuing to the next one", randomValue, accumulator));
                }
            }
        } else {
            Log.w(TAG, "No app usage data available between " + rangeStart.toString() +
                    " and " + rangeEnd.toString());
        }

        return randomApp;
    }


    private String topSummary(Map<String, Integer> permCounts, int topN) {
        String summary = null;

        if(!permCounts.isEmpty()) {
            List<String> concat = new LinkedList<>();
            for(String permission : permCounts.keySet()){
                int count = permCounts.get(permission);
                concat.add(String.format("%04d:%s", count, permission));
            }
            Collections.sort(concat);
            Collections.reverse(concat);

            StringBuffer sb = new StringBuffer();
            int n = 0;
            int lastCount = -1;
            for(String line : concat) {
                String permission = line.split(":")[1];
                int count = permCounts.get(permission);

                if(n >= topN && count != lastCount) {
                    break;
                }

                String readable = getReadablePermission(permission);
                sb.append(String.format("\n[%d] %s", count, readable));

                lastCount = count;
                n++;
            }

            summary = sb.toString();
        }

        return summary;
    }

    private String getReadablePermission(String permission) {
        String readable = permission;

        try {
            PackageManager pm = policyManager.getPackageManager();
            PermissionInfo info = pm.getPermissionInfo(permission, 0);
            readable = info.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Exception when querying PackageManager about " + permission);
            e.printStackTrace();
        }

        return readable;
    }
}
