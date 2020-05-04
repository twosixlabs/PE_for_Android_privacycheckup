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

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.twosixlabs.privacycheckup.R;
import com.twosixlabs.privacycheckup.ui.applications.AppData;
import com.twosixlabs.privacycheckup.ui.applications.detail.ApplicationDetailActivity;

import java.util.List;

public class RequestDetailListAdapter extends RecyclerView.Adapter<RequestDetailListAdapter.ViewHolder> {
    private List<AppData> listdata;
    public Context mContext;
    int totalRequestsForThisApp = 0;

    public RequestDetailListAdapter(List<AppData> listdata, Context context) {
        this.listdata = listdata;
        this.mContext = context;

    }

    @Override
    public RequestDetailListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.layout_request_detail_row, parent, false);
        RequestDetailListAdapter.ViewHolder viewHolder = new RequestDetailListAdapter.ViewHolder(listItem);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RequestDetailListAdapter.ViewHolder holder, final int position) {
        final AppData app = listdata.get(position);
        holder.textViewAppName.setText(app.getDisplayName());
        holder.textViewRequestCount.setText(app.getRequestCount() + " requests to use this permission");
        holder.imageViewAppIcon.setImageDrawable(app.getImgId());

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ApplicationDetailActivity.class);
                intent.putExtra("EXTRA_APPLICATION_PACKAGE", app.getPackageName());
                v.getContext().startActivity(intent);
            }
        });

    }


    @Override
    public int getItemCount() {
        return listdata.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewAppName;
        public TextView textViewRequestCount;
        public ImageView imageViewAppIcon;
        public RelativeLayout parentLayout;


        public ViewHolder(View itemView) {
            super(itemView);
            this.setIsRecyclable(false);
            this.parentLayout = (RelativeLayout) itemView.findViewById(R.id.relativeLayout);
            this.textViewAppName = (TextView) itemView.findViewById(R.id.textViewAppTitle);
            this.textViewRequestCount = (TextView) itemView.findViewById(R.id.textViewRequestCount);
            this.imageViewAppIcon = (ImageView) itemView.findViewById(R.id.imageViewAppIcon);
        }
    }
}
