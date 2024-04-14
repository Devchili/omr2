package com.letssolvetogether.omr;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.letssolvetogether.omr.main.R;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.widget.ArrayAdapter;
import android.widget.ListView;



import java.util.ArrayList;
import java.util.List;

public class ViewDatabaseContentsActivity extends AppCompatActivity {

    private ListView listViewDatabaseContents;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_database_contents);

        listViewDatabaseContents = findViewById(R.id.list_view_database_contents);
        dbHelper = new DatabaseHelper(this);

        displayDatabaseContents();
    }

    private void displayDatabaseContents() {
        List<String> databaseContents = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get table names
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                databaseContents.add(tableName);

                // Get table contents
                Cursor tableCursor = db.rawQuery("SELECT * FROM " + tableName, null);
                if (tableCursor != null) {
                    while (tableCursor.moveToNext()) {
                        StringBuilder rowContent = new StringBuilder();
                        for (int i = 0; i < tableCursor.getColumnCount(); i++) {
                            rowContent.append(tableCursor.getColumnName(i)).append(": ")
                                    .append(tableCursor.getString(i)).append(", ");
                        }
                        databaseContents.add(rowContent.toString());
                    }
                    tableCursor.close();
                }
            }
            cursor.close();
        }

        db.close();

        // Display database contents in ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, databaseContents);
        listViewDatabaseContents.setAdapter(adapter);
    }
}
