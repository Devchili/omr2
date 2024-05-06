package com.letssolvetogether.omr;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "classroom.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_CLASSROOM = "classroom";
    private static final String TABLE_SUBJECT = "subject";
    private static final String TABLE_EXAM = "exam";
    private static final String TABLE_STUDENT = "student";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_CLASSROOM_ID = "classroom_id";
    private static final String KEY_SCORE = "score";

    private static final String KEY_EXAM_ID = "exam_id";

    public static final String KEY_EXAM_NAME = "exam_name";

    // Classroom table create statement
    private static final String CREATE_TABLE_CLASSROOM = "CREATE TABLE " + TABLE_CLASSROOM +
            "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT UNIQUE" + ")";

    private static final String CREATE_TABLE_SUBJECT = "CREATE TABLE " + TABLE_SUBJECT +
            "(" + KEY_ID + " INTEGER PRIMARY KEY," +
            KEY_NAME + " TEXT UNIQUE," +
            KEY_CLASSROOM_ID + " INTEGER," +
            "FOREIGN KEY(" + KEY_CLASSROOM_ID + ") REFERENCES " + TABLE_CLASSROOM + "(" + KEY_ID + ")" + ")";

    private static final String CREATE_TABLE_EXAM = "CREATE TABLE " + TABLE_EXAM +
            "(" + KEY_ID + " INTEGER PRIMARY KEY," +
            KEY_NAME + " TEXT," +
            "subject_id INTEGER," +
            "UNIQUE(" + KEY_NAME + ", subject_id)," + // Ensure uniqueness on name and subject_id
            "FOREIGN KEY(subject_id) REFERENCES " + TABLE_SUBJECT + "(" + KEY_ID + ")" + ")";

    private static final String CREATE_TABLE_STUDENT = "CREATE TABLE " + TABLE_STUDENT +
            "(" + KEY_ID + " INTEGER PRIMARY KEY," +
            KEY_NAME + " TEXT," +
            KEY_EXAM_ID + " INTEGER," +
            "exam_name TEXT," +
            "subject_id INTEGER," +
            KEY_SCORE + " INTEGER DEFAULT 0," +
            "UNIQUE(" + KEY_NAME + ", " + KEY_EXAM_ID + ")" + ")"; // Ensure uniqueness on name and exam_id

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


    public List<StudentBlock> getAllStudentsWithScores() {
        List<StudentBlock> students = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME, KEY_SCORE};
        Cursor cursor = db.query(TABLE_STUDENT, projection, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                int score = cursor.getInt(cursor.getColumnIndex(KEY_SCORE));
                students.add(new StudentBlock(name, score));
            }
            cursor.close();
        }

        db.close();

        return students;
    }

    public static class StudentBlock {
        private String name;
        private int score;

        public StudentBlock(String name, int score) {
            this.name = name;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public int getScore() {
            return score;
        }
    }


    public List<String> getStudentsForeExamSortedByNameAscending(long examId) {
        List<String> studentNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME};
        String selection = KEY_EXAM_ID + " = ?";
        String[] selectionArgs = {String.valueOf(examId)};
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

    @SuppressLint("Range")
    public String getExamNameForStudent(String studentName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String examName = null;

        String[] projection = {"exam_name"};
        String selection = KEY_NAME + " = ?";
        String[] selectionArgs = {studentName};

        Cursor cursor = db.query(TABLE_STUDENT, projection, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            examName = cursor.getString(cursor.getColumnIndex("exam_name"));
            cursor.close();
        }

        db.close();

        return examName;
    }

    public List<String> getStudentsForExamByName(String examName) {
        List<String> studentNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME};
        String selection = "exam_name = ?";
        String[] selectionArgs = {examName};

        Cursor cursor = db.query(TABLE_STUDENT, projection, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String studentName = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                studentNames.add(studentName);
            }
            cursor.close();
        }


        return studentNames;
    }


    public boolean hasExamWithName(String examName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {KEY_ID};
        String selection = KEY_NAME + " = ?";
        String[] selectionArgs = {examName};
        Cursor cursor = db.query(TABLE_EXAM, projection, selection, selectionArgs, null, null, null);
        boolean hasExam = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return hasExam;
    }



    public List<String> getAllExamsSortedByNameAscending(long subjectId) {
        List<String> exams = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME};
        String selection = "subject_id = ?";
        String[] selectionArgs = {String.valueOf(subjectId)};
        String orderBy = KEY_NAME + " ASC";

        Cursor cursor = db.query(TABLE_EXAM, projection, selection, selectionArgs, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String exam = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                exams.add(exam);
            }
            cursor.close();
        }

        db.close();

        return exams;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_CLASSROOM);
        db.execSQL(CREATE_TABLE_SUBJECT);
        db.execSQL(CREATE_TABLE_EXAM);
        db.execSQL(CREATE_TABLE_STUDENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSROOM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXAM);
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

    public long insertSubject(String name, long classroomId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_CLASSROOM_ID, classroomId);

        long id = db.insert(TABLE_SUBJECT, null, values);

        db.close();

        return id;
    }

    public void insertExam(String name, long subjectId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put("subject_id", subjectId); // Store the subject ID

        long id = db.insert(TABLE_EXAM, null, values);

        db.close();

    }

    @SuppressLint("Range")
    public long getSubjectId(String subjectName) {
        if (subjectName == null) {
            return -1; // Or handle this case accordingly
        }

        SQLiteDatabase db = this.getReadableDatabase();
        String[] projection = {KEY_ID};
        String selection = KEY_NAME + " = ?";
        String[] selectionArgs = {subjectName};
        long subjectId = -1;

        Cursor cursor = db.query(TABLE_SUBJECT, projection, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            subjectId = cursor.getLong(cursor.getColumnIndex(KEY_ID));
            cursor.close();
        }

        db.close();

        return subjectId;
    }



    public void insertStudent(long examId, String examName, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_EXAM_ID, examId); // Store the exam ID
        values.put("exam_name", examName); // Store the exam name

        db.insert(TABLE_STUDENT, null, values);
        db.close();
    }






    public void deleteClassroom(long classroomId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CLASSROOM, KEY_ID + " = ?", new String[]{String.valueOf(classroomId)});
        db.delete(TABLE_STUDENT, KEY_CLASSROOM_ID + " = ?", new String[]{String.valueOf(classroomId)});
        db.close();
    }

    public void deleteSubject(String subjectName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SUBJECT, KEY_NAME + " = ?", new String[]{subjectName});
        db.close();
    }

    public void deleteExam(String examName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXAM, KEY_NAME + " = ?", new String[]{examName});
        db.close();
    }

    public void deleteStudent(long examId, String studentName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STUDENT, KEY_EXAM_ID + " = ? AND " + KEY_NAME + " = ?",
                new String[]{String.valueOf(examId), studentName});
        db.close();
    }

    @SuppressLint("Range")
    private long getExamId(String examName, SQLiteDatabase db) {
        db = this.getReadableDatabase();
        long examId = -1;

        String[] projection = {KEY_ID};
        String selection = KEY_NAME + " = ?";
        String[] selectionArgs = {examName};

        Cursor cursor = db.query(TABLE_EXAM, projection, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            examId = cursor.getLong(cursor.getColumnIndex(KEY_ID));
            cursor.close();
        }



        return examId;
    }

    public void saveScore(String examName, String selectedStudentName, int score) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            // Open the database connection
            db = this.getWritableDatabase();

            // Get the exam ID using the exam name
            long examId = getExamId(examName, db);
            if (examId == -1) {
                // Handle the case when exam is not found
                Log.e(TAG, "Error saving score: Exam not found");
                return;
            }

            // Check if the student exists for the specified exam
            String selection = KEY_NAME + " = ? AND " + KEY_EXAM_ID + " = ?";
            String[] selectionArgs = {selectedStudentName, String.valueOf(examId)};

            cursor = db.query(TABLE_STUDENT, new String[]{KEY_ID}, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                // Student exists, update the name and score
                long studentId = cursor.getLong(cursor.getColumnIndex(KEY_ID));
                ContentValues values = new ContentValues();
                values.put(KEY_NAME, selectedStudentName); // Update name
                values.put(KEY_SCORE, score); // Update score

                db.update(TABLE_STUDENT, values, KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
            } else {
                // Insert new student's score
                ContentValues values = new ContentValues();
                values.put(KEY_NAME, selectedStudentName);
                values.put(KEY_EXAM_ID, examId); // Include the exam ID
                values.put(KEY_EXAM_NAME, examName); // Include the exam name
                values.put(KEY_SCORE, score);

                db.insert(TABLE_STUDENT, null, values);
            }

            // Retrieve the score for logging or further processing
            List<Integer> savedScore = getScoresForStudent(String.valueOf(examId));
            Log.d(TAG, "Score saved for " + selectedStudentName + " in exam " + examName + ": " + savedScore);

        } catch (Exception e) {
            // Handle any exceptions, log or display error message
            Log.e(TAG, "Error saving score: " + e.getMessage());
        } finally {
            // Close the cursor
            if (cursor != null) {
                cursor.close();
            }
            // Close the database connection
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }



    @SuppressLint("Range")
    public List<Integer> getScoresForStudent(String studentName) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Integer> scores = new ArrayList<>();

        String selection = KEY_NAME + " = ?";
        String[] selectionArgs = {studentName};

        Cursor cursor = db.query(TABLE_STUDENT, new String[]{KEY_SCORE}, selection, selectionArgs, null, null, null);

        try {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int score = cursor.getInt(cursor.getColumnIndex(KEY_SCORE));
                    scores.add(score);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving scores for student: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return scores;
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

    @SuppressLint("Range")
    public List<String> getSubjectsByClassroomId(long classroomId) {
        List<String> subjects = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {KEY_NAME};
        String selection = KEY_CLASSROOM_ID + " = ?";
        String[] selectionArgs = {String.valueOf(classroomId)};

        Cursor cursor = db.query(TABLE_SUBJECT, projection, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String subject = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                subjects.add(subject);
            }
            cursor.close();
        }

        db.close();

        return subjects;
    }
}
