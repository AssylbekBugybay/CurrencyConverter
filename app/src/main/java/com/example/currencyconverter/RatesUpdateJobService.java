package com.example.currencyconverter;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


// The class inherits from JobService and updates the currencies
// Runs in the background and refreshes every 24 hours
public class RatesUpdateJobService extends JobService {

    private RatesUpdateAsyncTask ratesUpdateAsyncTask = new RatesUpdateAsyncTask(this);

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        //Warning: Run on the UI thread! No long running process!
        ratesUpdateAsyncTask.execute(jobParameters);
        //true: work runs in background; false: work is finished
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        //the progress could be saved;   true:reschedule ASAP;   false: not important
        return false;
    }


    private static class RatesUpdateAsyncTask extends AsyncTask<JobParameters, Void, JobParameters> {

        private final JobService jobService;

        public RatesUpdateAsyncTask(JobService jobService) {
            this.jobService = jobService;
        }

        @Override
        protected JobParameters doInBackground(JobParameters... jobParameters) {
            final String queryString = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

            ExchangeRateDatabase database = new ExchangeRateDatabase();
            RatesUpdateNotifier notifier = new RatesUpdateNotifier(jobService);

            try {
                URL url = new URL(queryString);

                URLConnection connection = url.openConnection();

                XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setInput(connection.getInputStream(), connection.getContentEncoding());

                int eventType = parser.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("Cube".equals(parser.getName()) && parser.getAttributeCount() == 2) {
                            String currency = parser.getAttributeValue(null, "currency");
                            String sRate = parser.getAttributeValue(null, "rate");

                            double rate = Double.parseDouble(sRate);
                            database.setExchangeRate(currency, rate);
                        }
                    }

                    eventType = parser.next();
                }
             // A malformed URL has occurred. Either no legal protocol could be found in a
             // specification string or the string could not be parsed
            } catch (MalformedURLException e) {
                Log.e("MalformedURLException", "MalformedURLException");
              // Internet connection issues, invalid url...
            } catch (IOException e) {
                Log.e("IOException", "IOException");
                e.printStackTrace();
              // This exception is thrown to signal XML Pull Parser related faults
            } catch (XmlPullParserException e) {
                Log.e("XmlPullParserException", "XmlPullParserException");
            }

            SharedPreferences prefs = jobService.getSharedPreferences("Currency updates",
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            for (String currency : database.getCurrencies()) {
                String currencyRate = Double.toString(database.getExchangeRate(currency));
                editor.putString(currency, currencyRate);
            }

            editor.apply();

            sendMessage();
            notifier.showOrUpdateNotification();

            return jobParameters[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            //Notify JobService that the job is complete and rescheduling is not needed
            jobService.jobFinished(jobParameters, false);
        }

        // A message is sent to the main activity once the currencies have been updated
        private void sendMessage() {
            Intent intent = new Intent("Daily update of currencies");
            LocalBroadcastManager.getInstance(jobService).sendBroadcast(intent);
        }
    }
}
