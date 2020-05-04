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

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.twosixlabs.privacycheckup.db.policy.PolicyUtils;

public class AddPolicyDialogFragment extends DialogFragment {

    private static final String[] DANGEROUS_PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.ADD_VOICEMAIL,
            Manifest.permission.USE_SIP,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public interface AddPolicyDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, String packageName, String permission, String decision);
    }

    private AddPolicyDialogListener listener;
    private ArrayAdapter<MyPackageInfo> packagesSpinnerAdapter;
    private ArrayAdapter<String> permissionsSpinnerAdapter;
    private RadioGroup radioGroup;
    private RadioButton radioButtonAllow;
    private RadioButton radioButtonDeny;
    private RadioButton radioButtonDenyBg;

    public String mPreselectPackageName;
    public String mPreselectPermissionName;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (AddPolicyDialogListener) context;

        // Set up packages spinner
        final PackageManager PM = context.getPackageManager();
        List<PackageInfo> packageInfos = PM.getInstalledPackages(0);
        List<MyPackageInfo> appNamesAndPackages = new LinkedList<>();
        for(PackageInfo pi : packageInfos) {
            String name = PM.getApplicationLabel(pi.applicationInfo).toString();
            MyPackageInfo mpi = new MyPackageInfo(pi.packageName, name);
            appNamesAndPackages.add(mpi);
        }

        appNamesAndPackages.sort(new Comparator<MyPackageInfo>() {
            @Override
            public int compare(MyPackageInfo o1, MyPackageInfo o2) {
                return o1.label.compareToIgnoreCase(o2.label);
            }
        });

        MyPackageInfo topDummyPackage = new MyPackageInfo(null, "Select package...");
        appNamesAndPackages.add(0, topDummyPackage);

        packagesSpinnerAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                appNamesAndPackages
        );

        // Set up permissions spinner
        List<String> simplePermissionNames = new ArrayList<>();
        simplePermissionNames.add("Select permission...");
        for (String fullPermissionName : DANGEROUS_PERMISSIONS) {
            String simplePermissionName = fullPermissionName.substring(fullPermissionName.indexOf("permission.") + 11);
            simplePermissionNames.add(simplePermissionName);
        }
        permissionsSpinnerAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                simplePermissionNames
        );

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null){
            mPreselectPackageName = getArguments().getString("PACKAGE_NAME");
            mPreselectPermissionName = getArguments().getString("PERMISSION_NAME");
        }

        //AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        MaterialAlertDialogBuilder buidler = new MaterialAlertDialogBuilder(getContext(), R.style.ThemeOverlay_MaterialComponents_Dialog);

        // Create View that holds spinner
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View contentView = inflater.inflate(R.layout.dialog_add_policy, null);

        radioGroup = (RadioGroup) contentView.findViewById(R.id.radioButtonGroup);
        radioButtonAllow = (RadioButton) contentView.findViewById(R.id.radioButtonAllow);
        radioButtonDeny = (RadioButton) contentView.findViewById(R.id.radioButtonDeny);
        radioButtonDenyBg = (RadioButton) contentView.findViewById(R.id.radioButtonDenyBackground);

        final Spinner packagesSpinner = contentView.findViewById(R.id.packages_spinner);
        final Spinner permissionsSpinner = contentView.findViewById(R.id.permissions_spinner);

        // Create spinner with package names
        packagesSpinner.setAdapter(packagesSpinnerAdapter);
        permissionsSpinner.setAdapter(permissionsSpinnerAdapter);

        // Pre-select spinner value if it exists
        if (mPreselectPermissionName != null && mPreselectPackageName != null) {
            MyPackageInfo mpi = new MyPackageInfo(mPreselectPackageName, null);
            int spinnerPosPackage = packagesSpinnerAdapter.getPosition(mpi);
            packagesSpinner.setSelection(spinnerPosPackage);

            // Get last piece of full permission name to select from spinner
            String[] permissionString = mPreselectPermissionName.split(Pattern.quote("."));
            int spinnerPosPermission = permissionsSpinnerAdapter.getPosition(permissionString[permissionString.length-1]);
            permissionsSpinner.setSelection(spinnerPosPermission);
        }

        // Create dialog
        buidler.setView(contentView)
                .setTitle(R.string.dialog_add_policy)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (packagesSpinner.getSelectedItemPosition() > 0 &&
                                permissionsSpinner.getSelectedItemPosition() > 0) {
                            String selectedPackage = ((MyPackageInfo) packagesSpinner.getSelectedItem()).packageName;
                            String selectedPermission = DANGEROUS_PERMISSIONS[permissionsSpinner.getSelectedItemPosition() - 1];
                            String selectedDecision;
                            if (radioGroup.getCheckedRadioButtonId() == radioButtonAllow.getId()) {
                                selectedDecision = PolicyUtils.POLICY_ALLOW;
                            } else if (radioGroup.getCheckedRadioButtonId() == radioButtonDeny.getId()) {
                                selectedDecision = PolicyUtils.POLICY_DENY;
                            } else {
                                selectedDecision = PolicyUtils.POLICY_DENY_BG;
                            }
                            listener.onDialogPositiveClick(AddPolicyDialogFragment.this, selectedPackage, selectedPermission, selectedDecision);
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //canceled
            }
        });
        return buidler.create();

    }

    private class MyPackageInfo{
        public String packageName;
        public String label;

        public MyPackageInfo(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj != null && obj instanceof MyPackageInfo) {
                MyPackageInfo info = (MyPackageInfo) obj;

                if(info.packageName != null) {
                    return info.packageName.equals(this.packageName);
                }
            }

            return false;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
