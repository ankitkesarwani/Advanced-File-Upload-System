package com.example.kesar.fileuploadfirebase;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button mSelectBtn;
    private Button mPauseBtn;
    private Button mCancelBtn;

    private final static int FILE_SELECT_CODE = 1;
    private int FILE_IS_SELECTED = 0;

    private StorageReference mStorageRef;

    private TextView mFilenameLabel;

    private ProgressBar mProgress;

    private TextView mProgressLabel;
    private TextView mSizeLabel;

    private StorageTask mStorageTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSelectBtn = (Button) findViewById(R.id.select_btn);
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mCancelBtn = (Button) findViewById(R.id.cancel_btn);

        mFilenameLabel = (TextView) findViewById(R.id.filename_label);

        mProgress = (ProgressBar) findViewById(R.id.upload_progress);
        mSizeLabel = (TextView) findViewById(R.id.size_label);
        mProgressLabel = (TextView) findViewById(R.id.progress_label);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        mSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                openFileSelector();

            }
        });

        mPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (FILE_IS_SELECTED == 1) {

                    String btnText = mPauseBtn.getText().toString();
                    if (btnText.equals("Pause Upload")) {

                        mStorageTask.pause();
                        mPauseBtn.setText("Resume Upload");

                    } else {

                        mStorageTask.resume();
                        mPauseBtn.setText("Pause Upload");

                    }
                } else {

                    Toast.makeText(MainActivity.this, "Please Select a file to upload", Toast.LENGTH_LONG).show();

                }
            }
        });

        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (FILE_IS_SELECTED == 1) {

                    mStorageTask.cancel();

                } else {

                    Toast.makeText(MainActivity.this, "Please Select a file to upload", Toast.LENGTH_LONG).show();

                }

            }
        });

    }

    private void openFileSelector() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {

            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);

        } catch (android.content.ActivityNotFoundException ex) {

            Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_LONG).show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {

            FILE_IS_SELECTED = 1;
            Uri fileUri = data.getData();


            String uriString = fileUri.toString();

            File myFile = new File(uriString);
            String path = myFile.getAbsolutePath();

            String displayName = null;

            if(uriString.startsWith("content://")) {

                Cursor cursor = null;

                try {

                    cursor = MainActivity.this.getContentResolver().query(fileUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {

                        displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }

                } finally {

                    cursor.close();

                }
            } else if(uriString.startsWith("file://")) {

                displayName = myFile.getName();

            }

            mFilenameLabel.setText(displayName);

            StorageReference riversRef = mStorageRef.child("files/" + displayName);

            mStorageTask = riversRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Get a URL to the uploaded content
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();

                            Toast.makeText(MainActivity.this, "File Uploaded", Toast.LENGTH_LONG).show();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            // ...
                            Toast.makeText(MainActivity.this, "There was an Error...", Toast.LENGTH_LONG).show();

                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            mProgress.setProgress((int) progress);

                            String progresstext = taskSnapshot.getBytesTransferred()/(1024 * 1024) + " / " + taskSnapshot.getTotalByteCount()/(1024 * 1024) + " mb";

                            mSizeLabel.setText(progresstext);

                        }
                    });
        }

        super.onActivityResult(requestCode, resultCode, data);

    }
}
