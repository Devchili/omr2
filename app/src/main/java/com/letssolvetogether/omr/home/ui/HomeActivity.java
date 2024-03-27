package com.letssolvetogether.omr.home.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.instacart.library.truetime.TrueTime;
import com.letssolvetogether.omr.ResultActivity;
import com.letssolvetogether.omr.main.R;
import com.letssolvetogether.omr.truetime.InitTrueTimeAsyncTask;
import com.letssolvetogether.omr.omrkey.ui.OMRKeyActivity;
import com.letssolvetogether.omr.settings.SettingsActivity;
import com.letssolvetogether.omr.camera.ui.CameraActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button scanOMRButton = findViewById(R.id.button_scan);

        // Set OnClickListener for Scan OMR Button
        scanOMRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open CameraActivity when Scan OMR Button is clicked
                Intent cameraIntent = new Intent(HomeActivity.this, CameraActivity.class);
                cameraIntent.putExtra("noOfQuestions", 20);
                startActivity(cameraIntent);
            }
        });

        Button nextActivityButton = findViewById(R.id.button_next_activity);

        // Set OnClickListener to the button
        nextActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Define intent to open NextActivity
                Intent intent = new Intent(HomeActivity.this, ResultActivity.class);
                startActivity(intent);
            }
        });

        displayValidityPeriodDialog();
    }

    public void displayAnswerKey(View view) {
        Intent omrKeyActivity = new Intent(this, OMRKeyActivity.class);
        omrKeyActivity.putExtra("noOfQuestions", 20);
        startActivity(omrKeyActivity);
    }

    public void displayValidityPeriodDialog() {
        boolean firstrun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstrun", true);
        if (firstrun) {
            // Save the state
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstrun", false)
                    .apply();

            // Display Dialog
            AlertDialog.Builder dialogTips = new AlertDialog.Builder(HomeActivity.this);
            dialogTips.setTitle("Note:");
            dialogTips.setMessage("Happy Scanning!");
            dialogTips.setNeutralButton("Ok", null);
            dialogTips.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    if (!TrueTime.isInitialized()) {
                        new InitTrueTimeAsyncTask(HomeActivity.this).execute();
                    }
                }
            });
            dialogTips.show();
        } else {
            if (!TrueTime.isInitialized()) {
                new InitTrueTimeAsyncTask(HomeActivity.this).execute();
            }
        }
    }
}
