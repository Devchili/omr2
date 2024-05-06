package com.letssolvetogether.omr.omrkey.ui;

import androidx.room.Room;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.letssolvetogether.omr.omrkey.db.AppDatabase;
import com.letssolvetogether.omr.omrkey.db.OMRKey;
import com.letssolvetogether.omr.main.R;
import com.letssolvetogether.omr.utils.AnswersUtils;

public class OMRKeyActivity extends AppCompatActivity implements RadioButton.OnCheckedChangeListener{

    private int circleIds[] = new int[]{R.mipmap.ic_omr_circle_a, R.mipmap.ic_omr_circle_b, R.mipmap.ic_omr_circle_c, R.mipmap.ic_omr_circle_d};

    private int[] correctAnswers;

    private int noOfQuestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_omrkey);

        noOfQuestions = getIntent().getIntExtra("noOfQuestions", noOfQuestions);
        createAnswerKey(noOfQuestions);
        loadCorrectAnswers(noOfQuestions);

        findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storeCorrectAnswers(noOfQuestions);
            }
        });
    }

    public void onSubmitButtonClick(View view) {
        storeCorrectAnswers(noOfQuestions);

    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        int id = compoundButton.getId();
        CheckBox checkBox;

        for (int i = (id/4)*4; i < (id/4)*4 + 4; i++){
            checkBox = findViewById(i);
            if(checkBox.isChecked() && i != id){
                checkBox.setButtonDrawable(circleIds[i%4]);
                checkBox.setChecked(false);
                break;
            }
        }

        if(checked){
            compoundButton.setButtonDrawable(R.mipmap.ic_omr_black_circle);
            compoundButton.setChecked(true);
        }
        else {
            compoundButton.setButtonDrawable(circleIds[id%4]);
            compoundButton.setChecked(false);
        }
    }

    public void createAnswerKey(int noOfQuestions){
        TextView textView;
        CheckBox checkBox;
        TableLayout tableLayout = findViewById(R.id.tableLayout);
        TableRow tableRow;

        for(int i=0; i < noOfQuestions; i++){
            textView = new TextView(this);
            tableRow = new TableRow(this);

            if(i<9)
                textView.setText("\t"+String.valueOf(i+1)+")\t\t");
            else
                textView.setText(String.valueOf(i+1)+")\t\t");

            textView.setTextSize(20);
            textView.setPadding(4,0,0,0);

            tableRow.addView(textView);

            for(int j=0; j<4; j++){
                checkBox = new CheckBox(this);
                checkBox.setId((i*4)+j);
                checkBox.setButtonDrawable(circleIds[j]);
                checkBox.setPadding(4,30,4,30);
                checkBox.setOnCheckedChangeListener(this);
                tableRow.addView(checkBox);
            }
            tableLayout.addView(tableRow);
        }
    }

    public void loadCorrectAnswers(final int noOfQuestions){
        final String[] strCorrectAnswers = {""};
        final AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "omr").build();
        final OMRKey omrKey = new OMRKey();
        omrKey.setOmrkeyid(noOfQuestions);
        omrKey.setStrCorrectAnswers(strCorrectAnswers[0]);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if(db.omrKeyDao().findById(noOfQuestions) != null)
                    strCorrectAnswers[0] = db.omrKeyDao().findById(noOfQuestions).getStrCorrectAnswers();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                int[] answers;
                int correctAnswer;
                CheckBox checkBox;

                answers = AnswersUtils.strtointAnswers(strCorrectAnswers[0]);

                if(answers != null){
                    for(int i=0; i< answers.length; i++){
                        correctAnswer = answers[i];
                        if(correctAnswer != 0){
                            checkBox = findViewById((i*4) + (correctAnswer - 1));
                            checkBox.setChecked(true);
                        }
                    }
                }
            }
        }.execute();
    }

    public void storeCorrectAnswers(int noOfQuestions){
        correctAnswers = new int[noOfQuestions];
        int cnt = -1;
        CheckBox checkBox;
        for(int i=0; i < noOfQuestions * 4; i++){
            checkBox = findViewById(i);

            if(i%4 == 0)
                cnt++;

            if(checkBox.isChecked()){
                correctAnswers[cnt] = (i % 4) + 1;
            }
        }

        String strCorrectAnswers = AnswersUtils.inttostrAnswers(correctAnswers);

        if(strCorrectAnswers != null){
            final AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class, "omr").build();
            final OMRKey omrKey = new OMRKey();
            omrKey.setOmrkeyid(noOfQuestions);
            omrKey.setStrCorrectAnswers(strCorrectAnswers);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    db.omrKeyDao().insertOMRKey(omrKey);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    Toast.makeText(OMRKeyActivity.this, "Answers saved", Toast.LENGTH_LONG).show();
                }
            }.execute();
            finish();
        }
    }
}
