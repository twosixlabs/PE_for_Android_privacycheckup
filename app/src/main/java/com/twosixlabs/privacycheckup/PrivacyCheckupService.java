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

package com.twosixlabs.privacycheckup;

import android.app.policy.PolicyManagerService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.twosixlabs.privacycheckup.db.*;
import com.twosixlabs.privacycheckup.db.policy.*;
import com.twosixlabs.privacycheckup.db.requests.*;
import com.twosixlabs.privacycheckup.notification.PrivacyNotifier;
import com.twosixlabs.privacycheckup.stackcache.AbstractStackCache;
import com.twosixlabs.privacycheckup.stackcache.SharedPrefsStackCache;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static com.twosixlabs.privacycheckup.db.policy.PolicyUtils.POLICY_ALLOW;
import static com.twosixlabs.privacycheckup.db.policy.PolicyUtils.POLICY_DENY;
import static com.twosixlabs.privacycheckup.db.policy.PolicyUtils.POLICY_DENY_BG;

public class PrivacyCheckupService extends PolicyManagerService {
    public static final String NOTIFICATION_ACTION = PrivacyCheckupService.class.getSimpleName() + ".notification";
    public static final String NOTIFICATION_PACKAGE = NOTIFICATION_ACTION + ".package_name";
    public static final String NOTIFICATION_LENGTH_MINUTES = NOTIFICATION_ACTION + ".length_minutes";
    public static final String NOTIFICATION_TIME_START_MILLIS = NOTIFICATION_ACTION + ".time_start_millis";
    public static final String NOTIFICATION_TIME_END_MILLIS = NOTIFICATION_ACTION + ".time_end_millis";

    public static final String SHARED_PREFS = "PrivacyCheckupPrefs";

    public PrivacyNotifier notifier = null;

    private final IntentFilter NOTIFICATION_FILTER = new IntentFilter(NOTIFICATION_ACTION);
    private final BroadcastReceiver NOTIFICATION_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Figure out the time range for the pop-up stats
            long startTimeMillis = 0l;
            long endTimeMillis = 0l;
            if(intent.hasExtra(NOTIFICATION_LENGTH_MINUTES)) {
                // Get stats for the last X minutes
                long intervalMillis = intent.getIntExtra(NOTIFICATION_LENGTH_MINUTES, 0) * 60l * 1000l;

                endTimeMillis = System.currentTimeMillis();
                startTimeMillis = endTimeMillis - intervalMillis;
            } else if(intent.hasExtra(NOTIFICATION_TIME_START_MILLIS) && intent.hasExtra(NOTIFICATION_TIME_END_MILLIS)) {
                // Get stats between the given start and end times
                startTimeMillis = intent.getLongExtra(NOTIFICATION_TIME_START_MILLIS, 1l);
                endTimeMillis = intent.getLongExtra(NOTIFICATION_TIME_END_MILLIS, 1l);
            } else {
                // Get stats for the last `getNotificationMintervalMins()` minutes
                long intervalMillis = notifier.getNotificationIntervalMins() * 60l * 1000l;

                endTimeMillis = System.currentTimeMillis();
                startTimeMillis = endTimeMillis - intervalMillis;
            }

            final Date START_TIME = new Date(startTimeMillis);
            final Date END_TIME = new Date(endTimeMillis);
            long intervalMillis = endTimeMillis - startTimeMillis;

            // Figure out what app to display in the pop-up stats
            String packageName = null;
            if(intent.hasExtra(NOTIFICATION_PACKAGE)) {
                // Use the specified app
                packageName = intent.getStringExtra(NOTIFICATION_PACKAGE);
            } else {
                // Randomly draw an app
                packageName = notifier.getRandomAppWeightedByUsage(START_TIME, END_TIME);
            }
            final String PACKAGE_NAME = packageName;

            if(PACKAGE_NAME != null && intervalMillis > 0l) {
                new SendNotificationTask(PrivacyCheckupService.this, PACKAGE_NAME).execute(START_TIME, END_TIME);
            }
        }
    };

    protected static boolean isRunning = false;

    public static final String TAG = PrivacyCheckupService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        notifier = new PrivacyNotifier(this);
        registerReceiver(NOTIFICATION_RECEIVER, NOTIFICATION_FILTER);
        LocalBroadcastManager.getInstance(this).registerReceiver(NOTIFICATION_RECEIVER, NOTIFICATION_FILTER);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        unregisterReceiver(NOTIFICATION_RECEIVER);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(NOTIFICATION_RECEIVER);
    }

    @Override
    public boolean onAppInstall(String packageName, String odp) {
        return true;
    }

    @Override
        public void onPrivateDataRequest(String packageName, String permission, String purpose, String pal, String desc, ResultReceiver recv) {

        PrivateDataRequest req = new PrivateDataRequest();
        req.packageName = packageName;
        req.permission = permission;
        req.purpose = purpose;
        req.pal = pal;
        req.timestamp = new Date();

        Log.v(TAG, "Checking policy for " + req.getShortString());

        String policy = PolicyUtils.checkPrivateDataRequestPolicy(this, packageName, permission, null, pal);
        if (policy == null) {
            // TODO: add a default policy if we can't find one
            Log.v(TAG, String.format("No PAL policy found for {%s, %s, %s, %s}, allowing",
                                     packageName, permission, purpose, pal));
            returnPolicyDecision(true, recv);
            req.allowed = true;
        } else {
            Log.v(TAG, "Policy is " + policy);
            boolean allowed = policy.equals(POLICY_ALLOW);
            returnPolicyDecision(allowed, recv);
            req.allowed = allowed;
        }

        DatabaseClient.getInstance(this).getAppDatabase().privateDataRequestDao().insert(req);
        getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit().putString(pal, desc).apply();
        issuePeriodicNotification();
    }

    @Override
    public void onDangerousPermissionRequest(String packageName, String permission, String purpose, List<StackTraceElement[]> stackTrace,
                                             int flags, ComponentName callingComponent, ComponentName topActivity, ResultReceiver recv) {

        DangerousPermissionRequest req = new DangerousPermissionRequest();
        req.packageName = packageName;
        req.permission = permission;
        req.purpose = purpose;
        req.stackTrace = (stackTrace == null ? null : stackTraceToString(stackTrace));  // TODO: add this
        req.isPrivileged = (flags & PolicyManagerService.FROM_PRIV_APP_REQ) != 0;
        req.isSystem = (flags & PolicyManagerService.FROM_SYS_APP_REQ) != 0;
        req.isBackground = (topActivity == null) || !topActivity.getPackageName().equals(packageName);
        req.callingComponent = callingComponent;
        req.topActivity = topActivity;
        req.timestamp = new Date();

        Log.v(TAG, "Checking policy for " + req.getShortString());

        String policy = PolicyUtils.checkDangerousPermissionRequestPolicy(this, packageName, permission, purpose);
        if (policy == null) {
            // TODO: fall back to global policy settings
            Log.v(TAG, String.format("No policy found for {%s, %s, %s}, allowing",
                                        packageName, permission, purpose));
            returnPolicyDecision(true, recv);
            req.allowed = true;
        } else {
            // TODO: clean up this logic a bit
            Log.v(TAG, "Policy is " + policy);
            switch (policy) {
                case POLICY_ALLOW:
                    returnPolicyDecision(true, recv);
                    req.allowed = true;
                    break;
                case POLICY_DENY_BG:
                    if (req.isBackground) {
                        Log.v(TAG, "Denying background request");
                        returnPolicyDecision(false, recv);
                        req.allowed = false;
                        break;
                    } else {
                        Log.v(TAG, "Allowing background request");
                        returnPolicyDecision(true, recv);
                        req.allowed = true;
                        break;
                    }
                case POLICY_DENY:
                default:
                    Log.v(TAG, "Denying request");
                    returnPolicyDecision(false, recv);
                    req.allowed = false;
            }
        }
        DatabaseClient.getInstance(this).getAppDatabase().dangerousPermissionRequestDao().insert(req);
        issuePeriodicNotification();
    }

    private String stackTraceToString(List<StackTraceElement[]> stackTraceList) {
        String st = null;
        AbstractStackCache cache = new SharedPrefsStackCache(this);

        if(stackTraceList != null) {
            StringBuffer outerBuffer = new StringBuffer();
            outerBuffer.append("{");

            for(int m = 0; m < stackTraceList.size(); m++) {
                StackTraceElement[] stackTrace = stackTraceList.get(m);

                StringBuffer innerBuffer = new StringBuffer();
                innerBuffer.append("[");

                for(int n = 0; n < stackTrace.length; n++) {
                    String stackElement = cache.putInCache(stackTrace[n]);
                    boolean isLastInner = n == stackTrace.length - 1;
                    if(isLastInner) {
                        innerBuffer.append(stackElement);
                    } else {
                        innerBuffer.append(stackElement + ",");
                    }
                }

                innerBuffer.append("}");

                boolean isLastOuter = m == stackTraceList.size() - 1;
                if(isLastOuter) {
                    outerBuffer.append(innerBuffer);
                } else {
                    outerBuffer.append(innerBuffer);
                    outerBuffer.append(",");
                }
            }

            outerBuffer.append("}");
            st = outerBuffer.toString();
        }

        return st;
    }

    private void returnPolicyDecision(boolean allow, ResultReceiver recv) {
        Bundle b = new Bundle();
        b.putBoolean("allowPerm", allow);
        recv.send(0, b);

    }

    private void issuePeriodicNotification() {
        if(!notifier.hasUsagePermissions()) {
            notifier.requestUsagePermissions();
        }

        if (notifier.getLastNotificationTimeMillis() == 0) {
            notifier.setLastNotificationTimeToNow();
        }

        if(notifier.isTimeToNotify()) {
            Date rangeStart = new Date(notifier.getLastNotificationTimeMillis());
            Date rangeEnd = new Date();

            String randomApp = notifier.getRandomAppWeightedByUsage(rangeStart, rangeEnd);
            if(randomApp != null) {
                Log.i(TAG, "Issuing privacy notification for random app " + randomApp);

                Intent notify = new Intent(NOTIFICATION_ACTION);
                notify.putExtra(NOTIFICATION_PACKAGE, randomApp);
                notify.putExtra(NOTIFICATION_TIME_START_MILLIS, rangeStart.getTime());
                notify.putExtra(NOTIFICATION_TIME_END_MILLIS, rangeEnd.getTime());

                notifier.setLastNotificationTimeToNow();
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(notify);
            } else {
                Log.w(TAG, String.format("Received null random app between %s and %s",
                                          rangeStart.toString(), rangeEnd.toString()));
            }
        }
    }

    private static class SendNotificationTask extends AsyncTask<Date, Void, Void> {

        private WeakReference<PrivacyCheckupService> pmRef;
        private String packageName;

        SendNotificationTask(PrivacyCheckupService pm, String packageName) {
            pmRef = new WeakReference<>(pm);
            this.packageName = packageName;
        }

        @Override
        protected Void doInBackground(Date... dates) {
            Date startTime = dates[0];
            Date endTime = dates[1];
            int permissionCount = pmRef.get().notifier.getTopPermissionCount();
            String title = pmRef.get().notifier.generateTitle(startTime);
            String body = pmRef.get().notifier.generateBody(packageName, permissionCount, startTime, endTime);
            pmRef.get().sendPrivacyNotification(packageName, title, body);
            return null;
        }

    }
}
