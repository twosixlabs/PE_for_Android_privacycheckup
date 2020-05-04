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

import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;

/**
 * A class for individual apps as returned in the Applications view of the policy manager
 *
 * An app has:
 * - permissionName, the permission name
 * - count, number of total permissions requested in this view
 * - imgId, the icon/image
 */
public class PermissionRequestData {
    private final String fullPermissionName;

    public String getPermissionDescription() {
        return permissionDescription;
    }

    private final String permissionDescription;
    private String permissionLabel;

    public double getPercentOfAll() {
        return percentOfAll;
    }

    public void setPercentOfAll(double percentOfAll) {
        this.percentOfAll = percentOfAll;
    }

    private double percentOfAll;
    private boolean hidden;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    private boolean expanded;
    private int requestCount;
    private int backgroundRequestCount;
    private int foregroundRequestCount;
    private Drawable icon;
    public boolean isPal;


    private PermissionInfo permInfo;

    public String getFullPermissionName() {
        return fullPermissionName;
    }

    public PermissionRequestData(String fullPermissionName, String permissionLabel, String permissionDesc, Drawable icon, int requestCount, int fgRequestCount, int bgRequestCount, boolean hidden, boolean isPal) {
        this.fullPermissionName = fullPermissionName;
        this.permissionLabel = permissionLabel;
        this.permissionDescription = permissionDesc;
        this.icon = icon;
        this.requestCount = requestCount;
        this.foregroundRequestCount = fgRequestCount;
        this.backgroundRequestCount = bgRequestCount;
        this.hidden = hidden;
        this.expanded = false;
        this.isPal = isPal;
    }

    public String getPermissionLabel() {
        return permissionLabel;
    }
    public void setPermissionLabel(String permissionLabel) {
        this.permissionLabel = permissionLabel;
    }
    public Drawable getIcon() {
        return icon;
    }
    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    public boolean isPal() { return isPal; }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public int getBackgroundRequestCount() {
        return backgroundRequestCount;
    }

    public int getForegroundRequestCount() {
        return foregroundRequestCount;
    }
}
