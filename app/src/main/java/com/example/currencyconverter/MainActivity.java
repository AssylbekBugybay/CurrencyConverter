package com.example.currencyconverter;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int JOB_ID = 101;

    // The adapter for the spinners
    private CurrencyAdapter adapter;

     //Share action provider for the share button
    private ShareActionProvider shareActionProvider;
    private ExchangeRateDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        database = new ExchangeRateDatabase();
        adapter = new CurrencyAdapter(database);

        //setting the adapter for both spinners
        Spinner spinner1 = findViewById(R.id.spinner1);
        Spinner spinner2 = findViewById(R.id.spinner2);
        spinner1.setAdapter(adapter);
        spinner2.setAdapter(adapter);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerService();
        }
        //Starting/scheduling a service on system boot
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                new IntentFilter("Daily currency update"));
    }


        @Override
        protected void onPause() {
            super.onPause();
            //Obtain preferences
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            //Get editing access
            SharedPreferences.Editor editor = prefs.edit();

            EditText amountToConvert = findViewById(R.id.enterAmountToConvert);
            String amount = amountToConvert.getText().toString();

            TextView convertedAmount = findViewById(R.id.convertedAmountView);
            String result = convertedAmount.getText().toString();

            Spinner spinner1 = findViewById(R.id.spinner1);
            Spinner spinner2 = findViewById(R.id.spinner2);

            int positionFrom = spinner1.getSelectedItemPosition();
            int positionTo = spinner2.getSelectedItemPosition();

            for (String currency : database.getCurrencies()) {
                String currencyRate = Double.toString(database.getExchangeRate(currency));
                editor.putString(currency, currencyRate);
            }
            //Store key-value-pair values
            editor.putString("amount", amount);
            editor.putString("result", result);
            editor.putInt("positionFrom", positionFrom);
            editor.putInt("positionTo", positionTo);

            //Persist data in XML
            editor.apply();
        }

        //To read preferences again, saves information when left the activity,
        // what is stored in resume will be shown again
        @Override
        protected void onResume() {
            super.onResume();

            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);

            String amount = prefs.getString("amount", "");
            String result = prefs.getString("result", "0.00");
            int positionFrom = prefs.getInt("positionFrom", 0);
            int positionTo = prefs.getInt("positionTo", 0);

            for (String currency : database.getCurrencies()) {
                String currencyRate = prefs.getString(currency, "0.00");

                if ("EUR".equals(currency)) {
                    currencyRate = "1.00";
                }

                if (!("0.00".equals(currencyRate))) {
                    database.setExchangeRate(currency, Double.parseDouble(currencyRate));
                }
            }
            EditText amountToConvert = findViewById(R.id.enterAmountToConvert);
            TextView convertedAmount = findViewById(R.id.convertedAmountView);


            Spinner spinner1 = findViewById(R.id.spinner1);
            Spinner spinner2 = findViewById(R.id.spinner2);

            amountToConvert.setText(amount);
            convertedAmount.setText(result);
            spinner1.setSelection(positionFrom);
            spinner2.setSelection(positionTo);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.my_menu, menu);

            MenuItem shareItem = menu.findItem(R.id.action_share);
            shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
            setShareText(null);

            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch(item.getItemId()) {
                case R.id.currencyListMenuEntry:
                    Intent intent = new Intent(MainActivity.this, CurrencyListActivity.class);
                    MainActivity.this.startActivity(intent);
                    return true;

                case R.id.refreshRatesMenuEntry:
                    new Thread((Runnable) new ExchangeRateUpdateRunnable(this)).start();
                    return true;

                default:
                    return super.onOptionsItemSelected(item);
            }
        }

         //Calculates the conversion between two currencies.
        // view - the view on which the click has happened

        public void onConvertClicked(View view) {
            EditText amountToConvert = findViewById(R.id.enterAmountToConvert);
            TextView convertedAmount = findViewById(R.id.convertedAmountView);


            if(amountToConvert.getText().toString().length() == 0) {
                convertedAmount.setText(String.format(Locale.getDefault(),"%.2f", 0.00));
                return;
            }

            double amount = Double.parseDouble(amountToConvert.getText().toString());

            Spinner spinner1 = findViewById(R.id.spinner1);
            String currencyFrom = (String) spinner1.getSelectedItem();

            Spinner spinner2 = findViewById(R.id.spinner2);
            String currencyTo = (String) spinner2.getSelectedItem();

            convertedAmount.setText(String.format(Locale.getDefault(),
                    "%.2f", adapter.getDatabase().convert(amount, currencyFrom, currencyTo)));

            String text = "Currency Converter says: \n" +
                    amountToConvert.getText().toString() + " " + currencyFrom + " are " +
                    convertedAmount.getText().toString() + " " + currencyTo;

            setShareText(text);
        }


         //Sets the message for sharing the app with others
         //Text message to set

    // Always call setShareText() to update the share intent when the currency conversion result changes!
        private void setShareText(String text) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            if(text != null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            }

            shareActionProvider.setShareIntent(shareIntent);
        }


         //Registers the job service which will daily update currencies
        public void registerService() {
            ComponentName serviceName = new ComponentName(this, RatesUpdateJobService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                JobInfo jobInfo = new JobInfo.Builder(JOB_ID, serviceName)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setRequiresDeviceIdle(false)
                        .setRequiresCharging(false)
                        // Will keep the job also between reboots
                        // Requires android.permission.RECEIVE_BOOT_COMPLETE (added in Manifest)
                        .setPersisted(true)
                        .setPeriodic(86_400_000).build();//milliseconds in 24hours

                JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

                if (scheduler.getPendingJob(JOB_ID) == null) {
                    scheduler.schedule(jobInfo);
                }
            }

        }


        //Receives the message from the RatesUpdateJobService class once the updates have been finished
        private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(MainActivity.this, "Rates updated",
                        Toast.LENGTH_LONG).show();
                adapter.notifyDataSetChanged();
            }
        };

        public ExchangeRateDatabase getDatabase() {
            return database;
        }

        public CurrencyAdapter getAdapter() {
            return adapter;
        }

}