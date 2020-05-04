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

package com.twosixlabs.privacycheckup.ui.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IPieDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.twosixlabs.privacycheckup.R;
import com.twosixlabs.privacycheckup.db.DatabaseClient;
import com.twosixlabs.privacycheckup.db.requests.DangerousPermissionRequest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ApplicationsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ApplicationsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ApplicationsFragment extends Fragment {
    private static final String TAG = ApplicationsFragment.class.getName();

    Button btnBarChart, btnPieChart;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    //Declare pie chart
    PieChart pieChart;
    Spinner spinnerDateSelect;

    AlertDialog mLoadingDialog;

    ArrayList<String[]> globalFrequencies = new ArrayList();
    List<AppData> appData = new ArrayList<>();
    AppListAdapter adapter = new AppListAdapter(appData, getContext(), this);

    PieData globalPieData = new PieData();

    private OnFragmentInteractionListener mListener;

    public ApplicationsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ApplicationsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ApplicationsFragment newInstance(String param1, String param2) {
        ApplicationsFragment fragment = new ApplicationsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        Log.d(TAG, "Created ApplicationFragment, counting applications");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_applications, container, false);


        /**
         * Button setup
         */

        Button buttonSort = v.findViewById(R.id.buttonSelectSort);
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sort by...");

        // add a list
        final String[] sortOptions = {
                "Count (Ascending)",
                "Count (Descending)",
                "Name (A to Z)",
                "Name (Z to A)",
                "Visible First",
                "Hidden First"
        };

        builder.setItems(sortOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "Sorting by " + sortOptions[which]);
                switch (which) {
                    case 0: // SIZE - asc
                        Collections.sort(appData, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2){
                                return app1.getRequestCount() > app2.getRequestCount() ? -1 : (app1.getRequestCount() < app2.getRequestCount()) ? 1 : 0;
                            }
                        });
                        break;
                    case 1: // SIZE - desc
                        Collections.sort(appData, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2){
                                return app1.getRequestCount() < app2.getRequestCount() ? -1 : (app1.getRequestCount() > app2.getRequestCount()) ? 1 : 0;
                            }
                        });
                        break;
                    case 2: // NAME - asc
                        Collections.sort(appData, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2) {
                                return app1.getDisplayName().compareToIgnoreCase(app2.getDisplayName());
                            }
                        });
                        break;
                    case 3: // NAME - desc
                        Collections.sort(appData, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2) {
                                return -app1.getDisplayName().compareToIgnoreCase(app2.getDisplayName());
                            }
                        });
                        break;
                    case 4: // VISIBLE
                        Collections.sort(appData, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2) {
                                return Boolean.compare(app1.isHidden(), app2.isHidden());
                            }
                        });
                        break;
                    case 5: // INVISIBLE
                        Collections.sort(appData, new Comparator<AppData>() {
                            @Override
                            public int compare(AppData app1, AppData app2) {
                                return -Boolean.compare(app1.isHidden(), app2.isHidden());
                            }
                        });
                        break;
                }



                adapter.notifyDataSetChanged();
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

        /**
         * SPINNER SETUP
         */

        spinnerDateSelect = v.findViewById(R.id.spinnerTimeSelect);


        spinnerDateSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //necessary or we get an index oob error
                pieChart.highlightValues(null);

                String item = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Selected " + item);

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date todayDate = cal.getTime();
                Log.d(TAG, "The date is " + todayDate.toString());

                Date beforeToday = cal.getTime();

                switch(position){
                    case 0: //today
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "The date before today is " + beforeToday.toString());
                        new LoadPermissionRequestsForTimeTask(getActivity(), beforeToday, todayDate).execute();
                        break;
                    case 1: //3 days
                        cal.add(Calendar.DAY_OF_MONTH, -3);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "The date before today is " + beforeToday.toString());
                        new LoadPermissionRequestsForTimeTask(getActivity(), beforeToday, todayDate).execute();
                        break;
                    case 2: //7 days
                        cal.add(Calendar.DAY_OF_MONTH, -7);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "The date before today is " + beforeToday.toString());
                        new LoadPermissionRequestsForTimeTask(getActivity(), beforeToday, todayDate).execute();
                        break;
                    case 3: //30 days
                        cal.add(Calendar.DAY_OF_MONTH, -30);
                        beforeToday = cal.getTime();
                        Log.d(TAG, "The date before today is " + beforeToday.toString());
                        new LoadPermissionRequestsForTimeTask(getActivity(), beforeToday, todayDate).execute();
                        break;
                    case 4: //all
                        new LoadPermissionRequestsTask(getActivity()).execute();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



        List<String> timePeriod = new ArrayList<String>();
        timePeriod.add("Today");
        timePeriod.add("Last 3 Days");
        timePeriod.add("Last 7 Days");
        timePeriod.add("Last 30 Days");
        timePeriod.add("All Time");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item, timePeriod);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinnerDateSelect.setAdapter(dataAdapter);
        spinnerDateSelect.setSelection(4);

        /**
         * CHART SETUP
         */

        // Declare + initialize the pieChart
        pieChart = v.findViewById(R.id.piechartApplications);

        //format the pie chart
        pieChart.getLegend().setEnabled(false); //dont show the legend
        pieChart.getDescription().setEnabled(false); //dont show the description text

        /**
         * CHART DATA
         */
        ArrayList<PieEntry> entries = new ArrayList<>();
        for(String[] elem : globalFrequencies){
            int value = Integer.parseInt(elem[1]);
            String label = elem[0];
            entries.add(new PieEntry(value, label));
        }

        final PieDataSet pieDataSet = new PieDataSet(entries, "entries");


        /**
         * CHART STYLE
         */
        pieDataSet.setValueTextColor(Color.BLACK);

        pieChart.setCenterText("0 Applications");
        pieChart.animateXY(1000, 1000);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setDrawEntryLabels(false);
        pieChart.setUsePercentValues(true);

        //To align chart with more space for labels...
        //pieChart.setExtraOffsets(50, 25, 50, 25);


        // set the marker to the chart
        IMarker marker = new CustomMarkerView(getContext(), R.layout.marker_layout);
        pieChart.setMarker(marker);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                Highlight[] high = pieChart.getHighlighted();
                for (Highlight highlight : high) {
                    Log.d(TAG, "highlight: " + highlight.toString());
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });

        pieChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) { }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) { }

            @Override
            public void onChartLongPressed(MotionEvent e) {
                final Highlight h = pieChart.getHighlightByTouchPoint(e.getX(), e.getY());
                Highlight[] highlights= new Highlight[1];
                highlights[0] =h;
                final PieEntry pe = (PieEntry) globalPieData.getEntryForHighlight(h);

                pieChart.highlightValues(highlights);
                //pieChart.invalidate();

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                for(AppData app : appData){
                                    if(app.getDisplayName().equals(pe.getLabel())){
                                        app.setHidden(true);
                                        removeEntryByPackageName(app.getDisplayName());
                                        adapter.notifyDataSetChanged();

                                        //necessary or we get an index oob error
                                        pieChart.highlightValues(null);
                                    }
                                }

                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                PieData data = pieChart.getData();

                String label = data.getEntryForHighlight(h).toString();
                builder.setMessage("Remove application '" + pe.getLabel() + "' from the graph?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("Cancel", dialogClickListener).show();
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) { }

            @Override
            public void onChartSingleTapped(MotionEvent me) { }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) { }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) { }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) { }
        });

        //Add recycler view
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_apps);
        adapter = new AppListAdapter(appData, getContext(), this); //create list adapter with app data
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);

        return v;
    }

    // TODO: Rename method, update argument ands hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

//        new LoadPermissionRequestsTask(this.getActivity()).execute();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(R.layout.layout_loading_dialog);
        mLoadingDialog = builder.create();
        mLoadingDialog.show();


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private class LoadPermissionRequestsTask extends AsyncTask<Void, Void, List<DangerousPermissionRequest>> {
        private WeakReference<Activity> activityRef;

        LoadPermissionRequestsTask(Activity activity) {
            activityRef = new WeakReference<>(  activity);
        }

        @Override
        protected List<DangerousPermissionRequest> doInBackground(Void... voids) {
            List<DangerousPermissionRequest> privateDataRequests = DatabaseClient.getInstance(getContext()).getAppDatabase()
                    .dangerousPermissionRequestDao().getAll();

            return privateDataRequests;
        }

        @Override
        protected void onPostExecute(List<DangerousPermissionRequest> reqs) {
            //clear the global state, this should be a fresh start
            globalFrequencies.clear();
            appData.clear();
            globalPieData.clearValues();

            Log.d(TAG, "onPostExecute for LoadAppsTask...");
            List<DangerousPermissionRequest> requests = reqs;
            Log.d(TAG, "Got list of size " + reqs.size());

            /**
             * Iterate all entries and get a count for each unique value
             *
             * TODO Extract to new method
             */

            //stores all tempValues of names
            ArrayList<String> tempValues = new ArrayList();
            for(DangerousPermissionRequest req : reqs){
                String app = req.packageName;
                tempValues.add(app);
            }

            //create hash set from list of names, so that we only get unique
            Set<String> uniqueValues = new HashSet(tempValues);

            //count occurences of each app
            for(String val : uniqueValues){
                int occurrences = Collections.frequency(tempValues, val);
                Log.d(TAG, val + " occurs " + occurrences);
                globalFrequencies.add(new String[]{val, String.valueOf(occurrences)});

            }

            final PackageManager pm = getContext().getPackageManager();


            //get a list of installed apps
            List<ApplicationInfo> packages1 = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo packageInfo : packages1) {
                Log.d(TAG, "Package name : " + packageInfo.loadLabel(pm).toString());
                Log.d(TAG, "Package logo : " + packageInfo.loadIcon(pm));
                Log.d(TAG, "Installed package :" + packageInfo.packageName);
                Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
                Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
            }

            /**
             * Create a list of pie data entries which will be used to initialize piedataset
             *
             * MUST do this because we can't use globalFrequencies on it's own
             */

            ArrayList<PieEntry> frequencyPieDataEntries = new ArrayList<>();

            for(int i = 0; i < globalFrequencies.size(); i++){ //iterate over list and store AppData in object
                //get x value entry for a string
                String label = globalFrequencies.get(i)[0];
                int value = Integer.parseInt(globalFrequencies.get(i)[1]);
                List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                String name = label;
                Drawable icon = ContextCompat.getDrawable(getActivity(), android.R.drawable.ic_dialog_info);

                for(ApplicationInfo appInfo : packages){
                    if(appInfo.packageName.equals(label)){ //if we find a matching package...
                        name = appInfo.loadLabel(pm).toString(); //...grab the name!
                        icon = appInfo.loadIcon(pm); //..and the icon!

                    }else{
                        Log.d(TAG, "Couldn't find the application!");
                    }
                }

                AppData newAD= new AppData(name, label, icon, Integer.parseInt(globalFrequencies.get(i)[1]), false);
                newAD.setPercentOfAll(100);
                appData.add(newAD);

                //add pieentry with app data object as the extra data field
                double percent = (((double) value * 100) / (double) reqs.size());
                frequencyPieDataEntries.add(new PieEntry(value, name, percent));

            }


            final PieDataSet newPieDataSet = new PieDataSet(frequencyPieDataEntries, "entries");

            //format the data set
            newPieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            newPieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            newPieDataSet.setDrawValues(false); //hide the tempValues inside slices

            globalPieData = new PieData(newPieDataSet);
            Log.d(TAG, "Setting the pieData to the chart. data is size " + globalPieData.getEntryCount());
            pieChart.setData(globalPieData);
            pieChart.setCenterText(globalPieData.getEntryCount() + " applications");
            pieChart.invalidate();
            pieChart.notifyDataSetChanged();



            //first time, sort by size
            //TODO Extract to utility method this is duplicate code
            Collections.sort(appData, new Comparator<AppData>() {
                @Override
                public int compare(AppData app1, AppData app2){
                    return app1.getRequestCount() > app2.getRequestCount() ? -1 : (app1.getRequestCount() < app2.getRequestCount()) ? 1 : 0;
                }
            });

            adapter.notifyDataSetChanged();

            if(mLoadingDialog.isShowing()){
                mLoadingDialog.dismiss();
            }

        }
    }

    private class LoadPermissionRequestsForTimeTask extends AsyncTask<Void, Void, List<DangerousPermissionRequest>> {
        private WeakReference<Activity> activityRef;

        Date from;
        Date to;

        LoadPermissionRequestsForTimeTask(Activity activity, Date from, Date to) {
            activityRef = new WeakReference<>(  activity);
            this.from = from;
            this.to = to;
        }

        @Override
        protected List<DangerousPermissionRequest> doInBackground(Void... voids) {
            List<DangerousPermissionRequest> privateDataRequests = DatabaseClient.getInstance(getContext()).getAppDatabase()
                    .dangerousPermissionRequestDao().loadAllInRange(from, to);

            return privateDataRequests;
        }

        @Override
        protected void onPostExecute(List<DangerousPermissionRequest> reqs) {
            Log.d(TAG, "onPostExecute for LoadReqsForTimeTask...");

            globalFrequencies.clear();
            appData.clear();
            globalPieData.clearValues();

            //get list of all requests for this time
            List<DangerousPermissionRequest> requests = reqs;
            Log.d(TAG, "Got list of size " + reqs.size());

            //stores all values of names
            ArrayList<String> values = new ArrayList();

            //get all unique apps...
            for(DangerousPermissionRequest req : reqs){
                String app = req.packageName;
                values.add(app);
            }

            //create hash set from list of names
            Set<String> uniqueValues = new HashSet(values);

            //count occurences of each app
            for(String val : uniqueValues){
                int occurrences = Collections.frequency(values, val);
                Log.d(TAG, val + " occurs " + occurrences);
                globalFrequencies.add(new String[]{val, String.valueOf(occurrences)});
            }

            //use this to store data for the pie chart
            ArrayList<PieEntry> newEntries = new ArrayList<>();

//            appData = new ArrayList<>();
            appData.clear();

            final PackageManager pm = getContext().getPackageManager();

            for(int i = 0; i < globalFrequencies.size(); i++){ //iterate over list and store AppData in object
                //get x value entry for a string
                String label = globalFrequencies.get(i)[0];

                List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                String name = label;
                Drawable icon = ContextCompat.getDrawable(getActivity(), android.R.drawable.ic_dialog_info);

                for(ApplicationInfo appInfo : packages){
                    Log.d(TAG, "Checking against " + appInfo.packageName + " with the label of " + label);
                    if(appInfo.packageName.equals(label)){ //if we find a matching package...
                        Log.d(TAG, "Found the application with package name " + label);
                        name = appInfo.loadLabel(pm).toString(); //...grab the name!f
                        icon = appInfo.loadIcon(pm); //..and the icon!
                        break;
                    }else{
                        Log.d(TAG, "Couldn't find the application " + label + " in the set of installed applications");
                    }
                }

                appData.add(new AppData(name, label, icon, Integer.parseInt(globalFrequencies.get(i)[1]), false));

                int value = Integer.parseInt(globalFrequencies.get(i)[1]);
                double percent = (((double) value * 100) / (double) reqs.size());
                newEntries.add(new PieEntry(value, name, percent));

            }

            //create pie data set from the entries arraylist
            final PieDataSet newPieDataSet = new PieDataSet(newEntries, "entries");

            //format the data set
            newPieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            newPieDataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            newPieDataSet.setDrawValues(false); //hide the values inside slices

            globalPieData = new PieData(newPieDataSet);
            globalPieData.setValueFormatter(new PercentFormatter(pieChart));

            pieChart.setData(globalPieData);
            pieChart.setCenterText(globalPieData.getEntryCount() + " applications");
            pieChart.invalidate();
            pieChart.notifyDataSetChanged();

            //add count data to the pie chart
            for(String[] elem : globalFrequencies) {

            }

            adapter.notifyDataSetChanged();


        }
    }

    /**
     * Add entry back to piechart
     *
     * @param app
     */
    public void addEntry(AppData app) {
        Log.d(TAG, "Adding entry back to graph: " + app.getDisplayName());
        //get our own copy of the data set
        DataSet<PieEntry> data = (DataSet<PieEntry>) globalPieData.getDataSet();
        data.addEntry(new PieEntry(app.getRequestCount(), app.getDisplayName()));

        globalPieData = new PieData((IPieDataSet) data);

        pieChart.setCenterText(globalPieData.getEntryCount() + " Applications");

        pieChart.invalidate();
        pieChart.notifyDataSetChanged();
        pieChart.animateXY(1000, 1000);
    }

    /**
     * Remove entry from piechart by iterating over data and removing using name as key
     *
     * TODO: Improve efficiency
     *
     * @param label
     */
    public void removeEntryByPackageName(String label){
        pieChart.invalidate();
        mLoadingDialog.show();
        Log.d(TAG, "Removing entry: " + label);

        //get our own copy of the data set
        DataSet<PieEntry> data = (DataSet<PieEntry>) globalPieData.getDataSet();

        for(int i = 0; i < data.getEntryCount(); i++){
            Log.d(TAG, "RUNNING LOOP FOR THE " + i + " TIME! WE CAN ONLY RUN " + data.getEntryCount() + " TIMES");

                //get the entry for that index
                PieEntry pe = null;

                try {
                    pe = data.getEntryForIndex(i);
                }catch(Exception e){
                    Log.d(TAG, "Aha! ");
                }

                if(pe != null && pe.getLabel().equals(label)){
                    //TODO possible race condition here
                    try{
                        Log.d(TAG, "Got a match for removing with label: " + label);
                        globalPieData.removeEntry(pe, 0);

                        pieChart.setCenterText(globalPieData.getEntryCount() + " Applications");

                        pieChart.invalidate();
                        pieChart.notifyDataSetChanged();

                        pieChart.animateXY(1000, 1000);
                        Log.d(TAG, "Successfully removed!");
                        break;
                    }catch(Exception e){
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    }

                }else{
                    Log.e(TAG, "No match for label: " + label + ", error");
                }



        }

        mLoadingDialog.hide();
    }

    /**
     * Highlight entry in piechart by iterating over chart to get index
     *
     * TODO: Improve efficiency and use native PieChart highlight code
     *
     * @param label
     */
    public void highlightByPackageName(String label) {
        Log.d(TAG, "Highlighting entry: " + label);

        DataSet<PieEntry> data = (DataSet<PieEntry>) globalPieData.getDataSet();
        for(int i = 0; i < data.getEntryCount(); i++){
            //TODO There's a race condition that occurs here
            PieEntry pe = data.getEntryForIndex(i);

            if(pe.getLabel().equals(label)){
                Log.d(TAG, "Got a match for label: " + label);
                pieChart.highlightValue(i, 0);
                break;

            }else{
                Log.e(TAG, "No match for label: " + label + ", error");
            }
        }

    }
}
