package com.letssolvetogether.omr.truetime;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
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
            Log.i(TAG, "Please connect to internet and try again");
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(!TrueTime.isInitialized()){
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("In order to check free validity period of this App, we require you to turn on internet once and then try again");
            builder.setNeutralButton("Try Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int sumthin) {
                }
            });

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    new InitTrueTimeAsyncTask(context).execute();
                }
            });
            builder.show();

        }else{
            Date trueTimeDate =  TrueTime.now();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String date = formatter.format(trueTimeDate);
            Log.i(TAG,"TrueTime Intialized - " + date);

            //Caution: if lastFreeDate and trueTimeDate is same, it will exit.
            Date lastFreeDate = new Date(118,11,31);  //Dec 30, 2018
            boolean validPeriodExpired = trueTimeDate.before(lastFreeDate);

            if(!validPeriodExpired){
                //Toast.makeText(context,"Your free validity period is expired", Toast.LENGTH_SHORT).show();
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Your free validity period is expired.");
                builder.setNeutralButton("Ok",null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        System.exit(1);
                        Log.i(TAG,"Exit");
                    }
                });
                builder.show();
            }
        }
    }
}