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

package com.twosixlabs.privacycheckup.ui.applications.detail;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twosixlabs.privacycheckup.PolicyActivity;
import com.twosixlabs.privacycheckup.R;
import com.twosixlabs.privacycheckup.ui.requests.PermissionRequestData;
import com.twosixlabs.privacycheckup.ui.requests.detail.RequestDetailActivity;

import java.text.DecimalFormat;
import java.util.List;

public class ApplicationDetailListAdapter extends RecyclerView.Adapter<ApplicationDetailListAdapter.ViewHolder> {
    private List<PermissionRequestData> listdata;
    public Context mContext;
    public String mPackageName;
    int totalRequestsForThisApp = 0;

    private static final String TAG = ApplicationDetailListAdapter.class.getName();


    public ApplicationDetailListAdapter(List<PermissionRequestData> listdata, Context context, String packageName) {
        this.listdata = listdata;
        this.mContext = context;

        this.mPackageName = packageName;


    }

    @Override
    public ApplicationDetailListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.layout_application_detail_row, parent, false);
        ApplicationDetailListAdapter.ViewHolder viewHolder = new ApplicationDetailListAdapter.ViewHolder(listItem);


        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ApplicationDetailListAdapter.ViewHolder holder, final int position) {
        final PermissionRequestData myListData = listdata.get(position);


        //TODO: Do this somewhere else, doesn't make sense to do this for every item
        this.totalRequestsForThisApp = 0;
        for (PermissionRequestData item: listdata) {
            Log.d(TAG, "Counting all data, currently we have " + this.totalRequestsForThisApp);
            this.totalRequestsForThisApp = this.totalRequestsForThisApp + item.getRequestCount();
        }

        int requestCount = listdata.get(position).getRequestCount();
        int requestCountBg = listdata.get(position).getBackgroundRequestCount();
        int requestCountFg = listdata.get(position).getForegroundRequestCount();

        holder.textView.setText(listdata.get(position).getPermissionLabel());
        holder.textViewRequestsCount.setText(requestCount + " requests to use this permission");
        holder.textViewDescription.setText(listdata.get(position).getPermissionDescription());

        holder.imageViewRequestIcon.setImageDrawable(myListData.getIcon());

        DecimalFormat value = new DecimalFormat("#.##");

        Log.d(TAG, "Got FOREGROUND req count is " + requestCountFg);
        holder.textViewCountComparedFg.setText(requestCountFg + " foreground / " + totalRequestsForThisApp + " total");
        double progressPercentage = (requestCountFg * 100.0 / totalRequestsForThisApp);
        Log.d(TAG, "Percent is " + progressPercentage);
        holder.progressBar.setProgress((int) progressPercentage, true);
        holder.textViewPercentageFg.setText(value.format(progressPercentage) + "%");

        Log.d(TAG, "Got BACKGROUND req count is " + requestCountBg);
        holder.textViewCountComparedBg.setText(requestCountBg + " background / " + totalRequestsForThisApp + " total");
        double progress2 = (requestCountBg * 100.0 / totalRequestsForThisApp);
        Log.d(TAG, "Percent is " + progress2);
        holder.progressBar2.setProgress((int) progress2, true);
        holder.textViewPercentageBg.setText(value.format(progress2) + "%");


        holder.textViewButtonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                //creating a popup menu
                PopupMenu popup = new PopupMenu(mContext, holder.textViewButtonMenu);
                //inflating menu from xml resource
                popup.inflate(R.menu.permission_detail_menu);
                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.item1:
                                //handle menu1 click
                                Intent intent = new Intent(view.getContext(), RequestDetailActivity.class);
                                intent.putExtra("EXTRA_PERMISSION_NAME", myListData.getFullPermissionName());
                                view.getContext().startActivity(intent);
                                return true;
                            case R.id.item2:
                                //handle menu2 click
                                //get application package name, and permission name... populate in the intentPolicyActivity
                                Intent intentPolicyActivity = new Intent(view.getContext(), PolicyActivity.class);
                                intentPolicyActivity.putExtra("PACKAGE_NAME", mPackageName);
                                intentPolicyActivity.putExtra("PERMISSION_NAME", myListData.getFullPermissionName());
                                view.getContext().startActivity(intentPolicyActivity);
                                return true;
//                            case R.id.menu3:
//                                //handle menu3 click
//                                return true;
                            default:
                                return false;
                        }
                    }
                });
                //displaying the popup
                popup.show();

            }
        });

        holder.relativeLayout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Toast.makeText(v.getContext(), "click on item: " + myListData.getPermissionLabel(), Toast.LENGTH_LONG).show();

                myListData.setExpanded(!myListData.isExpanded());

                Animation slideUp = AnimationUtils.loadAnimation(v.getContext(), R.anim.slide_up);
                Animation slideDown = AnimationUtils.loadAnimation(v.getContext(), R.anim.slide_down);

                if(myListData.isExpanded()){
                    //what to do when expanded?
                    holder.expandContent.setVisibility(View.VISIBLE);
                    holder.expandContent.startAnimation(slideDown);
                }else{
                    //what to do when not expanded?
                    holder.expandContent.setVisibility(View.GONE);
                    holder.expandContent.startAnimation(slideUp);
                }

                //setButtonToggleImage(holder.toggleButton, myListData.isHidden());

            }
        });

        if(myListData.isExpanded()){
            holder.expandContent.setVisibility(View.VISIBLE);
        }else{
            holder.expandContent.setVisibility(View.GONE);
        }

//        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(view.getContext(), "click on item: " + myListData.getPermissionName(), Toast.LENGTH_LONG).show();
//
//                // Get the current state of the item
//                boolean expanded = myListData.isExpanded();
//                // Change the state
//                myListData.setExpanded(!expanded);
//                // Notify the adapter that item has changed
//                notifyItemChanged(position);
//            }
//        });

        holder.relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //Toast.makeText(view.getContext(),"click on item: "+ myListData.getPackageName(),Toast.LENGTH_LONG).show();
                return true;
            }
        });
    }


    @Override
    public int getItemCount() {
        return listdata.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public TextView textViewRequestsCount;
        public TextView textViewDescription;
        public TextView textViewCountCompared;
        public TextView textViewPercentage;
        public TextView textViewCountComparedBg;
        public TextView textViewPercentageBg;
        public TextView textViewCountComparedFg;
        public TextView textViewPercentageFg;
        public RelativeLayout relativeLayout;
        public LinearLayout expandContent;
        public ProgressBar progressBar;
        public ProgressBar progressBar2;
        public ImageView imageViewRequestIcon;
        public TextView textViewButtonMenu;

        public ViewHolder(View itemView) {
            super(itemView);
            this.setIsRecyclable(false);
            this.textView = (TextView) itemView.findViewById(R.id.textView);
            this.textViewRequestsCount = (TextView) itemView.findViewById(R.id.textViewRequestCount1);
            this.textViewDescription = (TextView) itemView.findViewById(R.id.tvDescription);
//            this.textViewCountCompared = (TextView) itemView.findViewById(R.id.fromTotalCount);
//            this.textViewPercentage = (TextView) itemView.findViewById(R.id.textViewPercentage);
            this.textViewCountComparedFg = (TextView) itemView.findViewById(R.id.fromTotalCountForeground);
            this.textViewPercentageFg = (TextView) itemView.findViewById(R.id.textViewPercentageForeground);
            this.textViewCountComparedBg = (TextView) itemView.findViewById(R.id.fromTotalCountBackground);
            this.textViewPercentageBg = (TextView) itemView.findViewById(R.id.textViewPercentageBackground);
            this.progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            this.progressBar2 = (ProgressBar) itemView.findViewById(R.id.progressBar2);
            this.imageViewRequestIcon = (ImageView) itemView.findViewById(R.id.imageViewPermissionIcon);
            relativeLayout = (RelativeLayout) itemView.findViewById(R.id.relativeLayout);
            this.expandContent = itemView.findViewById(R.id.expandContent);
            this.textViewButtonMenu = itemView.findViewById(R.id.textViewOptions);

        }
    }
}
