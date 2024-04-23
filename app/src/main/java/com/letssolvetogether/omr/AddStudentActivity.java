package com.letssolvetogether.omr;

import android.content.DialogInterface;
import android.os.Bundle;
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

import com.letssolvetogether.omr.main.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddStudentActivity extends AppCompatActivity {
    private EditText editTextStudentName;
    private long classroomId;
    private DatabaseHelper dbHelper;
    private List<String> studentInfoList; // List to store student name and score
    private ArrayAdapter<String> adapter;
    private Set<String> addedStudents; // Keep track of added students

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        editTextStudentName = findViewById(R.id.edit_text_student_name);
        classroomId = getIntent().getLongExtra("classroomId", -1);
        dbHelper = new DatabaseHelper(this);

        studentInfoList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentInfoList);

        addedStudents = new HashSet<>(); // Initialize the set

        loadStudentsFromDatabase();
    }

    private void loadStudentsFromDatabase() {
        List<String> students = dbHelper.getStudentsForClassroomSortedByNameAscending(classroomId);
        studentInfoList.clear();
        studentInfoList.addAll(students);
        adapter.notifyDataSetChanged();

        // Add student blocks to the UI
        for (String student : students) {
            if (!addedStudents.contains(student)) {
                addStudentBlockToUI(student);
                addedStudents.add(student); // Add the student to the set
            }
        }
    }

    public void addStudent(View view) {
        String studentName = editTextStudentName.getText().toString().trim();
        if (!studentName.isEmpty()) {
            dbHelper.insertStudent(classroomId, studentName);
            Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show();
            editTextStudentName.getText().clear();
            loadStudentsFromDatabase(); // Refresh the list after adding a student
        } else {
            Toast.makeText(this, "Please enter a student name", Toast.LENGTH_SHORT).show();
        }
    }

    private void addStudentBlockToUI(String studentName) {
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

        // Create a TextView for the student's score
        TextView txtScore = new TextView(this);
        txtScore.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        int score = dbHelper.getScoreForStudent(studentName, classroomId);
        if (score != -1) {
            txtScore.setText("Score: " + score);
        } else {
            txtScore.setText("Score: N/A");
        }
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
                                dbHelper.deleteStudent(classroomId, studentName);

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
