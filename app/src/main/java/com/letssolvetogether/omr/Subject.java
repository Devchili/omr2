package com.letssolvetogether.omr;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.BaseMenuPresenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.letssolvetogether.omr.main.R;

public class Subject extends AppCompatActivity {
    private LinearLayout layoutSubjects;
    private EditText editTextSubjectName;
    private DatabaseHelper dbHelper;
    private Map<Long, View> subjectViews;
    private String className; // Added to store the class name

    private long classroomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);

        // Retrieve the class name from the intent extra
        className = getIntent().getStringExtra("className");

        long classroomId = getIntent().getLongExtra("classroomId", -1);


        layoutSubjects = findViewById(R.id.list_view_subjects);

        dbHelper = new DatabaseHelper(this);
        subjectViews = new HashMap<>();

        // Set the title of the activity to the class name
        if (className != null) {
            setTitle(className + " - Classroom ID: " + classroomId);
        }


        ImageButton addButton = findViewById(R.id.btn_add_subject);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSubjectBlock();
            }
        });

        loadSubjectsFromDatabase();
    }

    private void loadSubjectsFromDatabase() {
        long classroomId = getIntent().getLongExtra("classroomId", -1);
        if (classroomId != -1) {
            List<String> subjects = dbHelper.getSubjectsByClassroomId(classroomId);
            for (String subject : subjects) {
                if (!subjectViews.containsKey(subject.hashCode())) {
                    addSubjectBlockToUI(subject);
                }
            }
        } else {
            Toast.makeText(Subject.this, "Error: Classroom ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void addSubjectBlockToUI(final String subject) {
        // Create button for the subject
        Button btnSubject = new Button(this);
        btnSubject.setText(subject);
        btnSubject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retrieve the subject ID associated with the subject name
                long subjectId = dbHelper.getSubjectId(subject);
                if (subjectId != -1) {
                    // Subject ID found, proceed to start Exam activity
                    Intent examIntent = new Intent(Subject.this, Exam.class);
                    examIntent.putExtra("classroomId", classroomId);
                    examIntent.putExtra("subjectId", subjectId);
                    examIntent.putExtra("className", className); // Pass the class name
                    examIntent.putExtra("subjectName", subject); // Pass the subject name
                    startActivity(examIntent);
                } else {
                    // Subject ID not found, show error message
                    Toast.makeText(Subject.this, "Subject ID not found for the clicked view", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(R.drawable.baseline_delete_24);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Subject.this);
                builder.setMessage("Are you sure you want to delete this exam?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dbHelper.deleteSubject(subject);
                                LinearLayout parentLayout = (LinearLayout) view.getParent();
                                layoutSubjects.removeView(parentLayout);
                                // Remove the subject from the subjectViews map
                                for (Map.Entry<Long, View> entry : subjectViews.entrySet()) {
                                    if (entry.getValue() == parentLayout) {
                                        subjectViews.remove(entry.getKey());
                                        break;
                                    }
                                }
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
        layout.addView(btnSubject);
        layout.addView(new View(this), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        layout.addView(btnDelete);

        layoutSubjects.addView(layout);
        subjectViews.put((long) subject.hashCode(), layout);
    }

    public void addSubjectBlock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Subject");

        // Set up the input for subject name
        final EditText inputSubjectName = new EditText(this);
        inputSubjectName.setHint("Enter subject name");
        builder.setView(inputSubjectName);

        // Set up the buttons
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String subjectName = inputSubjectName.getText().toString().trim();
                if (!subjectName.isEmpty()) {
                    long classroomId = getIntent().getLongExtra("classroomId", -1);
                    if (classroomId != -1) {
                        dbHelper.insertSubject(subjectName, classroomId);
                        addSubjectBlockToUI(subjectName);
                    } else {
                        Toast.makeText(Subject.this, "Error: Classroom ID not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Subject.this, "Please enter a subject name", Toast.LENGTH_SHORT).show();
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
