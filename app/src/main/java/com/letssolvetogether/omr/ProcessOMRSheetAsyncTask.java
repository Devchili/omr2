package com.letssolvetogether.omr;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

    private DatabaseHelper databaseHelper;

    public ProcessOMRSheetAsyncTask(CameraView mCameraView, OMRSheet omrSheet, DatabaseHelper databaseHelper) {
        this.omrSheet = omrSheet;
        this.mCameraView = mCameraView;
        this.databaseHelper = databaseHelper;

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

                dialogUnsupporteCameraResolution.setMessage("The Camera resolution " + roiOfOMR.rows() + "x" + matOMR.cols() + " is not supported by OMR Checker.\nPlease take a screenshot and send an email to shreyaspatel29@gmail.com");
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
            textView.setText(" Score: " + score);
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(30);

            // Set OMR Sheet to be displayed in the dialog
            iv.setImageBitmap(omrSheet.getBmpOMRSheet());

            dialogOMRSheetDisplay.setCustomTitle(textView);
            dialogOMRSheetDisplay.setView(linearLayout);
            dialogOMRSheetDisplay.setPositiveButton("Save Score", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Show dialog to select classroom
                    showClassroomSelectionDialog();
                }
            });
            dialogOMRSheetDisplay.setNeutralButton("Go for next OMR", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int sumthin) {
                    mCameraView.requestPreviewFrame();
                }
            });

            dialogOMRSheetDisplay.show();
            Log.i(TAG, "DONE");
        }
    }



    private void showClassroomSelectionDialog() {
        // Fetch classrooms from database
        final List<ClassroomBlock> classrooms = databaseHelper.getAllClassrooms();

        // Show dialog to select classroom
        AlertDialog.Builder builder = new AlertDialog.Builder(mCameraView.getContext());
        builder.setTitle("Select Classroom");
        CharSequence[] items = new CharSequence[classrooms.size()];
        for (int i = 0; i < classrooms.size(); i++) {
            items[i] = classrooms.get(i).getName();
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final ClassroomBlock selectedClassroom = classrooms.get(which);
                // Show dialog to select student
                showStudentSelectionDialog(selectedClassroom);
            }
        });
        builder.show();
    }

    private void showStudentSelectionDialog(final ClassroomBlock classroom) {
        // Fetch students for the selected classroom from database
        final List<String> studentNames = databaseHelper.getStudentsForClassroom(classroom.getId());

        // Show dialog to select student
        AlertDialog.Builder builder = new AlertDialog.Builder(mCameraView.getContext());
        builder.setTitle("Select Student");
        CharSequence[] items = new CharSequence[studentNames.size()];
        for (int i = 0; i < studentNames.size(); i++) {
            items[i] = studentNames.get(i);
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedStudent = studentNames.get(which);
                // Save score for the selected student
                saveScoreForStudent(classroom.getName(), selectedStudent, score);
            }
        });
        builder.show();
    }

    private void saveScoreForStudent(String classroomName, String studentName, int score) {
        // Save score for the student in the specified classroom
        databaseHelper.saveScore(classroomName, studentName, score);

        // Display a toast message confirming the successful saving of the score
        String message = "Score saved for student: " + studentName + " in classroom: " + classroomName;
        Toast.makeText(mCameraView.getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
