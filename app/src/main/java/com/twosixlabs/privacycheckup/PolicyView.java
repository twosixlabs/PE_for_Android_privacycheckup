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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.twosixlabs.privacycheckup.db.policy.PolicyUtils;

public class PolicyView extends LinearLayout {

    private TextView mIsPalLabelView;
    private TextView mPackageNameView;
    private TextView mPermissionNameView;
    private TextView mPolicyDecisionView;
    private String mPolicyDecision;
    private ImageButton mDeleteButton;
    private int mId;
    private boolean mIsPalPolicy = false;

    public PolicyView(Context context) {
        this(context, null);
    }

    public PolicyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.policy_view, this);
        initView(view);
    }

    public void setId(int id) {
        mId = id;
    }
    public int getPolicyId() {
        return mId;
    }

    public String getPolicyDecision() {
        return mPolicyDecision;
    }

    public boolean isPalPolicy() {
        return mIsPalPolicy;
    }

    public void setPolicyFields(int id, String packageName, String permission, String decision, boolean isPalPolicy) {
        mId = id;
        mPackageNameView.setText(packageName);
        String shortPermission = permission.substring(permission.lastIndexOf(".") + 1);
        mPermissionNameView.setText(shortPermission);
        mPolicyDecision = decision;
        String decisionText;
        switch (mPolicyDecision) {
            case PolicyUtils.POLICY_ALLOW:
                decisionText = "ALLOW";
                break;
            case PolicyUtils.POLICY_DENY:
                decisionText = "DENY";
                break;
            case PolicyUtils.POLICY_DENY_BG:
                decisionText = "DENY_BG";
                break;
            default:
                decisionText = "?";
        }
        mPolicyDecisionView.setText(decisionText);
        mIsPalPolicy = isPalPolicy;
        if (mIsPalPolicy) {
            mIsPalLabelView.setVisibility(VISIBLE);
        } else {
            mIsPalLabelView.setVisibility(GONE);
        }
    }

    public void setButtonListener(OnClickListener listener) {
        mDeleteButton.setOnClickListener(listener);
    }

    private void initView(View view) {
        mIsPalLabelView = view.findViewById(R.id.is_pal_label);
        mPackageNameView = view.findViewById(R.id.package_name);
        mPermissionNameView = view.findViewById(R.id.permission);
        mPolicyDecisionView = view.findViewById(R.id.policy);
        mDeleteButton = view.findViewById(R.id.delete_button);
    }
}
