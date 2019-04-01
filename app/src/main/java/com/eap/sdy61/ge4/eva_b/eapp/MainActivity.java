package com.eap.sdy61.ge4.eva_b.eapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private Toolbar mToolbar;

    // Buttons
    Button internetBtn;
    Button openStorageBtn;
    Button saveStorageBtn;
    EditText fileText;

    // Google Drive
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;

    Boolean driverReady;

    // Opened file

    Task<DriveContents> mOpenTaskRead;
    Task<DriveContents> mOpenTaskUpdate;

    Context mContext;

    Fragment ThreeFragment;

    @Override
    protected void onDriveClientReady() {
        driverReady = true;


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(mToolbar);

        ThreeFragment = new ThreeFragment();

        mContext = this;

        // Get the buttons by the view
        internetBtn = findViewById(R.id.btnInternet);
        openStorageBtn = findViewById(R.id.btnOpenStorage);
        saveStorageBtn = findViewById(R.id.btnSaveStorage);

        // Get the text area

        fileText = findViewById(R.id.fileText);


        internetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOnline()) {
                    Uri uri = Uri.parse("http://www.eap.gr");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Αποτυχία εμφάνισης της σελίδας, παρακολούμε συνδεθείτε στο internet.", Toast.LENGTH_LONG).show();
                }
            }
        });

        openStorageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (driverReady) {
                    pickTextFile()
                            .addOnSuccessListener(MainActivity.this,
                                    driveId -> openDriveFile(driveId.asDriveFile()))
                            .addOnFailureListener(MainActivity.this, e -> {
                                Log.e(TAG, "No file selected", e);
                                showMessage(getString(R.string.file_not_selected));
////                    finish();
                            });
                }
            }
        });

        saveStorageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String text = fileText.getText().toString();
                if (driverReady) {
                        if (mOpenTaskRead != null && mOpenTaskUpdate != null) {
                            if (TextUtils.isEmpty(text) || text.trim().isEmpty()) {
                                Toast.makeText(MainActivity.this, "Δεν μπορείτε να αποθηκεύσετε αρχείο με κενό περιεχόμενο, παρκαλούμε πληκτρολογήστε κείμενο.", Toast.LENGTH_LONG).show();
                            } else {
                                updateDriveFile(mOpenTaskUpdate, text);
                            }
                        } else {
                            if (TextUtils.isEmpty(text) || text.trim().isEmpty()) {
                                Toast.makeText(MainActivity.this, "Δεν μπορείτε να δημιουργήσετε αρχείο με κενό περιεχόμενο, παρκαλούμε πληκτρολογήστε κείμενο.", Toast.LENGTH_LONG).show();
                            } else {
                                createFile(text);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_toTabs) {
            Intent intent = new Intent(MainActivity.this, TabsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }


    private void createFile(String text) {
        // [START drive_android_create_file]
        final Task<DriveFolder> rootFolderTask = getDriveResourceClient().getRootFolder();
        final Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(task -> {
                    DriveFolder parent = rootFolderTask.getResult();
                    DriveContents contents = createContentsTask.getResult();
                    OutputStream outputStream = contents.getOutputStream();
                    try (Writer writer = new OutputStreamWriter(outputStream)) {
                        writer.write(text);
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("eapp-file-" + ts + ".txt")
                            .setMimeType("text/plain")
                            .setStarred(true)
                            .build();

                    return getDriveResourceClient().createFile(parent, changeSet, contents);
                })
                .addOnSuccessListener(this,
                        driveFile -> {
                            showMessage(mContext.getString(R.string.file_created));
                            fileText.getText().clear();
//                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to create file", e);
                    showMessage(getString(R.string.file_create_error));
//                    finish();
                });
        // [END drive_android_create_file]
    }

    private void openDriveFile(DriveFile file) {
        // [START drive_android_open_for_append]
        mOpenTaskRead =
                getDriveResourceClient().openFile(file, DriveFile.MODE_READ_ONLY);
        mOpenTaskUpdate =
                getDriveResourceClient().openFile(file, DriveFile.MODE_WRITE_ONLY);

        // [END drive_android_open_for_append]

        // [START drive_android_read_contents]
        mOpenTaskRead.continueWithTask(task -> {
            DriveContents contents = task.getResult();
            // Process contents...
            // [START_EXCLUDE]
            // [START drive_android_read_as_string]
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(contents.getInputStream()))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                showMessage(getString(R.string.content_loaded));
                fileText.setText(builder.toString());
                // [START drive_android_discard_contents]
                Task<Void> discardTask = getDriveResourceClient().discardContents(contents);
                // [END drive_android_discard_contents]
                return discardTask;
            }
        })
                .addOnFailureListener(e -> {
                    // Handle failure
                    // [START_EXCLUDE]
                    Log.e(TAG, "Unable to read contents", e);
                    showMessage(getString(R.string.read_failed));
//                    finish();
                    // [END_EXCLUDE]
                });
        // [END drive_android_read_as_string]
    }

    private void updateDriveFile(Task<DriveContents> openTask, String text) {
        // [START drive_android_rewrite_contents]
        mOpenTaskUpdate.continueWithTask(task -> {
            DriveContents driveContents = task.getResult();
            try (OutputStream out = driveContents.getOutputStream()) {
                out.write(text.getBytes());
            }
            // [START drive_android_commit_content]
            Task<Void> commitTask =
                    getDriveResourceClient().commitContents(driveContents, null);
            // [END drive_android_commit_content]
            return commitTask;
        })
                .addOnSuccessListener(this,
                        aVoid -> {
                            showMessage(getString(R.string.file_updated));
                            mOpenTaskUpdate = null;
                            mOpenTaskRead = null;
                            fileText.getText().clear();
//                            finish();
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to update contents", e);
                    showMessage(getString(R.string.file_update_failed));
                    mOpenTaskUpdate = null;
                    mOpenTaskRead = null;
//                    finish();
                });
        // [END drive_android_rewrite_contents]
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if(ThreeFragment instanceof IOnFocusListenable) {
//            ((IOnFocusListenable) ThreeFragment).onWindowFocusChanged(hasFocus);
//        }
//    }

}
