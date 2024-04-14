package com.letssolvetogether.omr;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.letssolvetogether.omr.main.R;

import java.util.ArrayList;
import java.util.List;

public class AddStudentActivity extends AppCompatActivity {
    private EditText editTextStudentName;
    private long classroomId;
    private DatabaseHelper dbHelper;
    private ListView listViewStudents;
    private List<String> studentInfoList; // List to store student name and score
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        editTextStudentName = findViewById(R.id.edit_text_student_name);
        classroomId = getIntent().getLongExtra("classroomId", -1);
        dbHelper = new DatabaseHelper(this);
        listViewStudents = findViewById(R.id.list_view_students);

        studentInfoList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentInfoList);
        listViewStudents.setAdapter(adapter);

        loadStudentsFromDatabase();
    }

    private void loadStudentsFromDatabase() {
        List<String> students = dbHelper.getStudentsForClassroom(classroomId);
        studentInfoList.clear();
        for (String studentName : students) {
            // Fetch score for each student and append it to studentInfoList
            int score = dbHelper.getScoreForStudent(studentName, classroomId);
            String studentInfo = studentName + " - Score: " + score;
            studentInfoList.add(studentInfo);
        }
        adapter.notifyDataSetChanged();
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
}
