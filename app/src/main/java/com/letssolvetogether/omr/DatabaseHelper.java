package com.letssolvetogether.omr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
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
            "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_NAME + " TEXT" + ")";

    private static final String CREATE_TABLE_SUBJECT = "CREATE TABLE " + TABLE_SUBJECT +
            "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_NAME + " TEXT," +
            KEY_CLASSROOM_ID + " INTEGER," +
            "FOREIGN KEY(" + KEY_CLASSROOM_ID + ") REFERENCES " + TABLE_CLASSROOM + "(" + KEY_ID + ")" + ")";

    private static final String CREATE_TABLE_EXAM = "CREATE TABLE " + TABLE_EXAM +
            "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_NAME + " TEXT," +
            "subject_id INTEGER," +
            "num_questions INTEGER," + // Add column for number of questions
            "FOREIGN KEY(subject_id) REFERENCES " + TABLE_SUBJECT + "(" + KEY_ID + ")" + ")";

    private static final String CREATE_TABLE_STUDENT = "CREATE TABLE " + TABLE_STUDENT +
            "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            KEY_NAME + " TEXT," +
            KEY_EXAM_ID + " INTEGER," +
            "exam_name TEXT," +
            "subject_id INTEGER," +
            KEY_SCORE + " INTEGER DEFAULT 0" + ")";

    private static final String CREATE_TABLE_ANSWER_COMPARISON = "CREATE TABLE answer_comparison (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "question_number INTEGER," +
            "student_answer TEXT," +
            "correct_answer TEXT," +
            "is_correct INTEGER" +
            ")";



    // TAG for logging
    private static final String TAG = "DatabaseHelper";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 134123;

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
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(KEY_NAME));
                @SuppressLint("Range") int score = cursor.getInt(cursor.getColumnIndex(KEY_SCORE));
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

    public void exportAllScoresToExcel(Context context, String className, String subjectName, String examName) {
        // Get all student scores
        List<Map.Entry<String, Integer>> allScores = getScoresForAllStudents();

        // Create a new workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("All Student Scores");

        // Add header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Student Name");
        headerRow.createCell(1).setCellValue("Score");

        // Add data rows
        int rowNum = 1;
        for (Map.Entry<String, Integer> entry : allScores) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey()); // Student name
            row.createCell(1).setCellValue(entry.getValue()); // Score
        }

        // Generate the file name based on classroom, subject, and exam names
        String now = String.valueOf(System.currentTimeMillis());
        String fileName = className + "_" + subjectName + "_" + examName + ".xlsx";

        // Determine the file path based on Android version
        String filePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + fileName;
        } else {
            filePath = Environment.getExternalStorageDirectory().toString() + "/" + fileName;
        }


        // Save the workbook to the specified file path
        File file = new File(filePath);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
            Toast.makeText(context, "All student scores exported to Excel successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error exporting all student scores to Excel: " + e.getMessage());
            Toast.makeText(context, "Error exporting all student scores to Excel", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing workbook: " + e.getMessage());
            }
        }
    }


    private List<Map.Entry<String, Integer>> getScoresForAllStudents() {
        List<Map.Entry<String, Integer>> allScores = new ArrayList<>();

        // Get all students with their scores
        List<StudentBlock> students = getAllStudentsWithScores();
        for (StudentBlock student : students) {
            String studentName = student.getName();
            int score = student.getScore();
            allScores.add(new AbstractMap.SimpleEntry<>(studentName, score));
        }

        return allScores;
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
        db.execSQL(CREATE_TABLE_ANSWER_COMPARISON);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASSROOM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUBJECT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXAM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENT);
        db.execSQL(CREATE_TABLE_ANSWER_COMPARISON);
        // create new tables
        onCreate(db);
    }


    public void insertAnswerComparison(long examId, String studentName, byte[][] studentAnswers, int[] correctAnswers, int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Insert student name and exam ID
        values.put("exam_id", examId);
        values.put("student_name", studentName);

        // Insert each question's comparison
        for (int i = 0; i < studentAnswers.length; i++) {
            values.put("question_number", i + 1); // Question numbers start from 1
            values.put("student_answer", convertByteArrayToString(studentAnswers[i]));
            values.put("correct_answer", String.valueOf(correctAnswers[i]));
            values.put("is_correct", studentAnswers[i].equals(correctAnswers[i]) ? 1 : 0); // Compare student's answer with correct answer

            // Insert the comparison into the database
            db.insert("answer_comparison", null, values);
        }

        // Insert total score
        values.clear();
        values.put("exam_id", examId);
        values.put("student_name", studentName);
        values.put("total_score", score);
        db.insert("exam_scores", null, values);

        db.close();
    }

    // Utility method to convert byte array to String
    private String convertByteArrayToString(byte[] byteArray) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : byteArray) {
            stringBuilder.append(b);
        }
        return stringBuilder.toString();
    }

    public List<AnswerComparisonBlock> getAllAnswerComparisonData() {
        List<AnswerComparisonBlock> answerComparisonBlocks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {"question_number", "student_answer", "correct_answer", "is_correct"};
        Cursor cursor = db.query("answer_comparison", projection, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int questionNumber = cursor.getInt(cursor.getColumnIndex("question_number"));
                String studentAnswer = cursor.getString(cursor.getColumnIndex("student_answer"));
                String correctAnswer = cursor.getString(cursor.getColumnIndex("correct_answer"));
                int isCorrectInt = cursor.getInt(cursor.getColumnIndex("is_correct"));
                boolean isCorrect = isCorrectInt == 1;
                answerComparisonBlocks.add(new AnswerComparisonBlock(questionNumber, studentAnswer, correctAnswer, isCorrect));
            }
            cursor.close();
        }

        db.close();

        return answerComparisonBlocks;
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

    public void insertExam(String name, long subjectId, int numQuestions) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put("subject_id", subjectId); // Store the subject ID
        values.put("num_questions", numQuestions); // Store the number of questions

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
        try {
            // Delete all subjects associated with the classroom
            db.delete(TABLE_SUBJECT, KEY_CLASSROOM_ID + " = ?", new String[]{String.valueOf(classroomId)});

            // Get all exams associated with the subjects of this classroom
            String selectExamsQuery = "SELECT " + KEY_ID + " FROM " + TABLE_EXAM +
                    " WHERE " + "subject_id" + " IN (SELECT " + KEY_ID + " FROM " + TABLE_SUBJECT +
                    " WHERE " + KEY_CLASSROOM_ID + " = ?)";
            Cursor cursor = db.rawQuery(selectExamsQuery, new String[]{String.valueOf(classroomId)});

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    @SuppressLint("Range")  long examId = cursor.getLong(cursor.getColumnIndex(KEY_ID));

                    // Delete all students associated with each exam
                    db.delete(TABLE_STUDENT, KEY_EXAM_ID + " = ?", new String[]{String.valueOf(examId)});

                    // Delete the exam itself
                    db.delete(TABLE_EXAM, KEY_ID + " = ?", new String[]{String.valueOf(examId)});
                }
                cursor.close();
            }

            // Finally, delete the classroom
            db.delete(TABLE_CLASSROOM, KEY_ID + " = ?", new String[]{String.valueOf(classroomId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting classroom and its related data: " + e.getMessage());
        } finally {
            db.close();
        }
    }


    public void deleteSubject(String subjectName) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Get the subject ID
            long subjectId = getSubjectId(subjectName);
            if (subjectId == -1) {
                Log.e(TAG, "Subject not found: " + subjectName);
                return;
            }

            // Delete all exams associated with the subject
            db.delete(TABLE_EXAM, "subject_id = ?", new String[]{String.valueOf(subjectId)});

            // Delete all students associated with the subject and exam name
            db.delete(TABLE_STUDENT, "subject_id = ? AND exam_name = ?",
                    new String[]{String.valueOf(subjectId), subjectName});

            // Finally, delete the subject itself
            db.delete(TABLE_SUBJECT, KEY_ID + " = ?", new String[]{String.valueOf(subjectId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting subject and its related data: " + e.getMessage());
        } finally {
            db.close();
        }
    }


    public void deleteExam(String examName) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Delete all students associated with the exam name
            db.delete(TABLE_STUDENT, "exam_name = ?", new String[]{examName});

            // Finally, delete the exam itself
            db.delete(TABLE_EXAM, KEY_NAME + " = ?", new String[]{examName});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting exam and its related data: " + e.getMessage());
        } finally {
            db.close();
        }
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
                @SuppressLint("Range") long studentId = cursor.getLong(cursor.getColumnIndex(KEY_ID));
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
