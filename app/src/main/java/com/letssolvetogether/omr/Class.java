package com.letssolvetogether.omr;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import android.util.Log;

import java.util.HashMap;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.letssolvetogether.omr.main.R;

import java.util.List;
import java.util.Map;

public class Class extends AppCompatActivity {
    private LinearLayout layoutClassrooms;
    private EditText editTextClassroomName;
    private DatabaseHelper dbHelper;

    // Map to keep track of added classroom blocks
    private Map<Long, View> classroomViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        layoutClassrooms = findViewById(R.id.list_view_classrooms);
        editTextClassroomName = findViewById(R.id.edit_text_classroom_name);
        dbHelper = new DatabaseHelper(this);
        classroomViews = new HashMap<>();

        Button addButton = findViewById(R.id.btn_add_classroom);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addClassroomBlock();
            }
        });

        // Load existing classrooms from the database
        loadClassroomsFromDatabase();
    }

    private void loadClassroomsFromDatabase() {
        List<ClassroomBlock> classrooms = dbHelper.getAllClassroomsSortedByNameAscending();
        for (ClassroomBlock classroom : classrooms) {
            // Check if the classroom is not already added
            if (!classroomViews.containsKey(classroom.getId())) {
                addClassroomBlockToUI(classroom);
            }
        }
    }


    private void addClassroomBlockToUI(ClassroomBlock classroom) {
        Button btnClassroom = new Button(this);
        btnClassroom.setText(classroom.getName());
        btnClassroom.setOnClickListener(view -> {
            Intent intent = new Intent(Class.this, AddStudentActivity.class);
            intent.putExtra("classroomId", classroom.getId());
            startActivity(intent);
        });

        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(R.drawable.baseline_delete_24);
        btnDelete.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure you want to delete this classroom?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dbHelper.deleteClassroom(classroom.getId());
                            // Find the parent of the delete button, which is the LinearLayout containing both buttons
                            LinearLayout parentLayout = (LinearLayout) view.getParent();
                            // Remove the parent layout
                            layoutClassrooms.removeView(parentLayout);
                            // Remove from the map
                            classroomViews.remove(classroom.getId());
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog, do nothing
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(btnClassroom);

        // Add spacer to push delete button to the right
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        layout.addView(new View(this), params);

        layout.addView(btnDelete);

        // Add the new classroom block to the end of the classrooms layout
        layoutClassrooms.addView(layout, layoutClassrooms.getChildCount());

        // Add to the map
        classroomViews.put(classroom.getId(), layout);
    }


    public void addClassroomBlock() {
        String classroomName = editTextClassroomName.getText().toString().trim();
        if (!classroomName.isEmpty()) {
            // Insert the new classroom into the database
            ClassroomBlock classroom = dbHelper.insertClassroom(classroomName);

            // Add the newly inserted classroom to the UI
            addClassroomBlockToUI(classroom);

            // Clear the EditText field
            editTextClassroomName.getText().clear();
        }
    }
}