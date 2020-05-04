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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.twosixlabs.privacycheckup.db.policy.PolicyUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddPalPolicyDialogFragment extends DialogFragment {

    public interface AddPalPolicyDialogListener {
        void onPalDialogPositiveClick(DialogFragment dialog, String packageName, String pal, String permission, String purpose, String decision);
    }

    private AddPalPolicyDialogListener listener;

    private ArrayAdapter<SpinnerItem> packagesSpinnerAdapter;
    private List<SpinnerItem> packagesList;

    private ArrayAdapter<SpinnerItem> palsSpinnerAdapter;
    private List<SpinnerItem> palsList;

    private ArrayAdapter<SpinnerItem> permissionsSpinnerAdapter;
    private List<SpinnerItem> permissionsList;

    private ArrayAdapter<SpinnerItem> purposesSpinnerAdapter;
    private List<SpinnerItem> purposesList;

    private String selectedPackage;
    private String selectedPal;
    private String selectedPermission;
    private String selectedPurpose;

    private RadioGroup radioGroup;
    private RadioButton radioButtonAllow;
    private RadioButton radioButtonDeny;

    private Map<String, Map<String, Map<String, Set<String>>>> uniquePalRequests;

    public AddPalPolicyDialogFragment(Map<String, Map<String, Map<String, Set<String>>>> uniquePalRequests) {
        this.uniquePalRequests = uniquePalRequests;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (AddPalPolicyDialogListener) context;

        // Set up packages spinner
        List<String> packages = new ArrayList<>(uniquePalRequests.keySet());
        packagesList = new LinkedList<>();
        final PackageManager PM = context.getPackageManager();
        for (String packageName : packages) {
            String name = packageName;
            try {
                name = PM.getApplicationLabel(PM.getApplicationInfo(packageName, 0)).toString();
            } catch (PackageManager.NameNotFoundException e) {
            }
            packagesList.add(new SpinnerItem(packageName, name));
        }

        packagesList.sort(new Comparator<SpinnerItem>() {
            @Override
            public int compare(SpinnerItem o1, SpinnerItem o2) {
                return o1.displayName.compareToIgnoreCase(o2.displayName);
            }
        });

        SpinnerItem topDummyPackage = new SpinnerItem(null, "Select package...");
        packagesList.add(0, topDummyPackage);

        packagesSpinnerAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                packagesList
        );

        // Set up pals spinner
        palsList = new ArrayList<>();
        palsSpinnerAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                palsList
        );

        // Set up permissions spinner
        permissionsList = new ArrayList<>();
        permissionsSpinnerAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                permissionsList
        );

        // Set up purposes spinner
        purposesList = new ArrayList<>();
        purposesSpinnerAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                purposesList
        );

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        MaterialAlertDialogBuilder buidler = new MaterialAlertDialogBuilder(getContext(), R.style.ThemeOverlay_MaterialComponents_Dialog);

        // Create View that holds spinner
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View contentView = inflater.inflate(R.layout.dialog_add_pal_policy, null);

        radioGroup = (RadioGroup) contentView.findViewById(R.id.radioButtonGroup);
        radioButtonAllow = (RadioButton) contentView.findViewById(R.id.radioButtonAllow);
        radioButtonDeny = (RadioButton) contentView.findViewById(R.id.radioButtonDeny);

        final Spinner packagesSpinner = contentView.findViewById(R.id.packages_spinner);
        final Spinner palsSpinner = contentView.findViewById(R.id.pals_spinner);
        final Spinner permissionsSpinner = contentView.findViewById(R.id.permissions_spinner);
        final Spinner purposesSpinner = contentView.findViewById(R.id.purposes_spinner);

        // Create spinner with package names
        packagesSpinner.setAdapter(packagesSpinnerAdapter);
        palsSpinner.setAdapter(palsSpinnerAdapter);
        permissionsSpinner.setAdapter(permissionsSpinnerAdapter);
        purposesSpinner.setAdapter(purposesSpinnerAdapter);

        packagesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newPackage = packagesList.get(position).name;
                if (position == 0 || !newPackage.equals(selectedPackage)) {
                    selectedPackage = null;
                    selectedPal = null;
                    palsList.clear();
                    palsSpinnerAdapter.notifyDataSetChanged();
                    palsSpinner.setVisibility(View.INVISIBLE);
                    selectedPermission = null;
                    permissionsList.clear();
                    permissionsSpinnerAdapter.notifyDataSetChanged();
                    permissionsSpinner.setVisibility(View.INVISIBLE);
                    selectedPurpose = null;
                    purposesList.clear();
                    purposesSpinnerAdapter.notifyDataSetChanged();
                    purposesSpinner.setVisibility(View.INVISIBLE);
                }
                selectedPackage = newPackage;
                if (position != 0) {
                    palsList.clear();
                    palsList.add(0, new SpinnerItem(null, "Select pal..."));
                    List<String> palsForPackage = new ArrayList<>(uniquePalRequests.get(selectedPackage).keySet());
                    for (String pal : palsForPackage) {
                        palsList.add(new SpinnerItem(pal, pal));
                    }
                    palsSpinnerAdapter.notifyDataSetChanged();
                    palsSpinner.setVisibility(View.VISIBLE);
                }

            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        palsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newPal = palsList.get(position).name;
                if (position == 0 || !newPal.equals(selectedPal)) {
                    selectedPal = null;
                    selectedPermission = null;
                    permissionsList.clear();
                    permissionsSpinnerAdapter.notifyDataSetChanged();
                    permissionsSpinner.setVisibility(View.INVISIBLE);
                    selectedPurpose = null;
                    purposesList.clear();
                    purposesSpinnerAdapter.notifyDataSetChanged();
                    purposesSpinner.setVisibility(View.INVISIBLE);
                }
                selectedPal = newPal;
                if (position != 0) {
                    permissionsList.clear();
                    permissionsList.add(0, new SpinnerItem(null, "Select permission..."));
                    List<String> permissionsForPal = new ArrayList<>(uniquePalRequests.get(selectedPackage).get(selectedPal).keySet());
                    for (String permission : permissionsForPal) {
                        permissionsList.add(new SpinnerItem(permission, permission.substring(permission.indexOf("permission.") + 11)));
                    }
                    permissionsSpinnerAdapter.notifyDataSetChanged();
                    permissionsSpinner.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        permissionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newPermission = permissionsList.get(position).name;
                if (position == 0 || !newPermission.equals(selectedPermission)) {
                    selectedPermission = null;
                    selectedPurpose = null;
                    purposesList.clear();
                    purposesSpinnerAdapter.notifyDataSetChanged();
                    purposesSpinner.setVisibility(View.INVISIBLE);
                }
                selectedPermission = newPermission;
                if (position != 0) {
                    purposesList.clear();
                    purposesList.add(0, new SpinnerItem(null, "Select purpose..."));
                    List<String> purposesForPermission = new ArrayList<>(uniquePalRequests.get(selectedPackage).get(selectedPal).get(selectedPermission));
                    for (String purpose : purposesForPermission) {
                        purposesList.add(new SpinnerItem(purpose, purpose));
                    }
                    purposesSpinnerAdapter.notifyDataSetChanged();
                    purposesSpinner.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });



        // Create dialog
        buidler.setView(contentView)
                .setTitle(R.string.dialog_add_pal_policy)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selectedPackage != null &&
                                selectedPal != null &&
                                selectedPermission != null &&
                                purposesSpinner.getSelectedItemPosition() > 0) {
                            selectedPurpose = purposesList.get(purposesSpinner.getSelectedItemPosition()).name;
                            String selectedDecision;
                            if (radioGroup.getCheckedRadioButtonId() == radioButtonAllow.getId()) {
                                selectedDecision = PolicyUtils.POLICY_ALLOW;
                            } else {
                                selectedDecision = PolicyUtils.POLICY_DENY;
                            }
                            listener.onPalDialogPositiveClick(AddPalPolicyDialogFragment.this, selectedPackage, selectedPal, selectedPermission, selectedPurpose, selectedDecision);
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

    private class SpinnerItem{
        public String name;
        public String displayName;

        public SpinnerItem(String packageName, String label) {
            this.name = packageName;
            this.displayName = label;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj != null && obj instanceof SpinnerItem) {
                SpinnerItem info = (SpinnerItem) obj;

                if(info.name != null) {
                    return info.name.equals(this.name);
                }
            }

            return false;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

}
