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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.twosixlabs.privacycheckup.db.DatabaseClient;
import com.twosixlabs.privacycheckup.db.policy.PalPolicySetting;
import com.twosixlabs.privacycheckup.db.policy.PolicySetting;
import com.twosixlabs.privacycheckup.db.policy.PolicyUtils;
import com.twosixlabs.privacycheckup.db.requests.DangerousPermissionRequest;
import com.twosixlabs.privacycheckup.db.requests.PrivateDataRequest;
import com.twosixlabs.privacycheckup.stackcache.AbstractStackCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PolicyActivity extends AppCompatActivity
        implements AddPolicyDialogFragment.AddPolicyDialogListener,
        AddPalPolicyDialogFragment.AddPalPolicyDialogListener {

    private static final String TAG = PrivacyCheckupService.class.getSimpleName();

    LinearLayout mPolicyListLayout;
    LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create menu bar
        Toolbar myToolbar = findViewById(R.id.app_bar);
        myToolbar.setOverflowIcon(getDrawable(R.drawable.ic_more_vert_white_24dp));
        setSupportActionBar(myToolbar);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mPolicyListLayout = findViewById(R.id.policies);

        Intent intent = getIntent();

        Bundle extras = intent.getExtras();
        if (extras != null) {
            String packageName = null;
            String permissionName = null;

            if (extras.containsKey("PACKAGE_NAME")) {
                 packageName = extras.getString("PACKAGE_NAME");
            }

            if(extras.containsKey("PERMISSION_NAME")){
                permissionName = extras.getString("PERMISSION_NAME");
            }

            // If a permission name and package name are provided, show the add dialog
            if(permissionName != null && packageName != null){
                AddPolicyDialogFragment addDialog = new AddPolicyDialogFragment();

                Bundle args = new Bundle();
                args.putString("PERMISSION_NAME", permissionName);
                args.putString("PACKAGE_NAME", packageName);
                addDialog.setArguments(args);

                addDialog.show(getSupportFragmentManager(), "addDialog");
            }else if(packageName != null && permissionName == null){
                Toast.makeText(this, "Showing policies for " + packageName, Toast.LENGTH_LONG).show();
                new LoadPoliciesForPackageTask(this, packageName).execute();
            }
        }else{
            new LoadPoliciesTask(this).execute();
            new LoadPalPoliciesTask(this).execute();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if the service is running and show an error if it's not
        if (!PrivacyCheckupService.isRunning) {
            View contentView = findViewById(R.id.app_content);
            contentView.setVisibility(View.GONE);
            View errorView = findViewById(R.id.app_error_content);
            errorView.setVisibility(View.VISIBLE);
        } else {
            View errorView = findViewById(R.id.app_error_content);
            errorView.setVisibility(View.GONE);
            View contentView = findViewById(R.id.app_content);
            contentView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_trigger:
                // Trigger the notification
                Intent notify = new Intent(PrivacyCheckupService.NOTIFICATION_ACTION);
                notify.putExtra(PrivacyCheckupService.NOTIFICATION_PACKAGE, "com.facebook.katana");
                notify.putExtra(PrivacyCheckupService.NOTIFICATION_LENGTH_MINUTES, 60);

                LocalBroadcastManager.getInstance(this).sendBroadcast(notify);
                return true;

            case R.id.action_add:
                // show add package UI
                AddPolicyDialogFragment addDialog = new AddPolicyDialogFragment();
                addDialog.show(getSupportFragmentManager(), "addDialog");
                return true;
            case R.id.action_add_pal:
                new AddPalPolicyDialogTask(this).execute();
                return true;
            case R.id.action_load_policies:
                new LoadTestPoliciesTask(this).execute();
            case R.id.action_dump_db:
                new DumpDbTask(this).execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String packageName, String permission, String decision) {
        new AddPolicyTask(this).execute(packageName, permission, decision);
    }

    @Override
    public void onPalDialogPositiveClick(DialogFragment dialog, String packageName, String pal, String permission, String purpose, String decision) {
        new AddPalPolicyTask(this).execute(packageName, pal, permission, purpose, decision);
    }

    private static class DumpDbTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> contextRef;

        DumpDbTask(Context context) {
            contextRef = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "DangerousPermissionRequests:");
            List<DangerousPermissionRequest> dangerousPermissionRequests = DatabaseClient.getInstance(contextRef.get()).getAppDatabase()
                    .dangerousPermissionRequestDao().getAll();
            for (DangerousPermissionRequest req : dangerousPermissionRequests) {
                Log.d(TAG, req.toString());
            }
            Log.d(TAG, "PrivateDataRequests");
            List<PrivateDataRequest> privateDataRequests = DatabaseClient.getInstance(contextRef.get()).getAppDatabase()
                    .privateDataRequestDao().getAll();
            for (PrivateDataRequest req : privateDataRequests) {
                Log.d(TAG, req.toString());
            }

            AbstractStackCache cache = AbstractStackCache.getCurrentCache();
            if(cache != null) {
                Log.d(TAG, cache.toString());
            }


            return null;
        }
    }

    private static class LoadPoliciesTask extends AsyncTask<Void, Void, List<PolicySetting>> {
        private WeakReference<Activity> activityRef;

        LoadPoliciesTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<PolicySetting> doInBackground(Void... voids) {
           return PolicyUtils.getAllPolicies(activityRef.get());
        }

        @Override
        protected void onPostExecute(List<PolicySetting> policies) {
            LinearLayout policiesLayout = activityRef.get().findViewById(R.id.policies);
            for (PolicySetting policy : policies) {
                final int id = policy.id;
                PolicyView view = new PolicyView(activityRef.get());
                view.setPolicyFields(id, policy.packageName, policy.permission, policy.policyDecision, false);
                view.setButtonListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new RemovePolicyTask(activityRef.get(), false).execute(id);
                    }
                });
                policiesLayout.addView(view);
            }
        }
    }

    private static class LoadPalPoliciesTask extends AsyncTask<Void, Void, List<PalPolicySetting>> {
        private WeakReference<Activity> activityRef;

        LoadPalPoliciesTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<PalPolicySetting> doInBackground(Void... voids) {
            return PolicyUtils.getAllPalPolicies(activityRef.get());
        }

        @Override
        protected void onPostExecute(List<PalPolicySetting> policies) {
            LinearLayout policiesLayout = activityRef.get().findViewById(R.id.policies);
            for (PalPolicySetting policy : policies) {
                final int id = policy.id;
                PolicyView view = new PolicyView(activityRef.get());
                view.setPolicyFields(id, policy.packageName, policy.pal, policy.policyDecision, true);
                view.setButtonListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new RemovePolicyTask(activityRef.get(), true).execute(id);
                    }
                });
                policiesLayout.addView(view);
            }
        }
    }

    private static class LoadPoliciesForPackageTask extends AsyncTask<Void, Void, List<PolicySetting>> {
        private WeakReference<Activity> activityRef;
        private String packageName;

        LoadPoliciesForPackageTask(Activity activity, String packageName) {
            activityRef = new WeakReference<>(activity);
            this.packageName = packageName;
        }

        @Override
        protected List<PolicySetting> doInBackground(Void... voids) {
            return PolicyUtils.getAllPolicies(activityRef.get());
        }

        @Override
        protected void onPostExecute(List<PolicySetting> policies) {
            LinearLayout policiesLayout = activityRef.get().findViewById(R.id.policies);
            policiesLayout.removeAllViews();
            for (PolicySetting policy : policies) {
                if(policy.packageName.equals(packageName)){
                    final int id = policy.id;
                    PolicyView view = new PolicyView(activityRef.get());
                    view.setPolicyFields(id, policy.packageName, policy.permission, policy.policyDecision, false);
                    view.setButtonListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new RemovePolicyTask(activityRef.get(), false).execute(id);
                        }
                    });
                    policiesLayout.addView(view);
                }else{
                    continue;
                }
            }
        }
    }

    private static class AddPolicyTask extends AsyncTask<String, Void, Integer> {
        private WeakReference<Activity> activityRef;
        private String packageName;
        private String permission;
        private String decision;

        AddPolicyTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Integer doInBackground(String... policy) {
            packageName = policy[0];
            permission = policy[1];
            decision = policy[2];
            return PolicyUtils.setPolicy(activityRef.get(), packageName, permission, decision);
        }

        @Override
        protected void onPostExecute(final Integer id) {
            LinearLayout policiesLayout = activityRef.get().findViewById(R.id.policies);
            boolean shouldAddView = true;
            for (int i = 0; i < policiesLayout.getChildCount(); i++) {
                View v = policiesLayout.getChildAt(i);
                if (v instanceof PolicyView) {
                    PolicyView policy = (PolicyView) v;
                    if (policy.getPolicyId() == id && !policy.isPalPolicy()) {
                        if (!policy.getPolicyDecision().equals(decision)) {
                            // Delete this, create updated one
                            policiesLayout.removeViewAt(i);
                        } else {
                            // Duplicate, do nothing
                            shouldAddView = false;
                        }
                        break;
                    }
                }
            }
            if (shouldAddView) {
                PolicyView view = new PolicyView(activityRef.get());
                view.setPolicyFields(id, packageName, permission, decision, false);
                view.setButtonListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new RemovePolicyTask(activityRef.get(), false).execute(id);
                    }
                });
                policiesLayout.addView(view);
            }
        }
    }

    private static class AddPalPolicyTask extends AsyncTask<String, Void, Integer> {
        private WeakReference<Activity> activityRef;
        private String packageName;
        private String pal;
        private String permission;
        private String purpose;
        private String decision;

        AddPalPolicyTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Integer doInBackground(String... policy) {
            packageName = policy[0];
            pal = policy[1];
            permission = policy[2];
            purpose = policy[3];
            decision = policy[4];
            return PolicyUtils.setPalPolicy(activityRef.get(), packageName, permission, purpose, pal, decision);
        }

        @Override
        protected void onPostExecute(final Integer id) {
            LinearLayout policiesLayout = activityRef.get().findViewById(R.id.policies);
            boolean shouldAddView = true;
            for (int i = 0; i < policiesLayout.getChildCount(); i++) {
                View v = policiesLayout.getChildAt(i);
                if (v instanceof PolicyView) {
                    PolicyView policy = (PolicyView) v;
                    if (policy.getPolicyId() == id && policy.isPalPolicy()) {
                        if (!policy.getPolicyDecision().equals(decision)) {
                            // Delete this, create updated one
                            policiesLayout.removeViewAt(i);
                        } else {
                            // Duplicate, do nothing
                            shouldAddView = false;
                        }
                        break;
                    }
                }
            }
            if (shouldAddView) {
                PolicyView view = new PolicyView(activityRef.get());
                view.setPolicyFields(id, packageName, pal, decision, true);
                view.setButtonListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new RemovePolicyTask(activityRef.get(), true).execute(id);
                    }
                });
                policiesLayout.addView(view);
            }
        }
    }

    private static class AddPalPolicyDialogTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<AppCompatActivity> activityRef;

        // Map package --> pal name --> permission --> purpose
        private Map<String, Map<String, Map<String, Set<String>>>> uniquePalRequests;

        AddPalPolicyDialogTask(AppCompatActivity activity) {
            activityRef = new WeakReference<>(activity);
            uniquePalRequests = new HashMap<>();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<PrivateDataRequest> palRequests = DatabaseClient.getInstance(activityRef.get())
                    .getAppDatabase().privateDataRequestDao().getAll();

            for (PrivateDataRequest r : palRequests) {
                Map<String, Map<String, Set<String>>> pals = uniquePalRequests.get(r.packageName);
                if (pals == null) {
                    pals = new HashMap<String, Map<String, Set<String>>>();
                    uniquePalRequests.put(r.packageName, pals);
                }

                Map<String, Set<String>> permissions = pals.get(r.pal);
                if (permissions == null) {
                    permissions = new HashMap<String, Set<String>>();
                    pals.put(r.pal, permissions);
                }

                Set<String> purposes = permissions.get(r.permission);
                if (purposes == null) {
                    purposes = new HashSet<String>();
                    permissions.put(r.permission, purposes);
                }

                purposes.add(r.purpose);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // show add package UI
            AddPalPolicyDialogFragment addDialog = new AddPalPolicyDialogFragment(uniquePalRequests);
            addDialog.show(activityRef.get().getSupportFragmentManager(), "addDialog");
        }
    }


    private static class RemovePolicyTask extends AsyncTask<Integer, Void, Integer> {
        private WeakReference<Activity> activityRef;
        private boolean isPalPolicy;

        RemovePolicyTask(Activity activity, boolean isPalPolicy) {
            activityRef = new WeakReference<>(activity);
            this.isPalPolicy = isPalPolicy;
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            int id = integers[0];
            if (isPalPolicy) {
                PolicyUtils.deletePalPolicy(activityRef.get(), id);
            } else {
                PolicyUtils.deletePolicy(activityRef.get(), id);
            }
            return id;
        }

        @Override
        protected void onPostExecute(Integer id) {
            LinearLayout policiesLayout = activityRef.get().findViewById(R.id.policies);
            for (int i = 0; i < policiesLayout.getChildCount(); i++) {
                View v = policiesLayout.getChildAt(i);
                if (v instanceof PolicyView) {
                    PolicyView policy = (PolicyView) v;
                    if (policy.getPolicyId() == id && policy.isPalPolicy() == isPalPolicy) {
                        policiesLayout.removeViewAt(i);
                        break;
                    }
                }
            }
        }
    }

    private static class LoadTestPoliciesTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Activity> activityRef;

        LoadTestPoliciesTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(activityRef.get().getAssets().open("test-policies.csv")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] policy = line.split(",");
                    PolicyUtils.setPolicy(activityRef.get(), policy[0], policy[1], policy[2]);
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(activityRef.get().getAssets().open("test-purpose-policies.csv")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] policy = line.split(",");
                    PolicyUtils.setPurposePolicy(activityRef.get(), policy[0], policy[1], policy[2], policy[3]);
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(activityRef.get().getAssets().open("test-pal-policies.csv")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] policy = line.split(",");
                    PolicyUtils.setPalPolicy(activityRef.get(), policy[0], policy[1], policy[2], policy[3], policy[4]);
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new LoadPoliciesTask(activityRef.get()).execute(); // update the UI
        }
    }
}
