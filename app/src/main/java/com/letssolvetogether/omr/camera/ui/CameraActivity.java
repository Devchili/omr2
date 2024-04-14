package com.letssolvetogether.omr.camera.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.room.Room;

import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;
import com.letssolvetogether.omr.DatabaseHelper;
import com.letssolvetogether.omr.ProcessOMRSheetAsyncTask;
import com.letssolvetogether.omr.main.R;
import com.letssolvetogether.omr.object.OMRSheet;
import com.letssolvetogether.omr.object.OMRSheetViewModelFactory;
import com.letssolvetogether.omr.omrkey.db.AppDatabase;
import com.letssolvetogether.omr.omrkey.db.OMRKey;
import com.letssolvetogether.omr.utils.AnswersUtils;
import com.letssolvetogether.omr.utils.PrereqChecks;

import org.opencv.android.OpenCVLoader;

import java.util.Objects;

public class CameraActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String TAG = "CameraActivity";

    private static int blurImagesCount;
    private static int lowBrightnessImagesCount;

    private static final int UI_ANIMATION_DELAY = 300;

    private final Handler mHideHandler = new Handler();
    private CameraView mCameraView;
    private OMRSheet omrSheet;
    private int noOfQuestions;
    private View mControlsView;
    private boolean mVisible;
    private Handler mBackgroundHandler;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initializeViews();
        noOfQuestions = Objects.requireNonNull(getIntent().getExtras()).getInt("noOfQuestions");
        loadCorrectAnswers();
        mVisible = true;

        Button btnDetect = findViewById(R.id.btnDetect);
        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform OMR sheet detection here
                detectOMRSheet();
            }
        });
    }

    private void initializeViews() {
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mCameraView = findViewById(R.id.fullscreen_content);
        mCameraView.setAspectRatio(AspectRatio.of(4, 3));
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
        mCameraView.setOnClickListener(view -> toggle());
    }

    private void loadCorrectAnswers() {
        omrSheet = ViewModelProviders.of(this, new OMRSheetViewModelFactory(20, 0, 0)).get(OMRSheet.class);
        final AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "omr").build();
        new AsyncTask<Void, Void, Void>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    if (db.omrKeyDao().findById(noOfQuestions) != null) {
                        OMRKey omrKey = db.omrKeyDao().findById(noOfQuestions);
                        String strCorrectAnswers = omrKey.getStrCorrectAnswers();
                        if (strCorrectAnswers != null && !strCorrectAnswers.isEmpty()) {
                            int[] answers = AnswersUtils.strtointAnswers(strCorrectAnswers);
                            omrSheet.setNumberOfQuestions(noOfQuestions);
                            omrSheet.setCorrectAnswers(answers);
                        } else {
                            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "No answers", Toast.LENGTH_LONG).show());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading correct answers: " + e.getMessage());
                }
                return null;
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
            displayTips();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            requestCameraPermission();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        hide();
    }

    private void requestCameraPermission() {
        ConfirmationDialogFragment.newInstance(R.string.camera_permission_confirmation,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION,
                        R.string.camera_permission_not_granted)
                .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.stop();
    }

    private void displayTips() {
        final CheckBox cbDoNotShowAgain;
        final String PREFS_NAME = "INFO_TIPS";
        AlertDialog.Builder dialogTips = new AlertDialog.Builder(this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View doNotShowLayout = adbInflater.inflate(R.layout.checkbox, null);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String skipMessage = settings.getString("skipMessage", "NOT checked");

        cbDoNotShowAgain = doNotShowLayout.findViewById(R.id.skip);
        dialogTips.setView(doNotShowLayout);
        dialogTips.setTitle("Tips:");
        String tipsMsg = "Put an OMR Sheet on flat surface.<br><br>Please make sure the light on an OMR Sheet is proper (not too bright, not too low)<br><br>And there is no shadow.<br><br><p style=\"text-align:center;\">Happy Scanning :)";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            dialogTips.setMessage(Html.fromHtml(tipsMsg, Html.FROM_HTML_MODE_LEGACY));
        } else {
            dialogTips.setMessage(Html.fromHtml(tipsMsg));
        }

        dialogTips.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String checkBoxResult = "NOT checked";
                if (cbDoNotShowAgain.isChecked()) {
                    checkBoxResult = "checked";
                }
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("skipMessage", checkBoxResult);
                editor.apply();
                return;
            }
        });

        assert skipMessage != null;
        if (!skipMessage.equals("checked")) {
            dialogTips.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            mBackgroundHandler.getLooper().quitSafely();
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (permissions.length != 1 || grantResults.length != 1) {
                throw new RuntimeException("Error on requesting camera permission.");
            }
            // No need to start camera here; it is handled by onResume
        }
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;
        mHideHandler.removeCallbacks(mShowPart2Runnable);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        mCameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide() {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, AUTO_HIDE_DELAY_MILLIS);
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private final CameraView.Callback mCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), R.string.picture_taken, Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(() -> {
            });
        }

        @Override
        public void onPreviewReady() {
            // Method not used in button-triggered detection
        }
    };

    // Method to perform OMR sheet detection
// Method to perform OMR sheet detection
    private void detectOMRSheet() {
        // Create an instance of DatabaseHelper
        DatabaseHelper databaseHelper = new DatabaseHelper(this); // Assuming 'this' is the context

        // Pass the DatabaseHelper object to the constructor of ProcessOMRSheetAsyncTask
        ProcessOMRSheetAsyncTask processOMRSheetAsyncTask = new ProcessOMRSheetAsyncTask(mCameraView, omrSheet, databaseHelper);
        processOMRSheetAsyncTask.execute();
    }


    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                                                             String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            assert args != null;
            return new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> {
                                String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                if (permissions == null) {
                                    throw new IllegalArgumentException();
                                }
                                ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                                        permissions, args.getInt(ARG_REQUEST_CODE));
                            })
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> Toast.makeText(getActivity(),
                                    args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                    Toast.LENGTH_SHORT).show())
                    .create();
        }
    }
}
