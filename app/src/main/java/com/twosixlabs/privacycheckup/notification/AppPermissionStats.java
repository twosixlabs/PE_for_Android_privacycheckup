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

package com.twosixlabs.privacycheckup.notification;

import android.content.Context;

import com.twosixlabs.privacycheckup.db.DatabaseClient;
import com.twosixlabs.privacycheckup.db.requests.DangerousPermissionRequest;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class AppPermissionStats {
    private String packageName;
    private long binWidthMillis;
    private Date rangeStart;
    private Date rangeEnd;
    private List<DangerousPermissionRequest> perms;

    private Map<String, Integer> foregroundRequests = null;
    private Map<String, Integer> backgroundRequests = null;

    /**
     * Create an object representing application permission usage statistics.
     * @param packageName App package name
     * @param rangeStart When these stats being
     * @param rangeEnd When these stats end
     * @param binWidthMillis Binning window width in milliseconds. Multiple permission requests
     *                       within a binning window count as just 1.
     * @param c App context. Used to poll the local database.
     */
    public AppPermissionStats(String packageName, Date rangeStart, Date rangeEnd, long binWidthMillis, Context c) {
        this.packageName = packageName;
        this.binWidthMillis = binWidthMillis;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;

        final boolean ALLOWED = true;
        String[] packages = new String[]{packageName};
        List<DangerousPermissionRequest> dprs  = DatabaseClient.getInstance(c).getAppDatabase()
                                                .dangerousPermissionRequestDao()
                                                .loadAllAllowedWithPackageNameInRange(packages, rangeStart, rangeEnd, ALLOWED);
        Collections.sort(dprs, new TimestampComparator());
        this.perms = dprs;
    }

    /**
     *
     * @return A list of timestamps (in milliseconds) signifying the beginning of each binning window.
     */
    private List<Long> getBins() {
        long startMillis = rangeStart.getTime();
        long endMillis = rangeEnd.getTime();

        long startBinMillis = startMillis - (startMillis % binWidthMillis);
        List<Long> bins = new LinkedList<>();
        for(long bin = startBinMillis; bin <= endMillis; bin += binWidthMillis) {
            bins.add(bin);
        }

        return bins;
    }

    /**
     *
     * @param isBackground Set to true to return only background permission stats. False returns
     *                     only foreground permission stats.
     * @return A mapping between permissions and the number of binning windows that permission was
     * granted.
     */
    public Map<String, Integer> getPermissionBinCounts(boolean isBackground) {
        // Check the cache first
        if(isBackground && backgroundRequests != null) {
            return backgroundRequests;
        }
        if(!isBackground && foregroundRequests != null) {
            return foregroundRequests;
        }

        // Filter for the permissions matching the specified visibility
        // Also, initialize a mapping of permissions and bin counts
        List<DangerousPermissionRequest> visibilityPerms = new LinkedList<>();
        Map<String, Integer> binCounts = new TreeMap<>();
        for(DangerousPermissionRequest dpr : perms) {
            if(isBackground == dpr.isBackground) {
                visibilityPerms.add(dpr);

                String permission = dpr.permission;
                if(!binCounts.containsKey(permission)) {
                    binCounts.put(permission, 0);
                }
            }
        }

        // Traverse the bin widths through the list of filtered permissions
        List<Long> bins = getBins();
        for(Long binVal : bins) {
            long binStart = binVal.longValue();
            long binEnd = binStart + binWidthMillis;
            Set<String> permissionsSeen = new TreeSet<>();

            while (!visibilityPerms.isEmpty() &&
                    visibilityPerms.get(0).timestamp.getTime() >= binStart &&
                    visibilityPerms.get(0).timestamp.getTime() < binEnd) {

                DangerousPermissionRequest dpr = visibilityPerms.remove(0);
                String permission = dpr.permission;
                if(!permissionsSeen.contains(permission)) {
                    permissionsSeen.add(permission);

                    int count = binCounts.get(permission) + 1;
                    binCounts.put(permission, count);
                }
            }
        }

        if(isBackground) {
            backgroundRequests = binCounts;
        } else {
            foregroundRequests = binCounts;
        }

        return binCounts;
    }

    /**
     *
     * @return The app package name for these statistics
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     *
     * @return When these statistics start
     */
    public Date getRangeStart() {
        return rangeStart;
    }

    /**
     *
     * @return When these statistics end
     */
    public Date getRangeEnd() {
        return rangeEnd;
    }

    /**
     *
     * @return The width of the binning window, in milliseconds
     */
    public long getBinWidthMillis() {
        return binWidthMillis;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("Package = " + packageName + "\n");
        sb.append("Range start = " + rangeStart.toString() + "\n");
        sb.append("Range end = " + rangeEnd.toString() + "\n");
        sb.append("Perm count = " + perms.size() + "\n");

        if(!perms.isEmpty()) {
            DangerousPermissionRequest first = perms.get(0);
            DangerousPermissionRequest last = perms.get(perms.size() - 1);

            sb.append(String.format("First perm time = %s", first.timestamp.toString()) + "\n");
            sb.append(String.format("Last perm time = %s", last.timestamp.toString()));
        }

        return sb.toString();
    }

    private class TimestampComparator implements Comparator<DangerousPermissionRequest> {

        @Override
        public int compare(DangerousPermissionRequest o1, DangerousPermissionRequest o2) {
            long timestampDiff = o1.timestamp.getTime() - o2.timestamp.getTime();

            if(timestampDiff > 0) {
                return 1;
            } else if(timestampDiff < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
