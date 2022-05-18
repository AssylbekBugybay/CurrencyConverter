package com.example.currencyconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;


public class CurrencyListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view);

        final ExchangeRateDatabase database = new ExchangeRateDatabase();
        final CurrencyAdapter adapter = new CurrencyAdapter(database);

        final ListView myListView = findViewById(R.id.myListView);


        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String currencyName = (String) adapter.getItem(i);
                String capital = database.getCapital(currencyName);

                // Capital location on the map
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0`?q=" + capital));
                startActivity(intent);

            }
        });

        myListView.setAdapter(adapter);
    }
}
