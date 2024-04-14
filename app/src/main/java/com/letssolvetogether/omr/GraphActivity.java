package com.letssolvetogether.omr;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.letssolvetogether.omr.main.R;
import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    private BarChart barChart;
    private Spinner classSpinner;
    private DatabaseHelper databaseHelper;
    private List<ClassroomBlock> classrooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        barChart = findViewById(R.id.barChart);
        classSpinner = findViewById(R.id.classSpinner);
        databaseHelper = new DatabaseHelper(this);

        classrooms = databaseHelper.getAllClassrooms();
        setupClassSpinner();

        drawGraph(classrooms.get(0).getId()); // Draw graph for the first class initially

        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                long selectedClassId = classrooms.get(position).getId();
                drawGraph(selectedClassId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    private void setupClassSpinner() {
        List<String> classNames = new ArrayList<>();
        for (ClassroomBlock classroom : classrooms) {
            classNames.add(classroom.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classNames);
        classSpinner.setAdapter(adapter);
    }

    private void drawGraph(long classId) {
        List<String> students = databaseHelper.getStudentsForClassroom(classId);

        int[] scoreCount = new int[21]; // Array to store the count of each score (1-20)
        int totalStudents = students.size();

        // Initialize counts to 0
        for (int i = 1; i <= 20; i++) {
            scoreCount[i] = 0;
        }

        // Count scores
        for (String student : students) {
            int score = databaseHelper.getScoreForStudent(student, classId);
            if (score >= 1 && score <= 20) { // Score range is 1-20
                scoreCount[score]++;
            }
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        // Populate entries for each score
        for (int i = 1; i <= 20; i++) {
            float percentage = ((float) scoreCount[i] / totalStudents) * 100;
            entries.add(new BarEntry(i - 1, percentage)); // Subtract 1 from the x-value
            labels.add(" " + i); // Adjusted to start with a space
        }

        BarDataSet dataSet = new BarDataSet(entries, "Score Distribution (%)");
        dataSet.setColor(getResources().getColor(R.color.green)); // Adjust color if needed

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f); // Adjust the width of the bars if needed

        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        xAxis.setLabelCount(labels.size());
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f); // Set the minimum value to 0
        xAxis.setAxisMaximum(19); // Set the maximum value to 19

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setAxisMinimum(0);
        rightAxis.setAxisMaximum(100);

        barChart.getDescription().setEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setDrawGridBackground(false);
        barChart.setFitBars(true);
        barChart.invalidate(); // Refresh
    }



}
