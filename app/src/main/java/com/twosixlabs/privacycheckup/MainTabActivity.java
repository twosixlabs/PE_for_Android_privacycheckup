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

package com.twosixlabs.privacycheckup;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.twosixlabs.privacycheckup.db.DatabaseClient;
import com.twosixlabs.privacycheckup.db.requests.DangerousPermissionRequest;
import com.twosixlabs.privacycheckup.ui.applications.ApplicationsFragment;
import com.twosixlabs.privacycheckup.ui.main.SectionsPagerAdapter;
import com.twosixlabs.privacycheckup.ui.requests.RequestsFragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainTabActivity extends AppCompatActivity
        implements ApplicationsFragment.OnFragmentInteractionListener, RequestsFragment.OnFragmentInteractionListener {

    private static final String TAG = MainTabActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tab);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = findViewById(R.id.fab);

        // create menu bar
        Toolbar myToolbar = findViewById(R.id.toolbar3);
        myToolbar.setOverflowIcon(getDrawable(R.drawable.ic_more_vert_white_24dp));
        setSupportActionBar(myToolbar);
        Window window = getWindow();


        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tab_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reload_data:
                //TODO reload only data and relevant parts of page, not entire activity
                finish();
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);
                return true;
            case R.id.action_manage_policy:
                Intent intent = new Intent(this, PolicyActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_export_db:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                new MainTabActivity.ExportDbTask(getApplicationContext()).execute();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The database will be exported to your device's storage. \n\nIf this application has not already been given permission to access external storage, a prompt will appear following this screen.").setPositiveButton("Okay", dialogClickListener)
                        .setNegativeButton("Cancel", dialogClickListener).show();
                return true;
            case R.id.action_reset_stats:
                new AlertDialog.Builder(MainTabActivity.this)
                        .setTitle("Reset Statistics")
                        .setMessage("Are you sure you want reset statistics? This action can not be undone.\n\nTo first export the data, tap 'Cancel' and choose 'Export Database.'")
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //do nothing, let it proceed
                                Toast.makeText(MainTabActivity.this, "Clearing tables...", Toast.LENGTH_LONG).show();
                                new MainTabActivity.ResetDbTask(getApplicationContext()).execute();
                            }

                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MainTabActivity.this, "Cancelled!", Toast.LENGTH_SHORT).show();
                            }

                        }).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class ResetDbTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> contextRef;

        ResetDbTask(Context context) {
            contextRef = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... voids) {
            DatabaseClient.getInstance(contextRef.get()).getAppDatabase().clearAllTables();
            return null;

        }

        protected void onPostExecute(Void param) {
            Log.e(TAG, "Clear tables succeed");
            finish();
            startActivity(getIntent());
        }

    }


    private class ExportDbTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> contextRef;
        private final ProgressDialog dialog = new ProgressDialog(MainTabActivity.this);

        ExportDbTask(Context context) {
            contextRef = new WeakReference<>(context);
        }

        String fileExportLocation = "";

        @Override
        protected void onPreExecute() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission is granted");
                    Toast.makeText(MainTabActivity.this, "Exporting...", Toast.LENGTH_LONG).show();

                } else {
                    Log.v(TAG, "Permission is revoked");
                    ActivityCompat.requestPermissions(MainTabActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    this.cancel(true);
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                Log.v(TAG, "Permission is granted");
                Toast.makeText(MainTabActivity.this, "Exporting...", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File exportDir = new File(Environment.getExternalStorageDirectory(), "/privacy_checkup_export/");
            Log.d(TAG, "Adding file to " + exportDir.getAbsolutePath());

            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir, new SimpleDateFormat("yyyyMMddHHmm'.csv'").format(new Date()));
            fileExportLocation = file.getAbsolutePath();


            try {
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                String[] columnNames = {
                        "id",
                        "package_name",
                        "permission",
                        "purpose",
                        "stack_trace",
                        "is_privileged",
                        "is_system",
                        "is_background",
                        "calling_component",
                        "top_activity",
                        "allowed",
                        "timestamp"};

                csvWrite.writeNext(columnNames);

                List<DangerousPermissionRequest> dangerousPermissionRequests = DatabaseClient.getInstance(contextRef.get()).getAppDatabase()
                        .dangerousPermissionRequestDao().getAll();

                for (DangerousPermissionRequest req : dangerousPermissionRequests) {
                    String[] contentArray = {
                            String.valueOf(req.id),
                            req.packageName,
                            req.permission,
                            req.purpose,
                            req.stackTrace,
                            String.valueOf(req.isPrivileged),
                            String.valueOf(req.isSystem),
                            String.valueOf(req.isBackground),
                            String.valueOf(req.callingComponent),
                            String.valueOf(req.topActivity),
                            String.valueOf(req.allowed),
                            String.valueOf(req.timestamp)
                    };
                    csvWrite.writeNext(contentArray);
                    Log.d(TAG, req.toString());
                }

                csvWrite.close();

            } catch (IOException e) {
                Log.e(TAG, "CSV export failed: " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(Void param) {
            Log.e(TAG, "CSV export succeeded");
            Toast.makeText(MainTabActivity.this, "Successfully exported CSV to " + fileExportLocation, Toast.LENGTH_LONG).show();

        }
    }

    /**
     * From opencsv
     * <p>
     * http://opencsv.sourceforge.net/apidocs/com/opencsv/CSVWriter.html
     */
    public class CSVWriter {
        private PrintWriter pw;
        private char separator;
        private char escapechar;
        private String lineEnd;
        private char quotechar;
        public static final char DEFAULT_SEPARATOR = ',';
        public static final char NO_QUOTE_CHARACTER = '\u0000';
        public static final char NO_ESCAPE_CHARACTER = '\u0000';
        public static final String DEFAULT_LINE_END = "\n";
        public static final char DEFAULT_QUOTE_CHARACTER = '"';
        public static final char DEFAULT_ESCAPE_CHARACTER = '"';

        public CSVWriter(Writer writer) {
            this(writer, DEFAULT_SEPARATOR, DEFAULT_QUOTE_CHARACTER,
                    DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
        }

        public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
            this.pw = new PrintWriter(writer);
            this.separator = separator;
            this.quotechar = quotechar;
            this.escapechar = escapechar;
            this.lineEnd = lineEnd;
        }

        public void writeNext(String[] nextLine) {
            if (nextLine == null)
                return;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < nextLine.length; i++) {

                if (i != 0) {
                    sb.append(separator);
                }
                String nextElement = nextLine[i];
                if (nextElement == null)
                    continue;
                if (quotechar != NO_QUOTE_CHARACTER)
                    sb.append(quotechar);
                for (int j = 0; j < nextElement.length(); j++) {
                    char nextChar = nextElement.charAt(j);
                    if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
                        sb.append(escapechar).append(nextChar);
                    } else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
                        sb.append(escapechar).append(nextChar);
                    } else {
                        sb.append(nextChar);
                    }
                }
                if (quotechar != NO_QUOTE_CHARACTER)
                    sb.append(quotechar);
            }
            sb.append(lineEnd);
            pw.write(sb.toString());
        }

        public void close() throws IOException {
            pw.flush();
            pw.close();
        }

        public void flush() throws IOException {
            pw.flush();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }
}