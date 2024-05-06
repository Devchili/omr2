package com.letssolvetogether.omr;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;

import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.List;

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
    private String examName;

    private DatabaseHelper dbHelper;
    private long examId;

    public ProcessOMRSheetAsyncTask(CameraView mCameraView, OMRSheet omrSheet, String examName, long examId, DatabaseHelper dbHelper) {
        this.omrSheet = omrSheet;
        this.mCameraView = mCameraView;
        this.examName = examName;
        this.dbHelper = dbHelper;
        this.examId = examId;

        if (this.dbHelper == null) {
            // Initialize dbHelper if it is null
            this.dbHelper = new DatabaseHelper(mCameraView.getContext()); // Assuming DatabaseHelper constructor requires a Context parameter
        }


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

        boolean isBlurry = prereqChecks.isBlurry(bmpOMRSheet);
        if (isBlurry) {
            mCameraView.requestPreviewFrame();
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
        if (omrSheetCorners == null) {
            mCameraView.requestPreviewFrame();
        } else {
            omrSheet.setMatOMRSheet(matOMR);
            omrSheet.setWidth(matOMR.cols());
            omrSheet.setHeight(matOMR.rows());
            omrSheet.setOmrSheetBlock();
            omrSheet.setOmrSheetCorners(omrSheetCorners);

            Mat roiOfOMR = detectionUtil.findROIofOMR(omrSheet);
            if (matOMR == null) {
                mCameraView.requestPreviewFrame();
            }
            omrSheet.setMatOMRSheet(roiOfOMR);

            Bitmap bmp = Bitmap.createBitmap(roiOfOMR.cols(), roiOfOMR.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(roiOfOMR, bmp);
            omrSheet.setBmpOMRSheet(bmp);

            try {
                studentAnswers = detectionUtil.getStudentAnswers(roiOfOMR);
            } catch (UnsupportedCameraResolutionException e) {
                AlertDialog.Builder dialogUnsupporteCameraResolution = new AlertDialog.Builder(mCameraView.getContext());

                dialogUnsupporteCameraResolution.setMessage("The Camera resolution " + roiOfOMR.rows() + "x" + matOMR.cols() + " is not supported by OMR Checker.\nPlease take screenshot and send a mail to shreyaspatel29@gmail.com");
                dialogUnsupporteCameraResolution.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialogUnsupporteCameraResolution.show();
                return;
            }

            score = new EvaluationUtil(omrSheet).calculateScore(studentAnswers, omrSheet.getCorrectAnswers());

            new DrawingUtil(omrSheet).drawRectangle(studentAnswers);

            final AlertDialog.Builder dialogOMRSheetDisplay = new AlertDialog.Builder(mCameraView.getContext());

            // Set custom title to dialog box
            TextView textView = new TextView(mCameraView.getContext());
            textView.setText("Exam: " + examName + "\nScore: " + score);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(30);

            // Set OMR Sheet to be displayed in dialog
            iv.setImageBitmap(omrSheet.getBmpOMRSheet());

            dialogOMRSheetDisplay.setCustomTitle(textView);
            dialogOMRSheetDisplay.setView(linearLayout);

            dialogOMRSheetDisplay.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Check if dbHelper is not null
                    if (dbHelper != null) {
                        // Prompt user to input student's name
                        AlertDialog.Builder builder = new AlertDialog.Builder(mCameraView.getContext());
                        builder.setTitle("Enter Student's Name");

                        // Set up the input
                        final EditText input = new EditText(mCameraView.getContext());
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);

                        // Set up the buttons
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String studentName = input.getText().toString();

                                // Store the score for the specified student and exam
                                dbHelper.saveScore(examName, studentName, score);

                                // Show a toast indicating the score has been saved for the selected student
                                Toast.makeText(mCameraView.getContext(), "Score saved for " + studentName, Toast.LENGTH_SHORT).show();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                    } else {
                        // Handle the case when dbHelper is null
                        // Display a toast message indicating that dbHelper is not initialized
                        Toast.makeText(mCameraView.getContext(), "Error: Database helper not initialized", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialogOMRSheetDisplay.setNeutralButton("Next", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int sumthin) {
                }
            });

            dialogOMRSheetDisplay.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    mCameraView.requestPreviewFrame();
                }
            });

            dialogOMRSheetDisplay.show();
            Log.i(TAG, "DONE");
        }
    }

}
