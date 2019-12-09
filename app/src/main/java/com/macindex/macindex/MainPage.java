package com.macindex.macindex;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.ResultSet;

/**
 * MacIndex Android application and Specs database
 * University of Illinois, CS125 FA19 Final Project
 *
 * For additional Database Design Information, please refer to:
 * https://github.com/paizhangpi/MacIndex/
 */
public class MainPage extends AppCompatActivity {

    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDatabase();
        initInterface();
    }

    private void initDatabase() {
        String appDataPath = this.getApplicationInfo().dataDir;
        File dbFolder = new File(appDataPath + "/databases");
        dbFolder.mkdir();
        File dbFilePath = new File(appDataPath + "/databases/specs.db");
        try {
            InputStream inputStream = this.getAssets().open("specs.db");
            OutputStream outputStream = new FileOutputStream(dbFilePath);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0)
            {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Sorry, database initialization error occurred.\n\n" +
                    "For additional information, please refer to GitHub readme.")
                    .setNegativeButton("QUIT", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            finish();
                        }}).setTitle("Error").setTitle("Fatal Error").setCancelable(false).show();
        }
        DatabaseOpenHelper dbHelper = new DatabaseOpenHelper(this);
        database = dbHelper.getReadableDatabase();
    }

    /**
     * If new category, or data category was added, database structure was changed.
     * Thus, code update is necessary to handle new data.
     * Also, exceptions may happen even after you update the code during runtime. Please refer to:
     * https://github.com/paizhangpi/MacIndex/
     *
     * Not necessary if only new machine was added.
     */
    private void initInterface() {
        // Change the number below.
        for (int i = 0; i <= 9; i++) {
            LinearLayout currentLayout = findViewById(CategoryHelper.getLayout(i));
            initCategory(currentLayout, i);
        }
        // Basic functionality was finished on 16:12 CST, Dec 2, 2019.
    }

    private void initCategory(final LinearLayout currentLayout, final int category) {
        try {
            Cursor cursor = database.query("category" + String.valueOf(category), null,
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                View mainChunk = getLayoutInflater().inflate(R.layout.chunk_main, null);
                TextView machineName = mainChunk.findViewById(R.id.machineName);
                Button viewButton = mainChunk.findViewById(R.id.viewButton);
                // Create a String for each data category. Update here.
                final String thisName = cursor.getString(cursor.getColumnIndex("name"));
                final String thisProcessor = cursor.getString(cursor.getColumnIndex("processor"));
                final String thisMaxRAM = cursor.getString(cursor.getColumnIndex("maxram"));
                final String thisYear = cursor.getString(cursor.getColumnIndex("year"));
                final String thisModel = cursor.getString(cursor.getColumnIndex("model"));
                final byte[] thisBlob = cursor.getBlob(cursor.getColumnIndex("pic"));

                machineName.setText(thisName);
                viewButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View unused) {
                        Intent intent = new Intent(MainPage.this, SpecsActivity.class);
                        // Put each String to Specs Intent. Update here.
                        intent.putExtra("name", thisName);
                        intent.putExtra("processor", thisProcessor);
                        intent.putExtra("maxram", thisMaxRAM);
                        intent.putExtra("year", thisYear);
                        intent.putExtra("model", thisModel);

                        String path = null;

                        if (thisBlob != null) {
                            Bitmap pic = BitmapFactory.decodeByteArray(thisBlob, 0, thisBlob.length);
                            Log.i("h", "Converted blob to bitmap");
                            try {
                                File file = File.createTempFile("tempF", ".tmp");
                                try (FileOutputStream out = new FileOutputStream(file, false)) {
                                    pic.compress(Bitmap.CompressFormat.PNG, 100, out); //
                                    // bmp is your Bitmap instance
                                    // PNG is a lossless format, the compression factor (100) is ignored
                                    Log.i("h", "Created image format");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                path = file.getPath();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        intent.putExtra("path", path);

                        startActivity(intent);
                    }
                });
                currentLayout.addView(mainChunk);
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this).setMessage("Sorry, database query error occurred.\n\n" +
                    "For additional information, please refer to GitHub readme.")
                    .setPositiveButton("DISMISS", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            dialog.dismiss();
                        }}).setNegativeButton("QUIT", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    finish();
                }}).setTitle("Error").setCancelable(false).show();
        }
    }
}
