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

package com.twosixlabs.privacycheckup.ui.requests;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.twosixlabs.privacycheckup.R;
import com.twosixlabs.privacycheckup.ui.requests.detail.RequestDetailActivity;

import java.util.List;

public class PermissionRequestListAdapter extends RecyclerView.Adapter<PermissionRequestListAdapter.ViewHolder> {
    private List<PermissionRequestData> listdata;
    public Context mContext;
    private RequestsFragment permReqFragment;

    public PermissionRequestListAdapter(List<PermissionRequestData> listdata, Context context, RequestsFragment requestsFragment){
        this.listdata = listdata;
        this.mContext = context;
        this.permReqFragment = requestsFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.layout_request_list_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PermissionRequestData myListData = listdata.get(position);

        holder.textView.setText(listdata.get(position).getPermissionLabel());
        holder.imageView.setImageDrawable(myListData.getIcon());

        if(!myListData.isPal()){ //normal permission
            holder.palLabel.setVisibility(View.GONE);
            holder.textViewRequestsCount.setText(listdata.get(position).getRequestCount() + " requests to use this permission");

        }else{ //a pal
            holder.palLabel.setVisibility(View.VISIBLE);
            holder.textViewRequestsCount.setText(listdata.get(position).getRequestCount() + " requests to this μPAL");

        }

        holder.toggleButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                myListData.setHidden(!myListData.isHidden());

                if(myListData.isHidden()){
                    permReqFragment.removeEntryByPackageName(myListData.getPermissionLabel());
                }else{
                    permReqFragment.addEntry(myListData);
                }

                setButtonToggleImage(holder.toggleButton, myListData.isHidden());
            }
        });

        if(myListData.isHidden()){
            holder.toggleButton.setBackgroundResource(R.drawable.ic_visibility_off_black_24dp);
        }else{
            holder.toggleButton.setBackgroundResource(R.drawable.ic_visibility_black_24dp);
        }

        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(view.getContext(),"click on item: "+ myListData.getPermissionLabel(),Toast.LENGTH_LONG).show();
                Intent intent = new Intent(view.getContext(), RequestDetailActivity.class);

                if(myListData.isPal()){
                    intent.putExtra("EXTRA_PAL_NAME", myListData.getFullPermissionName());
                }else{
                    intent.putExtra("EXTRA_PERMISSION_NAME", myListData.getFullPermissionName());
                }

                view.getContext().startActivity(intent);
            }

        });

        holder.relativeLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view){
                //Toast.makeText(view.getContext(),"click on item: "+ myListData.getPackageName(),Toast.LENGTH_LONG).show();
                permReqFragment.highlightByPackageName(myListData.getPermissionLabel());
                return true;
            }
        });
    }

    private void setButtonToggleImage(Button buttonView, boolean isSelected){
        if(isSelected){
            buttonView.setBackgroundResource(R.drawable.ic_visibility_off_black_24dp);
        }else{
            buttonView.setBackgroundResource(R.drawable.ic_visibility_black_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public TextView textViewRequestsCount;
        public RelativeLayout relativeLayout;
        public Button toggleButton;
        public TextView palLabel;
        public ViewHolder(View itemView) {
            super(itemView);
            this.setIsRecyclable(false);
            this.imageView = (ImageView) itemView.findViewById(R.id.imageViewRequestIcon);
            this.textView = (TextView) itemView.findViewById(R.id.textView);
            this.textViewRequestsCount = (TextView) itemView.findViewById(R.id.textViewRequestCount);
            this.toggleButton = (Button) itemView.findViewById(R.id.btnToggleVisibility);
            this.palLabel = (TextView) itemView.findViewById(R.id.textViewPalLabel);
            relativeLayout = (RelativeLayout)itemView.findViewById(R.id.relativeLayout);
        }
    }
}