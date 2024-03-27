package com.letssolvetogether.omr;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.letssolvetogether.omr.main.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private List<String> studentScoresList;
    private PieChart pieChart;
    private SharedPreferences sharedPreferences;
    private Spinner filterSpinner;
    private String selectedFilter = "All"; // Initialize selectedFilter with a default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        sharedPreferences = getSharedPreferences("OMR_PREFERENCES", MODE_PRIVATE);

        // Initialize UI components
        ListView listView = findViewById(R.id.listView);
        filterSpinner = findViewById(R.id.spinner);
        pieChart = findViewById(R.id.pieChart);
        Button btnReset = findViewById(R.id.btnReset);

        // Initialize student scores list
        studentScoresList = new ArrayList<>();

        // Initialize ArrayAdapter for ListView
        adapter = new ArrayAdapter<>(this, R.layout.list_item_layout, R.id.textViewListItem, studentScoresList);
        listView.setAdapter(adapter);

        // Populate filter spinner options
        String[] filterOptions = {"All", "Quiz 1", "Quiz 2", "Quiz 3", "Quiz 4", "Quiz 5"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterOptions);
        filterSpinner.setAdapter(filterAdapter);

        // Load student scores initially
        loadStudentScores(selectedFilter); // Pass selectedFilter to loadStudentScores

        // Set up filter spinner listener
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFilter = filterOptions[position]; // Update selectedFilter
                loadStudentScores(selectedFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set up reset button click listener
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirmationDialog();
            }
        });
    }

    private void loadStudentScores(String filter) {
        Map<String, ?> allEntries = sharedPreferences.getAll();
        studentScoresList.clear();
        int totalScores = 0;
        int totalWrongScores = 0;

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            if (key.equals("StudentScores")) {
                String[] studentScores = entry.getValue().toString().split(";");
                if (studentScores.length >= 1) {
                    if (filter.equals("All")) {
                        studentScoresList.addAll(Arrays.asList(studentScores));
                    } else {
                        for (String score : studentScores) {
                            if (score.contains(filter)) {
                                studentScoresList.add(score);
                            }
                        }
                    }

                    // Recalculate totals based on the selected filter
                    for (String score : studentScoresList) {
                        String[] scoreArray = score.split(":");
                        if (scoreArray.length >= 2) {
                            try {
                                int studentScore = Integer.parseInt(scoreArray[1].trim());
                                totalScores++;
                                // Calculate wrong scores
                                int wrongScore = 20 - studentScore;
                                if (wrongScore > 0) {
                                    totalWrongScores += wrongScore;
                                }
                            } catch (NumberFormatException e) {
                                // Handle invalid score format gracefully
                                // Log the error or skip this score
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            }
        }

        // Calculate percentage of wrong scores out of total scores
        float percentageWrong;
        if (totalScores > 0) {
            percentageWrong = ((float) totalWrongScores / (totalScores * 20)) * 100;
        } else {
            percentageWrong = 0;
        }

        // Calculate percentage of correct scores out of total scores
        float percentageCorrect = 100 - percentageWrong;

        // Log the loaded scores
        Log.d("LoadedScores", "Filter: " + filter + ", Scores: " + studentScoresList.toString());

        // Update ListView
        adapter.notifyDataSetChanged();

        // Update PieChart
        updatePieChart(percentageCorrect, percentageWrong);
    }

    private void updatePieChart(float percentageCorrect, float percentageWrong) {
        // Populate data for the PieChart
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(percentageCorrect, "Correct"));
        entries.add(new PieEntry(percentageWrong, "Wrong"));

        // Set up the dataset
        PieDataSet pieDataSet = new PieDataSet(entries, "Score Analysis");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

        // Set up the data and apply it to the chart
        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(pieData);

        // Customize chart properties
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(android.R.color.transparent);
        pieChart.setTransparentCircleRadius(61f);

        pieChart.invalidate();
    }


    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(ResultActivity.this)
                .setTitle("Reset Confirmation")
                .setMessage("Are you sure you want to reset?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove("StudentScores");
                        editor.apply();
                        studentScoresList.clear();
                        adapter.notifyDataSetChanged();
                        pieChart.clear();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
