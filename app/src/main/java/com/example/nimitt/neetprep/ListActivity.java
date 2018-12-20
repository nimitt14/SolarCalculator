package com.example.nimitt.neetprep;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListActivity extends Activity {

    private ListView lv;
    ArrayList<String> peopleList;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.listview);

        lv=(ListView)findViewById(R.id.listView);

        peopleList = new ArrayList<String>();

        peopleList=getIntent().getExtras().getStringArrayList("key");
          ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, peopleList);
        lv.setAdapter(arrayAdapter);

// register onClickListener to handle click events on each item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            // argument position gives the index of item which is clicked
            public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3)
            {
                Intent returnIntent=new Intent();
                returnIntent.putExtra("result",position);
                setResult(Activity.RESULT_OK,returnIntent);
                finish();

            }
        });
    }
}

