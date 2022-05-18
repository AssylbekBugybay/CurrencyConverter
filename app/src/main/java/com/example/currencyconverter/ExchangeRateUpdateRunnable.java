package com.example.currencyconverter;

import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

// The class updates the interface Runnable which updates the rates upon the user request
public class    ExchangeRateUpdateRunnable implements Runnable{
    private MainActivity mainActivity;
    private RatesUpdateNotifier notifier;

    public ExchangeRateUpdateRunnable(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.notifier = new RatesUpdateNotifier(mainActivity);
    }

    @Override
    public void run() {
        final String queryString = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    try {
        URL url = new URL(queryString);

        URLConnection connection = url.openConnection();

        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(connection.getInputStream(),connection.getContentEncoding());

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if ("Cube".equals(parser.getName()) && parser.getAttributeCount() == 2) {
                    String currency = parser.getAttributeValue(null, "currency");
                    String sRate = parser.getAttributeValue(null, "rate");

                    double rate = Double.parseDouble(sRate);
                    mainActivity.getDatabase().setExchangeRate(currency, rate);
                }
            }
            eventType = parser.next();
        }

    // A malformed URL has occurred. Either no legal protocol could be found in a
    // specification string or the string could not be parsed
    } catch (MalformedURLException e) {
        Log.e("MalformedURLException", "MalformedURLException");
    } catch (IOException e) {
        // Internet connection issues, invalid url...
        Log.e("IOException", "IOException");
        e.printStackTrace();
        // This exception is thrown to signal XML Pull Parser related faults
    } catch (XmlPullParserException e) {
        Log.e("XmlPullParserException", "XmlPullParserException");
    }

    // A toast can only be started from the UI thread. This means that new Runnable needs to be created
    // A new Runnable is passed to the UI thread for execution
    mainActivity.runOnUiThread(new Runnable() {
        @Override
            public void run() {
                Toast.makeText(mainActivity.getApplicationContext(),
                        "The rates have been successfully updated", Toast.LENGTH_LONG).show();
                mainActivity.getAdapter().notifyDataSetChanged();
                notifier.showOrUpdateNotification();
            }
        });
    }
}
