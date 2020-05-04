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

import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.twosixlabs.privacycheckup.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UsageStatsHelper {
    private static final String TAG = UsageStatsHelper.class.getSimpleName();
    private static final String USAGE_CHANNEL = "USAGE_CHANNEL";
    private static final int NOTIFICATION_ID = 10007;

    private Context context = null;
    private NotificationManager nm = null;
    private PackageManager pm = null;

    public UsageStatsHelper(Context c) {
        this.context = c;
        this.nm = c.getSystemService(NotificationManager.class);
        this.pm = c.getPackageManager();

        initNotificationChannel();
    }

    /**
     *
     * @return True if this app has usage permissions granted to it
     */
    public boolean hasUsagePermissions() {
        boolean result = false;

        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);

            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, appInfo.uid, appInfo.packageName);
            Log.v(TAG, String.format("hasUsagePermissions() package=%s, uid=%s, mode=%d", appInfo.packageName, appInfo.uid, mode));
            result = (mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_DEFAULT);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find app info for " + context.getPackageName());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Display a notification to activate usage permissions for this app
     */
    public void requestUsagePermissions() {
        final Intent LAUNCH_USAGE_SETTINGS = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        final PendingIntent PENDING = PendingIntent.getActivity(context, 0, LAUNCH_USAGE_SETTINGS, 0);

        final NotificationCompat.Builder NOTIFICATION = new NotificationCompat.Builder(context, USAGE_CHANNEL)
        .setSmallIcon(R.drawable.ic_security_black_24dp)
        .setContentTitle("Usage permissions required")
        .setContentText("Tap here to enable usage access for Privacy Checkup")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(PENDING)
        .setAutoCancel(true);

        nm.notify(NOTIFICATION_ID, NOTIFICATION.build());
    }

    /**
     *
     * @return
     */
    public Map<String, Double> getAppProbabilities(Date start, Date end) {
        Map<String, Double> probabilities = new LinkedHashMap<>();

        if(hasUsagePermissions()) {
            Log.v(TAG, "getAppProbabilities() using weights from UsageStats");

            long startMillis = start.getTime();
            long endMillis = end.getTime();

            UsageStatsManager usm = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
            Map<String, UsageStats> aggregateStats = usm.queryAndAggregateUsageStats(startMillis, endMillis);

            long totalForeground = 0l;
            List<AppTime> appTimes = new ArrayList<>(aggregateStats.keySet().size());
            for(String app : aggregateStats.keySet()) {
                if(!isExempt(app)) {
                    long foregroundTime = aggregateStats.get(app).getTotalTimeInForeground();
                    if(foregroundTime > 0l) {
                        appTimes.add(new AppTime(app, foregroundTime));
                        totalForeground += foregroundTime;
                    }
                }
            }

            Collections.sort(appTimes, new AppTimeComparator());
            Collections.reverse(appTimes);
            for(AppTime appTime : appTimes) {
                String app = appTime.packageName;
                long foregroundTime = appTime.time;
                double probability = (foregroundTime * 1.0d) / totalForeground;

                probabilities.put(app, probability);
                Log.v(TAG, String.format("App=%s, Foreground=%d, Total=%d, Prob=%f", app, foregroundTime, totalForeground,probability));
            }

        } else {
            Log.v(TAG, "getAppProbabilities() using uniform weights");

            final PackageManager PM = context.getPackageManager();
            List<ApplicationInfo> appInfos = PM.getInstalledApplications(PackageManager.GET_META_DATA);
            List<String> allowedPackageNames = new ArrayList<>(appInfos.size());

            for(ApplicationInfo ai : appInfos) {
                String packageName = ai.packageName;
                if(!isExempt(packageName)) {
                    allowedPackageNames.add(packageName);
                }
            }

            if(!allowedPackageNames.isEmpty()) {
                double uniformProbability = 1.0 / (1.0 * allowedPackageNames.size());
                for(String packageName : allowedPackageNames) {
                    probabilities.put(packageName, uniformProbability);
                }
            }
        }

        return probabilities;
    }

    private void initNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(USAGE_CHANNEL) == null) {
            final CharSequence NAME = "Usage Check";
            final String DESCRIPTION = "Alert the user to enable UsageStats access";
            final int IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(USAGE_CHANNEL, NAME, IMPORTANCE);
            channel.setDescription(DESCRIPTION);

            Log.i(TAG, "Registering notification channel " + USAGE_CHANNEL);
            nm.createNotificationChannel(channel);
        }
    }

    /**
     * Helper function to identify apps to skip: the current launcher, system apps, and this policy manager
     * @param packageName App package name
     * @return True if the app should be exempt from usage reports
     */
    private boolean isExempt(String packageName) {
        if(packageName.equals(context.getPackageName())) {
            Log.v(TAG, packageName + " is this policy manager, skipping");
            return true;
        }

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if(ri != null && ri.activityInfo.packageName.equals(packageName)) {
            Log.v(TAG, packageName + " is the current launcher, skipping");
            return true;
        }

        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            if((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                Log.v(TAG, packageName + " is a system package, skipping");
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    private class AppTime {
        public String packageName = null;
        public long time = Long.MIN_VALUE;

        public AppTime(String packageName, long time) {
            this.packageName = packageName;
            this.time = time;
        }
    }


    private class AppTimeComparator implements Comparator<AppTime> {
        @Override
        public int compare(AppTime o1, AppTime o2) {
            long diff = o1.time - o2.time;
            if(diff > 0l) {
                return 1;
            } else if(diff == 0) {
                return 0;
            } else {
                return -1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
}
