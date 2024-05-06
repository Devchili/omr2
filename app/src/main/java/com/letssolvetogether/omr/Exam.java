package com.letssolvetogether.omr;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.letssolvetogether.omr.camera.ui.CameraActivity;
import com.letssolvetogether.omr.main.R;
import com.letssolvetogether.omr.omrkey.ui.OMRKeyActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Exam extends AppCompatActivity {
    private LinearLayout layoutExams;
    private int noOfQuestions;
    private int examId;

    private DatabaseHelper dbHelper;
    private Map<String, View> examViews; // Change key type to String

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        layoutExams = findViewById(R.id.list_view_exams);

        dbHelper = new DatabaseHelper(this);
        examViews = new HashMap<>(); // Initialize examViews

        ImageButton addButton = findViewById(R.id.btn_add_exam);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addExamBlock();
            }
        });

        loadExamsFromDatabase();
    }

    private void loadExamsFromDatabase() {
        long subjectId = getIntent().getLongExtra("subjectId", -1);
        if (subjectId != -1) {
            List<String> exams = dbHelper.getAllExamsSortedByNameAscending(subjectId);
            for (String exam : exams) {
                if (!examViews.containsKey(exam.hashCode())) {
                    addExamBlockToUI(exam, subjectId, examId); // Pass subjectId to addExamBlockToUI
                }
            }
        } else {
            Toast.makeText(Exam.this, "Error: Subject ID not found", Toast.LENGTH_SHORT).show();
        }
    }



    private void addExamBlockToUI(String exam, long subjectId, long examId) {
        // Create button for the exam
        Button btnExam = new Button(this);
        btnExam.setText(exam);
        btnExam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open new activity for the exam
                Intent intent = new Intent(Exam.this, AddStudentActivity.class);
                intent.putExtra("examId", examId);
                intent.putExtra("examName", exam);
                startActivity(intent);
            }
        });

        ImageButton btnOMRKey = new ImageButton(this);
        btnOMRKey.setImageResource(R.drawable.baseline_key_24);
        btnOMRKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open OMRKeyActivity
                Intent omrKeyActivity = new Intent(Exam.this, OMRKeyActivity.class);
                omrKeyActivity.putExtra("noOfQuestions", noOfQuestions);
                startActivity(omrKeyActivity);
            }
        });
        ImageButton btnCam = new ImageButton(this);
        btnCam.setImageResource(R.drawable.baseline_camera_alt_24);
        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Exam.this, CameraActivity.class);
                cameraIntent.putExtra("examName", exam); // Pass the exam name as an extra
                cameraIntent.putExtra("noOfQuestions", noOfQuestions);
                startActivity(cameraIntent);
            }
        });

        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(R.drawable.baseline_delete_24);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Exam.this);
                builder.setMessage("Are you sure you want to delete this exam?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dbHelper.deleteExam(exam);
                                LinearLayout parentLayout = (LinearLayout) view.getParent();
                                layoutExams.removeView(parentLayout);
                                examViews.remove(exam); // Use exam name as key
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog, do nothing
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(btnExam);
        layout.addView(new View(this), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        layout.addView(btnCam);
        layout.addView(btnOMRKey);
        layout.addView(btnDelete);

        layoutExams.addView(layout);
        examViews.put(exam, layout); // Use exam name as key
    }


    public void addExamBlock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Exam");

        // Set up the input for exam name
        final EditText inputName = new EditText(this);
        inputName.setHint("Enter exam name");

        // Get the subject associated with the exam creation
        Intent intent = getIntent();
        final String selectedSubject = intent.getStringExtra("subject");
        final long subjectId = intent.getLongExtra("subjectId", -1); // Retrieve subjectId from intent

        // Display the selected subject
        final EditText selectedSubjectView = new EditText(this);
        selectedSubjectView.setText(selectedSubject);
        selectedSubjectView.setEnabled(false);

        // Set up the spinner for selecting the number of questions
        final Spinner spinnerQuestions = new Spinner(this);
        ArrayAdapter<CharSequence> questionAdapter = ArrayAdapter.createFromResource(this,
                R.array.question_options_array, android.R.layout.simple_spinner_item);
        questionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerQuestions.setAdapter(questionAdapter);

        // Create a LinearLayout to hold the views
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(inputName);
        layout.addView(selectedSubjectView);
        layout.addView(spinnerQuestions);
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String examName = inputName.getText().toString().trim();
                String selectedOption = spinnerQuestions.getSelectedItem().toString();
                noOfQuestions = Integer.parseInt(selectedOption); // Assign the value to the class-level variable

                if (!examName.isEmpty()) {
                    // Check if the exam name already exists
                    if (!dbHelper.hasExamWithName(examName)) {
                        if (subjectId != -1) {
                            dbHelper.insertExam(examName, subjectId); // Use the retrieved subject ID
                            addExamBlockToUI(examName, subjectId, examId); // Pass subjectId to addExamBlockToUI
                        } else {
                            Toast.makeText(Exam.this, "Error: Subject ID not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(Exam.this, "Exam name already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Exam.this, "Please enter an exam name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

}
