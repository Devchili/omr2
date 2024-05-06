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
import android.widget.Toast;

import java.util.HashMap;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.letssolvetogether.omr.camera.ui.CameraActivity;
import com.letssolvetogether.omr.main.R;
import com.letssolvetogether.omr.omrkey.ui.OMRKeyActivity;

import java.util.List;
import java.util.Map;

public class Class extends AppCompatActivity {
    private LinearLayout layoutClassrooms;

    private DatabaseHelper dbHelper;
    private Map<Long, View> classroomViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        layoutClassrooms = findViewById(R.id.list_view_classrooms);

        dbHelper = new DatabaseHelper(this);
        classroomViews = new HashMap<>();

        ImageButton addButton = findViewById(R.id.btn_add_class);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addClassroomBlock();
            }
        });

        loadClassroomsFromDatabase();
    }

    private void loadClassroomsFromDatabase() {
        List<ClassroomBlock> classrooms = dbHelper.getAllClassroomsSortedByNameAscending();
        for (ClassroomBlock classroom : classrooms) {
            if (!classroomViews.containsKey(classroom.getId())) {
                addClassroomBlockToUI(classroom);
            }
        }
    }


    private void addClassroomBlockToUI (ClassroomBlock classroom){
        // Create buttons for the classroom and delete
        Button btnClassroom = new Button(this);
        btnClassroom.setText(classroom.getName());
        btnClassroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Class.this, Subject.class);
                intent.putExtra("classroomId", classroom.getId());
                startActivity(intent);
            }
        });


        ImageButton btnCam = new ImageButton(this);
        btnCam.setImageResource(R.drawable.baseline_camera_alt_24);
        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Class.this, Database.class);
                intent.putExtra("classroomId", classroom.getId());
                startActivity(intent);
            }
        });


        ImageButton btnDelete = new ImageButton(this);
        btnDelete.setImageResource(R.drawable.baseline_delete_24);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Class.this);
                builder.setMessage("Are you sure you want to delete this classroom?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dbHelper.deleteClassroom(classroom.getId());
                                LinearLayout parentLayout = (LinearLayout) view.getParent();
                                layoutClassrooms.removeView(parentLayout);
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
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(btnClassroom);
        // Add OMR Key button to the left


        // Add spacer to push the buttons to the right
        layout.addView(new View(this), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

        layout.addView(btnCam);
        layout.addView(btnDelete);

        layoutClassrooms.addView(layout);
        classroomViews.put(classroom.getId(), layout);
    }

    public void addClassroomBlock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Classroom");

        // Set up the input for classroom name
        final EditText inputClassroomName = new EditText(this);
        inputClassroomName.setHint("Enter classroom name");
        builder.setView(inputClassroomName);

        // Set up the buttons
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String classroomName = inputClassroomName.getText().toString().trim();
                if (!classroomName.isEmpty()) {
                    ClassroomBlock classroom = dbHelper.insertClassroom(classroomName);
                    addClassroomBlockToUI(classroom);
                } else {
                    Toast.makeText(Class.this, "Please enter a classroom name", Toast.LENGTH_SHORT).show();
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
