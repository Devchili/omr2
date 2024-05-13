package com.letssolvetogether.omr;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import com.letssolvetogether.omr.main.R;

import java.util.List;

public class Database extends AppCompatActivity {

    private TextView databaseContentTextView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        // Initialize views
        databaseContentTextView = findViewById(R.id.database_content_text_view);

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Display all contents
        displayAllContents();
    }

    private void displayAllContents() {
        StringBuilder stringBuilder = new StringBuilder();

        // Get all classrooms
        List<ClassroomBlock> classrooms = dbHelper.getAllClassroomsSortedByNameAscending();
        stringBuilder.append("Classrooms:\n");
        for (ClassroomBlock classroom : classrooms) {
            stringBuilder.append(classroom.getName()).append("\n");
        }
        stringBuilder.append("\n");

        // Get all students with their scores
        List<DatabaseHelper.StudentBlock> students = dbHelper.getAllStudentsWithScores();
        stringBuilder.append("Students and their Scores:\n");
        for (DatabaseHelper.StudentBlock student : students) {
            stringBuilder.append(student.getName()).append(": ").append(student.getScore()).append("\n");

            // Retrieve student answers and correct answers
            List<AnswerComparisonBlock> answerComparisons = dbHelper.getAllAnswerComparisonData();
            for (AnswerComparisonBlock answerComparison : answerComparisons) {
                stringBuilder.append("Question ").append(answerComparison.getQuestionNumber()).append(": ");
                stringBuilder.append("Student's Answer: ").append(answerComparison.getStudentAnswer()).append(", ");
                stringBuilder.append("Correct Answer: ").append(answerComparison.getCorrectAnswer()).append(", ");
                stringBuilder.append("Is Correct: ").append(answerComparison.isCorrect()).append("\n");
            }
        }
        stringBuilder.append("\n");

        // Display the content in TextView
        databaseContentTextView.setText(stringBuilder.toString());
    }

}
