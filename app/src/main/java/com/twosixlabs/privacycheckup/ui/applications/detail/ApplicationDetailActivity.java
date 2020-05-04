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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.twosixlabs.privacycheckup.PolicyActivity;
import com.twosixlabs.privacycheckup.R;
import com.twosixlabs.privacycheckup.db.DatabaseClient;
import com.twosixlabs.privacycheckup.db.requests.DangerousPermissionRequest;
import com.twosixlabs.privacycheckup.ui.requests.PermissionRequestData;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApplicationDetailActivity extends AppCompatActivity {
    private static final String TAG = ApplicationDetailActivity.class.getName();

    List<PermissionRequestData> mRequestsList = new ArrayList<>();
    public String applicationPackage = "";
    ArrayList<String[]> mGlobalFrequencies = new ArrayList();
    ApplicationDetailListAdapter mAdapter;

    // For the one hour view
    public final int[] fiveMinutelyData = new int[12];
    public final int[] fiveMinutelyDataBackground = new int[12];
    String[] minutes = new String[12]; //use this for minute labels

    // For the one day view
    public final int[] hourlyData = new int[24];
    public final int[] hourlyDataBackground = new int[24];
    String[] hours = new String[24]; // for hour labels

    // For the one week view
    public final int[] weekDailyData = new int[7];
    public final int[] weekDailyDataBackground = new int[7];
    String[] weekDays = new String[7];

    // For the 30 day view
    public final int[] monthDailyData = new int[30];
    public final int[] monthDailyDataBackground = new int[30];
    String[] monthDays = new String[30];

    String viewMode = "";

    BarChart barChart;
    BarDataSet barDataSet;
    BarDataSet barDataSetBackground;
    BarData globalBarData = new BarData();

    ArrayList<BarEntry> entries = new ArrayList<>();
    ArrayList<BarEntry> entriesBackground = new ArrayList<>();

    int totalForegroundRequests = 0;
    int totalBackgroundRequests = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // create menu bar
        Toolbar myToolbar = findViewById(R.id.toolbar);
        myToolbar.setOverflowIcon(getDrawable(R.drawable.ic_more_vert_white_24dp));
        setSupportActionBar(myToolbar);

        Intent intent = getIntent();
        final String title = intent.getStringExtra("EXTRA_APPLICATION_PACKAGE");
        getSupportActionBar().setTitle("Permission Requests");
        Log.d(TAG, "Got intent for package: " + title);
        this.applicationPackage = title;

        mAdapter = new ApplicationDetailListAdapter(mRequestsList, this, applicationPackage);


        PackageManager pm = getPackageManager();
        ApplicationInfo appInfo = null;

        try {
            appInfo = pm.getApplicationInfo(title, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView tvTitle = findViewById(R.id.appName);
        ImageView ivLogo = findViewById(R.id.imageView2);


        if(appInfo == null){
            //here we should notify user that the app is not installed!
            tvTitle.setText(title);

            new AlertDialog.Builder(this)
                    .setTitle("App Not Found")
                    .setMessage("The application you are viewing permission details for is not currently installed.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue
                        }
                    })
                    .show();
        }else{
            tvTitle.setText(appInfo.loadLabel(pm));
            ivLogo.setImageDrawable(appInfo.loadIcon(pm));
        }

        ivLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + title)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + title)));
                }
            }
        });

        //Add recycler view
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view_requests);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        /**
         * Bin size thing
         */
        Spinner binSizeSelect = (Spinner) findViewById(R.id.spinnerTimeSelectOld);


        binSizeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //necessary or we get an index oob error
                mRequestsList.clear();
                entries.clear();
                mGlobalFrequencies.clear();
                barChart.highlightValues(null);

                totalBackgroundRequests = 0;
                totalForegroundRequests = 0;

                Arrays.fill(hourlyData, 0);
                Arrays.fill(hourlyDataBackground, 0);
                Arrays.fill(fiveMinutelyData, 0);
                Arrays.fill(fiveMinutelyDataBackground, 0);

                String item = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Selected " + item);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date today = cal.getTime();
                Date beforeToday;
                int binCount = 0;

                SimpleDateFormat format = new SimpleDateFormat("EEEE, MMMM d"); //default format
                TextView tvTimeInfo = findViewById(R.id.textViewTimeInfo);

                switch(position){
                    case 0: //hour
                        binCount = 12; //one for every five mins
                        viewMode = "hour";

                        //get the hourly time now, we can't use the special adjusted date from above
                        Calendar calNow = Calendar.getInstance();
                        today = calNow.getTime();

                        //TODO Uncomment the line below to use all day for the last 30 days as a single hour of data
//                        calNow.add(Calendar.DAY_OF_MONTH, -30);
                        calNow.add(Calendar.HOUR_OF_DAY, -2);
                        beforeToday = calNow.getTime();
                        Log.d(TAG, "Checking for events from " + beforeToday.toString() + " to " + today.toString());
                        new LoadPermissionsForPackageInRange(getApplicationContext(), title, beforeToday, today, binCount).execute();

                        //for hour, let's show the day and the time range
                        format = new SimpleDateFormat("EEEE, MMMM d");
                        SimpleDateFormat formatTime = new SimpleDateFormat("h:mm aa");
                        tvTimeInfo.setText(format.format(beforeToday) + ", " + formatTime.format(beforeToday) + " - " + formatTime.format(today));

                        break;
                    case 1: //day
                        binCount = 24; //one for each hour of the day
                        viewMode = "day";

                        cal.add(Calendar.DAY_OF_MONTH, -30);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "Checking for events from " + beforeToday.toString() + " to " + today.toString());
                        new LoadPermissionsForPackageInRange(getApplicationContext(), title, beforeToday, today, binCount).execute();

                        //for day, we'll just show today's date!
                        tvTimeInfo.setText(format.format(today));

                        displayToday();

                        break;
                    case 2: //week / 7 days
                        binCount = 7; //one for each day, E-Z

                        cal.add(Calendar.DAY_OF_MONTH, -7);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "Checking for events from " + beforeToday.toString() + " to " + today.toString());
                        new LoadPermissionsForPackageInRange(getApplicationContext(), title, beforeToday, today, binCount).execute();

                        //for day range, show day of week and numbers
                        tvTimeInfo.setText(format.format(beforeToday) + " - " + format.format(today));

                        break;
                    case 3: //month / 30 days
                        binCount = 30; //one for eachday...?
                        cal.add(Calendar.DAY_OF_MONTH, -30);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "Checking for events from " + beforeToday.toString() + " to " + today.toString());
                        new LoadPermissionsForPackageInRange(getApplicationContext(), title, beforeToday, today, binCount).execute();

                        //for a whole month, include the year and drop the day of the week!
                        format = new SimpleDateFormat("MMMM d, YYYY");
                        tvTimeInfo.setText(format.format(beforeToday) + " - " + format.format(today));

                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // CONFIGURE ARRAYLIST
        List<String> timePeriod = new ArrayList<String>();
        timePeriod.add("Last hour");
        timePeriod.add("Today");
//        timePeriod.add("Last week");
//        timePeriod.add("Last month");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, timePeriod);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        binSizeSelect.setAdapter(dataAdapter);
        binSizeSelect.setSelection(0);

        /**
         * BAR CHART
         */

        //create barchart
        barChart = (BarChart)findViewById(R.id.barchart);

        //TODO: Re-enable marker pending feedback
        //IMarker marker = new CustomBarMarker(this, R.layout.bar_marker_layout);
        //barChart.setMarker(marker);

        barChart.animateXY(0, 1000);
        barChart.setScaleYEnabled(false);
        barChart.getLegend().setEnabled(false);   // Hide the legend
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setDrawGridLines(true);
        barChart.getAxisRight().setEnabled(true);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisLeft().setTextSize(12f);
        barChart.setFitBars(true);
        barChart.fitScreen();

        final TextView removeHighlightButton = findViewById(R.id.textViewRemoveHighlight);
        removeHighlightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked the clear image");

                //TODO Make this proper, for now just clear it
                barChart.highlightValues(null);
                TextView tvTimeInfo = findViewById(R.id.textViewHighlightDetailTime);
                tvTimeInfo.setText("");
                removeHighlightButton.setVisibility(View.GONE);

                resetDataTable();
            }
        });

        //Should onChartValueSelectedListener be different depending on what I'm looking at?!
        barChart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event){
                //Could be useful later
                return false;
            }
        });

        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (Build.VERSION.SDK_INT >= 26) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
                }

                Log.d(TAG, "X val is " + ((BarEntry) e).getX());

                //TODO: Deal with this when formatting is better on each chart entry label
                TextView tvTimeInfo = findViewById(R.id.textViewHighlightDetailTime);
                removeHighlightButton.setVisibility(View.VISIBLE);

                if(viewMode.equals("hour")){
                    tvTimeInfo.setText("Requests at " + minutes[(int)((BarEntry) e).getX()].replace("\n", " "));
                }else if(viewMode.equals("day")){
                    tvTimeInfo.setText("Requests at " + hours[(int)((BarEntry) e).getX()].replace("\n", " "));
                }

                float[] bg = ((BarEntry) e).getYVals();
                Log.d(TAG, "bg: " + Arrays.toString(bg));

                TextView tvTotal = findViewById(R.id.textViewTotalRequests);
                TextView tvBackground = findViewById(R.id.textViewBackgroundRequests);
                TextView tvForeground = findViewById(R.id.textViewForegroundRequests);
                tvTotal.setText(Integer.toString((int)bg[0]+Math.abs((int)bg[1])).toString());
                tvBackground.setText(Integer.toString(Math.abs((int)bg[1])));
                tvForeground.setText((Integer.toString((int)bg[0])));
            }

          @Override
            public void onNothingSelected() {
                Log.d(TAG, "De-select");
            }
        });
        
        //FORMAT X AXIS VALUES
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.setExtraBottomOffset(50);


        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);
//        xAxis.setAxisMinimum(0f);
//        xAxis.setAxisMaximum(24f);
//        xAxis.setAxisMaximum();
        xAxis.setLabelCount(7);
        xAxis.setXOffset(0);
//        xAxis.setCenterAxisLabels(true);

        barChart.setHorizontalScrollBarEnabled(true);
        barChart.getViewPortHandler().setMaximumScaleX(3f);

        entries = new ArrayList<>();

        barDataSet = new BarDataSet(entries, "Data Set 1");
        barDataSet.setColors(Color.parseColor("#e74c3c"), Color.parseColor("#3498db"), Color.parseColor("#CCCCCC"));
        barDataSet.setBarBorderWidth(0.9f);
        barDataSet.setValueTextSize(30f);
        barDataSet.setValueFormatter(new IntValueFormatter());

        globalBarData = new BarData(barDataSet);
        barChart.setData(globalBarData);
        barChart.invalidate();

    }

    /**
     * This function will modify UI elements to display a breakdown of permission requests for today
     */
    private void displayToday() {


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.application_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_show_policies:
                // Trigger the notification
                Intent intentPolicyActivity = new Intent(this, PolicyActivity.class);
                intentPolicyActivity.putExtra("PACKAGE_NAME", this.applicationPackage);
                startActivity(intentPolicyActivity);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public class IntValueFormatter extends ValueFormatter {

        @Override
        public String getFormattedValue(float value){

            return "test";
        }

    }

    private static ArrayList<BarEntry> getBarEntriesForHour(int[] fiveMinutelyData, int[] fiveMinutelyDataBackground){
        ArrayList<BarEntry> entries = new ArrayList<>();

        for(int i = 0; i < 12; i++){
            entries.add(new BarEntry(i, new float[]{fiveMinutelyData[i], fiveMinutelyDataBackground[i]}));
        }

        return entries;
    }

    private static ArrayList<BarEntry> getBarEntriesForDay(int[] hourlyData, int[] hourlyDataBackground){
        ArrayList<BarEntry> entries = new ArrayList<>();

        for(int i = 0; i < 24; i++){
            entries.add(new BarEntry(i, new float[]{hourlyData[i], hourlyDataBackground[i]}));
//            entries.add(new BarEntry(i, hourlyData[i]));

        }

        return entries;
    }





    private class LoadPermissionsForPackageInRange extends AsyncTask<Void, Void, List<DangerousPermissionRequest>> {
        private WeakReference<Context> contextRef;
        private Context mContext;
        String packageName;
        Date from;
        Date to;
        int binCount;

        LoadPermissionsForPackageInRange(Context context, String packageName, Date from, Date to, int binCount) {
            contextRef = new WeakReference<>(context);
            this.mContext = context;
            this.packageName = packageName;
            this.from = from;
            this.to = to;
            this.binCount = binCount;
        }

        @Override
        protected List<DangerousPermissionRequest> doInBackground(Void... voids) {
            String[] arrayIn = new String[] { packageName };
            List<DangerousPermissionRequest> privateDataRequests = DatabaseClient.getInstance(contextRef.get()).getAppDatabase()
                    .dangerousPermissionRequestDao().loadAllWithPackageNameInRange(arrayIn, from, to);

            return privateDataRequests;
        }

        @Override
        protected void onPostExecute(List<DangerousPermissionRequest> reqs) {
            mRequestsList.clear();
            mGlobalFrequencies.clear();



            Log.d(TAG, "onPostExecute for LoadAppsTask...");
            List<DangerousPermissionRequest> requests = reqs;
            Log.d(TAG, "Got list of size " + reqs.size());
            Log.d(TAG, "Performing  calculation for binCount of " + binCount);

            //stores all tempValues of names
            ArrayList<String> tempValues = new ArrayList();
            ArrayList<String> tempValuesFg = new ArrayList();
            ArrayList<String> tempValuesBg = new ArrayList();

            switch(binCount){
                case 12: //one hour
                    //populate the hourly data which does the graph


                    for(DangerousPermissionRequest req : reqs){
                        String app = req.permission;
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(req.timestamp);

                        Calendar calFrom = Calendar.getInstance();
                        calFrom.setTime(from);

                        //determine which five min interval this falls into, return a value 0 to 11
                        /**
                         * Let's say there are just three events:
                         * one at 10:51
                         * one at 11:13
                         * one at 11:25
                         *
                         * divide 51 by 5, we get 10.02 --- bin 10!
                         * divide 13 by 5, we get 2.6 --- bin 2!
                         * divide 25 by 5, we get 5 ---- bin 5!
                         *
                         * this will not properly represent distribution, since timestamps will be from
                         * 10:45 to 11:40. the event at 10:51 will be put at around the 11:20 mark...
                         *
                         * what do we have to add to account for this?
                         *
                         * level the playing field by subtracting the minutes from each event
                         *
                         * so, range starts at 10:45... subtract 45 from each event!
                         * 10:51 - 45m becomes 10:06 -> bin 1
                         * 11:13 - 45m becomes 10:26 ->  bin 4
                         * 11:25 - 45m becomes 10:40 -> bin 8
                         */


                        int minOffset = calFrom.get(Calendar.MINUTE);
                        Log.d(TAG, "Got offset value of " + minOffset);
                        Log.d(TAG, "Before adjust, timestamp is " + cal.getTime().toString());
                        cal.add(Calendar.MINUTE, -minOffset);
                        int minForBinning = cal.get(Calendar.MINUTE);
                        Log.d(TAG, "After adjust, timestamp is " + cal.getTime().toString());

                        int bin = (int)Math.floor((double)minForBinning/5);
                        Log.d(TAG, "Got a bin of " + bin);

                        if(!req.isBackground){ //foreground
                            fiveMinutelyData[bin]++; //...and use this as the index of the array.
                            tempValuesFg.add(app);
                        }else{ //background
                            fiveMinutelyDataBackground[bin]--;
                            tempValuesBg.add(app);
                        }

                        tempValues.add(app);
                    }

                    //customize the X axis for this data view
                    minutes = calculateXAxisForOneHour();

                    Log.d(TAG, "Number of X labels is " + minutes.length);
                    IndexAxisValueFormatter formatterMinutes = new IndexAxisValueFormatter(minutes);
                    barChart.getXAxis().setValueFormatter(formatterMinutes);

                    entries = getBarEntriesForHour(fiveMinutelyData, fiveMinutelyDataBackground);
                    barDataSet = new BarDataSet(entries, "Data Set: Every 5 Mins");

                    totalForegroundRequests = 0;
                    for(int i:fiveMinutelyData)
                        totalForegroundRequests+=i;

                    totalBackgroundRequests = 0;
                    for(int i:fiveMinutelyDataBackground)
                        totalBackgroundRequests+=i;

                    resetDataTable();

                    break;
                case 24: //one day
                    //populate the hourly data which does the graph
                    for(DangerousPermissionRequest req : reqs){
                        String app = req.permission;
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(req.timestamp);

                        int hour = cal.get(Calendar.HOUR_OF_DAY); //get the raw hour...

                        if(!req.isBackground){ //foreground
                            hourlyData[hour]++; //...and use this as the index of the array.
                            tempValuesFg.add(app);
                        }else{ //background
                            hourlyDataBackground[hour]--;
                            tempValuesBg.add(app);
                        }

                        tempValues.add(app);
                    }

                    //customize the X axis for this data view
                    hours = calculateXAxisForOneDay();

                    Log.d(TAG, "Number of X labels is " + hours.length);
                    IndexAxisValueFormatter formatter = new IndexAxisValueFormatter(hours);
                    barChart.getXAxis().setValueFormatter(formatter);

                    entries = getBarEntriesForDay(hourlyData, hourlyDataBackground);
                    barDataSet = new BarDataSet(entries, "Data Set: Every Hour");

                    totalForegroundRequests = 0;
                    for(int i:hourlyData)
                        totalForegroundRequests+=i;

                    totalBackgroundRequests = 0;
                    for(int i:hourlyDataBackground)
                        totalBackgroundRequests+=i;

                    resetDataTable();

                    break;
                case 7: //one week
                    break;
                case 30: //30 day
                    break;

            }



            Log.d(TAG, "Total foreground perm reqs: " + Arrays.toString(hourlyData));
            Log.d(TAG, "Total background perm reqs: " +  Arrays.toString(hourlyDataBackground));

            //create hash set from list of names, so that we only get unique
            Set<String> uniqueValues = new HashSet(tempValues);

            //count occurences of each app
            for(String val : uniqueValues){
                int occurrences = Collections.frequency(tempValues, val);
                int occurrencesFg = Collections.frequency(tempValuesFg, val);
                int occurrencesBg = Collections.frequency(tempValuesBg, val);

                Log.d(TAG, packageName + ": " + val + " occurs " + occurrences);
                mGlobalFrequencies.add(new String[]{val, String.valueOf(occurrences), String.valueOf(occurrencesFg), String.valueOf(occurrencesBg)});
            }

            for(int i = 0; i < mGlobalFrequencies.size(); i++){ //iterate over list and store permissionrequestdata in object
                //get x value entry for a string
                String label = mGlobalFrequencies.get(i)[0];
                int value = Integer.parseInt(mGlobalFrequencies.get(i)[1]);

                PackageManager pm = mContext.getPackageManager();
                PermissionInfo pi = null;
                try {
                    pi = pm.getPermissionInfo(label, PackageManager.GET_META_DATA);

                    Log.d(TAG, (String) pi.loadLabel(pm));
                    Log.d(TAG, (String) pi.loadDescription(pm));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                PermissionGroupInfo groupInfo = null;
                try {
                    groupInfo = pm.getPermissionGroupInfo(pi.group, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                Drawable permissionLogoDrawable = null;

                try {
                    if(groupInfo != null){
                        permissionLogoDrawable =  pm.getResourcesForApplication("android").getDrawable(groupInfo.icon);
                    }else {
                        permissionLogoDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_security_black_24dp);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                PermissionRequestData prd = new PermissionRequestData(
                        label, //package name
                        (String) pi.loadLabel(pm),
                        (String) pi.loadDescription(pm),
                        permissionLogoDrawable,
                        Integer.parseInt(mGlobalFrequencies.get(i)[1]), //number of requests, total
                        Integer.parseInt(mGlobalFrequencies.get(i)[2]), //number of fg requests
                        Integer.parseInt(mGlobalFrequencies.get(i)[3]), //number of bg requests
                        false,
                        false); //hidden or not

                mRequestsList.add(prd);
            }

            Collections.sort(mRequestsList, new Comparator<PermissionRequestData>() {
                @Override
                public int compare(PermissionRequestData perm1, PermissionRequestData perm2){
                    return perm1.getRequestCount() > perm2.getRequestCount() ? -1 : (perm1.getRequestCount() < perm2.getRequestCount()) ? 1 : 0;
                }
            });


            barChart.clearValues();

            ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();

            barDataSet.setColors(Color.parseColor("#43a047"), Color.parseColor("#e74c3c"));
            barDataSet.setStackLabels(new String[]{"Test1", "Test2", "Test3"});
            barDataSet.setDrawValues(false);
            dataSets.add(barDataSet);
            globalBarData = new BarData(dataSets);

            barChart.setData(globalBarData);
            barChart.notifyDataSetChanged();
            mAdapter.notifyDataSetChanged();
            barChart.setXAxisRenderer(new CustomXAxisRenderer(barChart.getViewPortHandler(), barChart.getXAxis(), barChart.getTransformer(YAxis.AxisDependency.LEFT)));

        }
    }

    private void resetDataTable() {
        barChart.highlightValues(null);
        TextView tvTimeInfo = findViewById(R.id.textViewHighlightDetailTime);
        tvTimeInfo.setText("");

        TextView removeHighlightButton = findViewById(R.id.textViewRemoveHighlight);
        removeHighlightButton.setVisibility(View.GONE);

        //entry 1
        TextView tvTotal = findViewById(R.id.textViewTotalRequests);
        int totalRequests = (totalForegroundRequests + (-totalBackgroundRequests));
        tvTotal.setText("" + totalRequests);

        //entry 2
        TextView tvForeground = findViewById(R.id.textViewForegroundRequests);
        tvForeground.setText(""+ totalForegroundRequests);

        //entry 3
        TextView tvBackground = findViewById(R.id.textViewBackgroundRequests);
        tvBackground.setText(""+ (-totalBackgroundRequests));
    }

    private static String[] calculateXAxisForOneDay(){
        String[] hours = new String[24];

        DateFormat df = new SimpleDateFormat("h\naa");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        int startDate = cal.get(Calendar.DATE);
        int hour = 0;

        while (cal.get(Calendar.DATE) == startDate) {
            hours[hour] = df.format(cal.getTime());
            System.out.println(df.format(cal.getTime()));
            cal.add(Calendar.MINUTE, 60);
            hour++;
        }

//
//        for(int i = 0; i < 24; i++){
//            hours[i] = Integer.toString(i+1) + "\n" + "hours";
//        }

        return hours;
    }

    private String[] calculateXAxisForOneHour() {
        String[] minutes = new String[12];

        DateFormat df = new SimpleDateFormat("h:mm\naa");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -2);
        int startDate = cal.get(Calendar.DATE);
        int minute = 0;

        while (minute < 12) {
            minutes[minute] = df.format(cal.getTime());
            System.out.println(df.format(cal.getTime()));
            cal.add(Calendar.MINUTE, 5);
            minute++; //represents add'l five mins
        }

        return minutes;
    }

    public class CustomXAxisRenderer extends XAxisRenderer {
        public CustomXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
            super(viewPortHandler, xAxis, trans);
        }

        @Override
        protected void drawLabel(Canvas c, String formattedLabel, float x, float y, MPPointF anchor, float angleDegrees) {
            try{
                if(formattedLabel != null){
                    String line[] = formattedLabel.split("\n");
                    Utils.drawXAxisValue(c, line[0], x, y, mAxisLabelPaint, anchor, angleDegrees);
                    Utils.drawXAxisValue(c, line[1], x, y + mAxisLabelPaint.getTextSize(), mAxisLabelPaint, anchor, angleDegrees);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


}
