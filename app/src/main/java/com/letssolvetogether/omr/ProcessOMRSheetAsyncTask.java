package com.letssolvetogether.omr;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.cameraview.CameraView;
import com.letssolvetogether.omr.detection.DetectionUtil;
import com.letssolvetogether.omr.drawing.DrawingUtil;
import com.letssolvetogether.omr.evaluation.EvaluationUtil;
import com.letssolvetogether.omr.exceptions.UnsupportedCameraResolutionException;
import com.letssolvetogether.omr.object.OMRSheet;
import com.letssolvetogether.omr.object.OMRSheetCorners;
import com.letssolvetogether.omr.utils.PrereqChecks;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class ProcessOMRSheetAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private static String TAG = "ProcessOMRSheetAsyncTask";

    private OMRSheet omrSheet;
    private OMRSheetCorners omrSheetCorners;
    private Bitmap bmpOMRSheet;
    private Mat matOMR;
    private CameraView mCameraView;
    private LinearLayout linearLayout;
    private ImageView iv;
    private DetectionUtil detectionUtil;
    private PrereqChecks prereqChecks;
    private byte[][] studentAnswers;
    private int score;

    private boolean isDetectionPaused = false; // Flag to track if detection is paused
    private boolean isDialogOpen = false; // Flag to track if a dialog box is open

    public ProcessOMRSheetAsyncTask(CameraView mCameraView, OMRSheet omrSheet) {
        this.omrSheet = omrSheet;
        this.mCameraView = mCameraView;

        detectionUtil = new DetectionUtil(omrSheet);
        prereqChecks = new PrereqChecks();

        linearLayout = new LinearLayout(mCameraView.getContext());
        iv = new ImageView(mCameraView.getContext());
        iv.setAdjustViewBounds(true);
        linearLayout.addView(iv);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        Log.i(TAG, "doInBackground");

        bmpOMRSheet = this.mCameraView.getPreviewFrame();

        if (isDetectionPaused || isDialogOpen) { // Pause detection if a dialog is open or detection is paused
            return false;
        }

        boolean isBlurry = prereqChecks.isBlurry(bmpOMRSheet);
        if (isBlurry) {
            return false;
        }

        matOMR = new Mat();
        Utils.bitmapToMat(bmpOMRSheet, matOMR);
        omrSheetCorners = detectionUtil.detectOMRSheetCorners(matOMR);
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        Log.i(TAG, "onPostExecute");
        if (!result || omrSheetCorners == null || isDialogOpen) { // Pause detection if result is false, corners are null, or a dialog is open
            mCameraView.requestPreviewFrame(); // Resume scanning
            return;
        } else {
            omrSheet.setMatOMRSheet(matOMR);
            omrSheet.setWidth(matOMR.cols());
            omrSheet.setHeight(matOMR.rows());
            omrSheet.setOmrSheetBlock();
            omrSheet.setOmrSheetCorners(omrSheetCorners);

            Mat roiOfOMR = detectionUtil.findROIofOMR(omrSheet);
            if (matOMR == null) {
                mCameraView.requestPreviewFrame(); // Resume scanning
                return;
            }
            omrSheet.setMatOMRSheet(roiOfOMR);

            Bitmap bmp = Bitmap.createBitmap(roiOfOMR.cols(), roiOfOMR.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(roiOfOMR, bmp);
            omrSheet.setBmpOMRSheet(bmp);

            try {
                studentAnswers = detectionUtil.getStudentAnswers(roiOfOMR);
            } catch (UnsupportedCameraResolutionException e) {
                AlertDialog.Builder dialogUnsupportedCameraResolution = new AlertDialog.Builder(mCameraView.getContext());

                dialogUnsupportedCameraResolution.setMessage("The Camera resolution " + roiOfOMR.rows() + "x" + matOMR.cols() + " is not supported by OMR Checker.\nPlease take a screenshot and send an email to shreyaspatel29@gmail.com");
                dialogUnsupportedCameraResolution.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialogUnsupportedCameraResolution.show();
                return;
            }

            score = new EvaluationUtil(omrSheet).calculateScore(studentAnswers, omrSheet.getCorrectAnswers());

            new DrawingUtil(omrSheet).drawRectangle(studentAnswers);

            final AlertDialog.Builder dialogOMRSheetDisplay = new AlertDialog.Builder(mCameraView.getContext());

            // Set custom title to dialog box
            TextView textView = new TextView(mCameraView.getContext());
            textView.setText(" Score: " + score);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(30);

            // Set OMR Sheet to be displayed in dialog
            iv.setImageBitmap(omrSheet.getBmpOMRSheet());

            dialogOMRSheetDisplay.setCustomTitle(textView);
            dialogOMRSheetDisplay.setView(linearLayout);
            dialogOMRSheetDisplay.setNeutralButton("Go for next OMR", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int sumthin) {
                    mCameraView.requestPreviewFrame(); // Resume scanning
                }
            });

            // Initialize SharedPreferences
            final SharedPreferences sharedPreferences = mCameraView.getContext().getSharedPreferences("OMR_PREFERENCES", Context.MODE_PRIVATE);

            // Set Save button and its onClickListener
            dialogOMRSheetDisplay.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Pause detection when "Save" is clicked
                    isDialogOpen = true;
                    isDetectionPaused = true;

                    // Show the quiz selection dialog first
                    final CharSequence[] items = {"Quiz 1", "Quiz 2", "Quiz 3", "Quiz 4", "Quiz 5"};
                    AlertDialog.Builder quizSelectionDialog = new AlertDialog.Builder(mCameraView.getContext());
                    quizSelectionDialog.setTitle("Select Quiz");
                    quizSelectionDialog.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Get selected quiz
                            final String selectedQuiz = "Quiz " + (which + 1);

                            // Build a dialog to get student name
                            final AlertDialog.Builder studentNameDialog = new AlertDialog.Builder(mCameraView.getContext());
                            final EditText input = new EditText(mCameraView.getContext());
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            studentNameDialog.setView(input);
                            studentNameDialog.setTitle("Enter Student Name");

                            // Set OK button
                            studentNameDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String studentName = input.getText().toString();
                                    String scoreRecord = selectedQuiz + " - " + studentName + " : " + score;
                                    // Retrieve existing data from SharedPreferences
                                    String existingData = sharedPreferences.getString("StudentScores", "");
                                    // Append the new student name and score
                                    String newData = existingData + ";" + scoreRecord;
                                    // Save the updated data back to SharedPreferences
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("StudentScores", newData);
                                    editor.apply();
                                    Toast.makeText(mCameraView.getContext(), "Student name and score saved!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    isDialogOpen = false; // Resume detection
                                    isDetectionPaused = false; // Resume detection
                                    mCameraView.requestPreviewFrame(); // Resume scanning
                                }
                            });

                            // Set Cancel button
                            studentNameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    isDialogOpen = false; // Resume detection
                                    isDetectionPaused = false; // Resume detection
                                    mCameraView.requestPreviewFrame(); // Resume scanning
                                }
                            });

                            // Show the student name input dialog
                            studentNameDialog.show();
                        }
                    });
                    // Show the quiz selection dialog
                    quizSelectionDialog.show();
                }
            });

            dialogOMRSheetDisplay.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    isDialogOpen = false; // Resume detection
                    isDetectionPaused = false; // Resume detection
                    mCameraView.requestPreviewFrame(); // Resume scanning
                }
            });

            dialogOMRSheetDisplay.show();
            Log.i(TAG, "DONE");
        }
    }
}
