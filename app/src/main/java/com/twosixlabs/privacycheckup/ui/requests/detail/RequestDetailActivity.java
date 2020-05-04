/*
 * Copyright © 2020 by Raytheon BBN Technologies Corp.
 *
 * This material is based upon work supported by DARPA and AFRL under Contract No. FA8750-16-C-0006.
 *
 * The Government has unlimited rights to use, modify, reproduce, release, perform, display, or
 * disclose computer software or computer software documentation marked with this legend.
 * Any reproduction of technical data, computer software, or portions thereof marked with
 * this legend must also reproduce this marking.
 *
 * DISCLAIMER LANGUAGE
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.twosixlabs.privacycheckup.ui.requests.detail;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.twosixlabs.privacycheckup.R;
import com.twosixlabs.privacycheckup.db.DatabaseClient;
import com.twosixlabs.privacycheckup.db.requests.DangerousPermissionRequest;
import com.twosixlabs.privacycheckup.db.requests.PrivateDataRequest;
import com.twosixlabs.privacycheckup.ui.applications.AppData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class RequestDetailActivity extends AppCompatActivity {

    private static final String TAG = RequestDetailActivity.class.getName();

    List<AppData> mAppsList = new ArrayList<>();
    RequestDetailListAdapter mAdapter = new RequestDetailListAdapter(mAppsList, this);
    String mFullPermissionName = "";
    String mPalName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Handle intent when created
        Intent intent = getIntent();

        String permissionLabel = "";
        String permissionDesc = "";
        Drawable permissionLogoDrawable = null;

        if (intent.hasExtra("EXTRA_PERMISSION_NAME")) {
            mFullPermissionName = intent.getStringExtra("EXTRA_PERMISSION_NAME");
            getSupportActionBar().setTitle("Applications Using Permission");

            //get the permission details for this permission name
            PackageManager pm = getPackageManager();
            PermissionInfo pi = null;
            try {
                pi = pm.getPermissionInfo(mFullPermissionName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            PermissionGroupInfo groupInfo = null;
            try {
                groupInfo = pm.getPermissionGroupInfo(pi.group, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            permissionLabel = (String) pi.loadLabel(pm);
            permissionDesc = (String) pi.loadDescription(pm);
            permissionLogoDrawable = null;

            try {
                if(groupInfo != null){
                    permissionLogoDrawable =  pm.getResourcesForApplication("android").getDrawable(groupInfo.icon);
                }else{
                    permissionLogoDrawable = ContextCompat.getDrawable(this, R.drawable.ic_security_black_24dp);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            // Do something else
        }

        if (intent.hasExtra("EXTRA_PAL_NAME")) {
            mPalName = intent.getStringExtra("EXTRA_PAL_NAME");
            getSupportActionBar().setTitle("Applications Using μPAL");
            permissionLabel = (String) mPalName;
            permissionDesc = "";
            permissionLogoDrawable = ContextCompat.getDrawable(this, R.drawable.ic_security_black_24dp);
        } else {
            // Do something else
        }

        ImageView imageIcon = findViewById(R.id.imageViewPermissionIcon);
        TextView tvTitle = findViewById(R.id.textViewPermissionTitle);
        TextView tvDesc = findViewById(R.id.textViewPermissionDesc);

        imageIcon.setImageDrawable(permissionLogoDrawable);
        tvTitle.setText(permissionLabel);
        tvDesc.setText(permissionDesc);

        //set up recycler` view
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view_apps_detail);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        //set up sort
        /**
         * Button setup
         */

        Button buttonSort = findViewById(R.id.buttonSelectSort);
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort by...");

        // add a list
        final String[] sortOptions = {
                "Count (Ascending)",
                "Count (Descending)",
                "Name (A to Z)",
                "Name (Z to A)"
        };

        builder.setItems(sortOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Sorting by " + sortOptions[which]);
                switch (which) {
                    case 0: // SIZE - asc
                        Collections.sort(mAppsList, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2){
                                return app1.getRequestCount() > app2.getRequestCount() ? -1 : (app1.getRequestCount() < app2.getRequestCount()) ? 1 : 0;
                            }
                        });
                        break;
                    case 1: // SIZE - desc
                        Collections.sort(mAppsList, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2){
                                return app1.getRequestCount() < app2.getRequestCount() ? -1 : (app1.getRequestCount() > app2.getRequestCount()) ? 1 : 0;
                            }
                        });
                        break;
                    case 2: // NAME - asc
                        Collections.sort(mAppsList, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2) {
                                return app1.getDisplayName().compareToIgnoreCase(app2.getDisplayName());
                            }
                        });
                        break;
                    case 3: // NAME - desc
                        Collections.sort(mAppsList, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2) {
                                return -app1.getDisplayName().compareToIgnoreCase(app2.getDisplayName());
                            }
                        });
                        break;
                }

                mAdapter.notifyDataSetChanged();
            }
        });

        // create and show the alert dialog
        final AlertDialog dialog = builder.create();
        buttonSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });

        if(!mPalName.equals("")){ //this is a micropal
            new LoadPrivateDataRequestsTask(this).execute();
        } else if(!mFullPermissionName.equals("")){ //this is a permission
            new LoadPermissionRequestsTask(this).execute();
        }
    }

    private class LoadPermissionRequestsTask extends AsyncTask<Void, Void, List<DangerousPermissionRequest>> {
        private WeakReference<Activity> activityRef;

        LoadPermissionRequestsTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<DangerousPermissionRequest> doInBackground(Void... voids) {
            List<DangerousPermissionRequest> privateDataRequests = DatabaseClient.getInstance(activityRef.get()).getAppDatabase()
                    .dangerousPermissionRequestDao().getAll();

            return privateDataRequests;
        }

        @Override
        protected void onPostExecute(List<DangerousPermissionRequest> reqs) {
            Log.d(TAG, "Got back reqs of size " + reqs.size());

            //Get all installed packages
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            //Get all unique entries, populate a list of AppData items
            Set<String> uniqueApplications = new HashSet();

            //get all unique apps...
            for(DangerousPermissionRequest req : reqs){
                String app = req.packageName;
                uniqueApplications.add(app);
            }

            //for each unique request type, populate count + details
            for(final String applicationName : uniqueApplications){
                //full name...
                final String fullPermissionName = mFullPermissionName;
                final String fullPackageName = applicationName;
                Drawable icon = ContextCompat.getDrawable(RequestDetailActivity.this, android.R.drawable.ic_dialog_info);
                String displayName = fullPackageName;

                //using anonymous inner to get request count...
                int numberOfRequests = (int) reqs.stream().filter(new Predicate<DangerousPermissionRequest>() {
                    @Override
                    public boolean test(DangerousPermissionRequest p) {
                        return p.permission.equals(fullPermissionName) && p.packageName.equals(fullPackageName);
                    }
                }).count();

                //foreground....
                int numberOfFgRequests = (int) reqs.stream().filter(new Predicate<DangerousPermissionRequest>() {
                    @Override
                    public boolean test(DangerousPermissionRequest p) {
                        return p.permission.equals(fullPermissionName) && p.packageName.equals(fullPackageName) && (!p.isBackground);
                    }
                }).count();

                //background...
                int numberOfBgRequests = (int) reqs.stream().filter(new Predicate<DangerousPermissionRequest>() {
                    @Override
                    public boolean test(DangerousPermissionRequest p) {
                        return p.permission.equals(fullPermissionName) && p.packageName.equals(fullPackageName) && (p.isBackground);
                    }
                }).count();

                for(ApplicationInfo appInfo : packages){
                    if(appInfo.packageName.equals(fullPackageName)){
                        displayName = appInfo.loadLabel(pm).toString(); //...grab the name!f
                        icon = appInfo.loadIcon(pm); //..and the icon!
                        break;
                    }else{
                        Log.d(TAG, "Couldn't find the application " + fullPackageName + " in the set of installed applications");
                    }
                }

                if(numberOfRequests > 0){
                    mAppsList.add(new AppData(
                            displayName, fullPackageName, icon, numberOfRequests, false
                    ));
                }else{
                    Log.d(TAG, "Application " + fullPackageName + " did not use this permission. Ignoring.");
                }

            }

            mAdapter.notifyDataSetChanged();
        }
    }

    private class LoadPrivateDataRequestsTask extends AsyncTask<Void, Void, List<PrivateDataRequest>> {
        private WeakReference<Activity> activityRef;

        LoadPrivateDataRequestsTask(Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        @Override
        protected List<PrivateDataRequest> doInBackground(Void... voids) {
            List<PrivateDataRequest> privateDataRequests = DatabaseClient.getInstance(activityRef.get()).getAppDatabase()
                    .privateDataRequestDao().getAll();

            return privateDataRequests;
        }

        @Override
        protected void onPostExecute(List<PrivateDataRequest> reqs) {
            Log.d(TAG, "Got back reqs of size " + reqs.size());

            //Get all installed packages
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            //Get all unique entries, populate a list of AppData items
            Set<String> uniqueApplications = new HashSet();

            //get all unique apps...
            for(PrivateDataRequest req : reqs){
                String app = req.packageName;
                uniqueApplications.add(app);
            }

            //for each unique request type, populate count + details
            for(final String applicationName : uniqueApplications){
                //full name...
                final String fullPermissionName = mPalName;
                final String fullPackageName = applicationName;
                Drawable icon = ContextCompat.getDrawable(RequestDetailActivity.this, android.R.drawable.ic_dialog_info);
                String displayName = fullPackageName;

                //using anonymous inner to get request count...
                int numberOfRequests = (int) reqs.stream().filter(new Predicate<PrivateDataRequest>() {
                    @Override
                    public boolean test(PrivateDataRequest p) {
                        return p.pal.equals(fullPermissionName) && p.packageName.equals(fullPackageName);
                    }
                }).count();

                for(ApplicationInfo appInfo : packages){
                    Log.d(TAG, "Comparing " + fullPackageName + " (Given) with " + appInfo.packageName);
                    if(appInfo.packageName.equals(fullPackageName)){
                        displayName = appInfo.loadLabel(pm).toString(); //...grab the name!
                        icon = appInfo.loadIcon(pm); //..and the icon!
                        break;
                    }else{
                        Log.d(TAG, "Couldn't find the application " + fullPackageName + " in the set of installed applications");
                    }
                }

                if(numberOfRequests > 0){
                    mAppsList.add(new AppData(
                            displayName, fullPackageName, icon, numberOfRequests, false
                    ));
                }else{
                    Log.d(TAG, "Application " + fullPackageName + " did not use this permission. Ignoring.");
                }

            }

            mAdapter.notifyDataSetChanged();
        }
    }
}
