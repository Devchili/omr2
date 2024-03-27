package com.letssolvetogether.omr.truetime;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.instacart.library.truetime.TrueTime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InitTrueTimeAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "InitTrueTimeAsyncTask";
    private Context context;

    public InitTrueTimeAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            TrueTime.build()
                    .withNtpHost("time.google.com")
                    .withLoggingEnabled(false)
                    .withSharedPreferencesCache(context)
                    .withConnectionTimeout(3_1428)
                    .initialize();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "something went wrong when trying to initialize TrueTime", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        Date trueTimeDate = TrueTime.now();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String date = formatter.format(trueTimeDate);
        Log.i(TAG, "TrueTime Initialized - " + date);

        // You may still want to perform some actions here based on trueTimeDate
        // For example:
        // if (trueTimeDate.before(someDate)) {
        //    // Do something
        // }

        // Since TrueTime is always considered initialized, there's no need for further checks
    }
}
