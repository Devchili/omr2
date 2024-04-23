package com.letssolvetogether.omr;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "classroom.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_CLASSROOM = "classroom";
    static final String TABLE_STUDENT = "student";

    // Common column names
    private static final String KEY_ID = "id";
    static final String KEY_NAME = "name";
    private static final String KEY_CLASSROOM_ID = "classroom_id";
    private static final String KEY_SCORE = "score";

    // Classroom table create statement
    private static final String CREATE_TABLE_CLASSROOM = "CREATE TABLE " + TABLE_CLASSROOM +
            "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT" + ")";

    // Student table create statement
    private static final String CREATE_TABLE_STUDENT = "CREATE TABLE " + TABLE_STUDENT +
            "(" + KEY_ID + " INTEGER PRIMARY KEY," +
            KEY_NAME + " TEXT," +
            KEY_CLASSROOM_ID + " INTEGER," +
            KEY_SCORE + " INTEGER DEFAULT 0" + ")";

    // TAG for logging
    private static final String TAG = "DatabaseHelper";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @SuppressLint("Range")
    public List<ClassroomBlock> getAllClassroomsSortedByNameAscending() {
        List<ClassroomBlock> classrooms = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CLASSROOM + " ORDER BY " + KEY_NAME + " ASC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ClassroomBlock classroom = new ClassroomBlock();
                classroom.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                classroom.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
                classrooms.add(classroom);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return classrooms;
    }

    public List<String> getStudentsForClassroomSortedByNameAscending(long classroomId) {
        List<String> studentNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME};
        String selection = KEY_CLASSROOM_ID + " = ?";
        String[] selectionArgs = {String.valueOf(classroomId)};
        String orderBy = KEY_NAME + " ASC";

        Cursor cursor = db.query(TABLE_STUDENT, projection, selection, selectionArgs, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String studentName = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                studentNames.add(studentName);
            }
            cursor.close();
        }

        db.close();

        return studentNames;
    }

    public void deleteStudent(long classroomId, String studentName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STUDENT, KEY_CLASSROOM_ID + " = ? AND " + KEY_NAME + " = ?",
                new String[]{String.valueOf(classroomId), studentName});
        db.close();
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_CLASSROOM);
        db.execSQL(CREATE_TABLE_STUDENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSROOM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENT);
        // create new tables
        onCreate(db);
    }

    public ClassroomBlock insertClassroom(String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);

        long id = db.insert(TABLE_CLASSROOM, null, values);

        db.close();

        return new ClassroomBlock(id, name);
    }

    public void insertStudent(long classroomId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_CLASSROOM_ID, classroomId);

        db.insert(TABLE_STUDENT, null, values);
        db.close();
    }

    @SuppressLint("Range")
    public List<ClassroomBlock> getAllClassrooms() {
        List<ClassroomBlock> classrooms = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CLASSROOM;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ClassroomBlock classroom = new ClassroomBlock();
                classroom.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                classroom.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
                classrooms.add(classroom);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return classrooms;
    }

    public List<String> getStudentsForClassroom(long classroomId) {
        List<String> studentNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME};
        String selection = KEY_CLASSROOM_ID + " = ?";
        String[] selectionArgs = {String.valueOf(classroomId)};

        Cursor cursor = db.query(TABLE_STUDENT, projection, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String studentName = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                studentNames.add(studentName);
            }
            cursor.close();
        }

        db.close();

        return studentNames;
    }

    public void deleteClassroom(long classroomId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CLASSROOM, KEY_ID + " = ?", new String[]{String.valueOf(classroomId)});
        db.delete(TABLE_STUDENT, KEY_CLASSROOM_ID + " = ?", new String[]{String.valueOf(classroomId)});
        db.close();
    }

    public void saveScore(String classroomName, String studentName, int score) {
        SQLiteDatabase db = null;
        try {
            // Get the classroom ID
            long classroomId = getClassroomId(classroomName);
            if (classroomId == -1) {
                // Handle the case when classroom is not found
                return;
            }

            // Open the database connection
            db = this.getWritableDatabase();

            // Update the score for the student in the specified classroom
            ContentValues values = new ContentValues();
            values.put(KEY_SCORE, score);

            String selection = KEY_NAME + " = ? AND " + KEY_CLASSROOM_ID + " = ?";
            String[] selectionArgs = {studentName, String.valueOf(classroomId)};

            db.update(TABLE_STUDENT, values, selection, selectionArgs);
        } catch (Exception e) {
            // Handle any exceptions, log or display error message
            Log.e(TAG, "Error saving score: " + e.getMessage());
        } finally {
            // Close the database connection
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }


    @SuppressLint("Range")
    public int getScoreForStudent(String studentName, long classroomId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int score = -1;

        String query = "SELECT " + KEY_SCORE + " FROM " + TABLE_STUDENT +
                " WHERE " + KEY_NAME + " = ? AND " + KEY_CLASSROOM_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{studentName, String.valueOf(classroomId)});

        if (cursor != null && cursor.moveToFirst()) {
            score = cursor.getInt(cursor.getColumnIndex(KEY_SCORE));
            cursor.close();
        }

        db.close();

        return score;
    }


    @SuppressLint("Range")
    private long getClassroomId(String classroomName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {KEY_ID};
        String selection = KEY_NAME + " = ?";
        String[] selectionArgs = {classroomName};

        Cursor cursor = db.query(TABLE_CLASSROOM, projection, selection, selectionArgs, null, null, null);
        long classroomId = -1;

        if (cursor != null && cursor.moveToFirst()) {
            classroomId = cursor.getLong(cursor.getColumnIndex(KEY_ID));
            cursor.close();
        }

        db.close();

        return classroomId;
    }
}
