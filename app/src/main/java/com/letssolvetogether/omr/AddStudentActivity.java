package com.letssolvetogether.omr;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.letssolvetogether.omr.main.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddStudentActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 123;

    private EditText editTextStudentName;
    private DatabaseHelper dbHelper;
    private List<String> studentInfoList; // List to store student name and score
    private ArrayAdapter<String> adapter;
    private Set<String> addedStudents; // Keep track of added students
    private long examId;
    private String examName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        // Retrieve exam ID and exam name from intent extras
        examId = getIntent().getLongExtra("examId", -1);
        examName = getIntent().getStringExtra("examName");
        String className = getIntent().getStringExtra("className"); // Retrieve class name
        String subjectName = getIntent().getStringExtra("subjectName"); // Retrieve subject name

        if (examId == -1 || examName == null || className == null || subjectName == null) {
            // Handle the case when examId, examName, className, or subjectName is not passed correctly
            Toast.makeText(this, "Error: Exam ID, Exam Name, Class Name, or Subject Name not found", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
            return;
        }

        dbHelper = new DatabaseHelper(this);

        // Set the title with the class name, subject name, and exam name
        setTitle(className + " - "+ subjectName + " - " + examName);

        // Display the exam name
        TextView textViewExamName = findViewById(R.id.text_view_exam_name);
        textViewExamName.setText("Exam Name: " + examName);



        studentInfoList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentInfoList);

        addedStudents = new HashSet<>(); // Initialize the set

        loadStudentsFromDatabase();
    }




    private void loadStudentsFromDatabase() {
        String examName = getIntent().getStringExtra("examName");
        if (examName != null) {
            List<String> students = dbHelper.getStudentsForExamByName(examName);
            studentInfoList.clear();
            studentInfoList.addAll(students);
            adapter.notifyDataSetChanged();

            // Add student blocks to the UI
            for (String student : students) {
                if (!addedStudents.contains(student)) {
                    addStudentBlockToUI(student, examId);
                    addedStudents.add(student); // Add the student to the set
                }
            }
        } else {
            Toast.makeText(this, "Error: Exam Name not found", Toast.LENGTH_SHORT).show();
        }
    }




    public void addStudent(View view) {
        String studentName = editTextStudentName.getText().toString().trim();
        if (!studentName.isEmpty()) {
            String examName = getIntent().getStringExtra("examName"); // Retrieve exam name
            if (examName != null) {
                dbHelper.insertStudent(examId, examName, studentName); // Pass exam name along with exam ID and student name
                Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
                editTextStudentName.getText().clear();
                loadStudentsFromDatabase(); // Refresh the list after adding a student
            } else {
                Toast.makeText(this, "Error: Exam Name not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter a student name", Toast.LENGTH_SHORT).show();
        }
    }

    public void exportToExcel(View view) {
        // Check for permission to write to external storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            // Permission is granted, proceed with exporting to Excel

            // Retrieve classroom name, subject name, and exam name
            String className = getIntent().getStringExtra("className");
            String subjectName = getIntent().getStringExtra("subjectName");
            String examName = getIntent().getStringExtra("examName");

            // Check if classroom name, subject name, and exam name are not null
            if (className != null && subjectName != null && examName != null) {
                dbHelper.exportAllScoresToExcel(this, className, subjectName, examName);
            } else {
                // Handle the case when classroom name, subject name, or exam name is not found
                Toast.makeText(this, "Error: Classroom name, subject name, or exam name not found", Toast.LENGTH_SHORT).show();
            }
        }
    }




    private void addStudentBlockToUI(String studentName, long examId) {
        LinearLayout layoutStudents = findViewById(R.id.layout_students); // Assuming this is the parent layout

        // Create a new LinearLayout for the student block
        LinearLayout studentBlockLayout = new LinearLayout(this);
        studentBlockLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        studentBlockLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Create the button for the student's name
        Button btnStudent = new Button(this);
        btnStudent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        btnStudent.setText(studentName);
        studentBlockLayout.addView(btnStudent);

// Assuming dbHelper is an instance of your database helper class

// Assuming studentName and examId are already defined

// Get the score for the student and exam

        List<Integer> score = dbHelper.getScoresForStudent(studentName);

// Create a new TextView to display the score
        TextView txtScore = new TextView(this);
        txtScore.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        txtScore.setText("Score: " + score);

// Add the TextView to the studentBlockLayout (assuming studentBlockLayout is already defined)
        studentBlockLayout.addView(txtScore);


        // Create the delete button
        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        btnDelete.setImageResource(R.drawable.baseline_delete_24);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle delete button click
                AlertDialog.Builder builder = new AlertDialog.Builder(AddStudentActivity.this);
                builder.setMessage("Are you sure you want to delete this student?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Delete the student from the database
                                dbHelper.deleteStudent(examId, studentName);

                                // Remove the student from the list
                                studentInfoList.remove(studentName);
                                adapter.notifyDataSetChanged();

                                // Remove the student block from the layout
                                layoutStudents.removeView(studentBlockLayout);

                                Toast.makeText(AddStudentActivity.this, "Student deleted successfully", Toast.LENGTH_SHORT).show();
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
        studentBlockLayout.addView(btnDelete);

        // Add the student block to the layout
        layoutStudents.addView(studentBlockLayout);
    }


}
