package com.eap.sdy61.ge4.eva_b.eapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.tool_bar); // Attaching the layout to the toolbar object
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_toTabs) {
            Intent intent = new Intent(MainActivity.this, TabsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

//    public void navToTabs(View view)
//    {
//        Intent intent = new Intent(FromActivity.this, ToActivity.class);
//        startActivity(intent);
//    }
//
//    public void sendMessage(View view)
//    {
//        Intent intent = new Intent(FromActivity.this, ToActivity.class);
//        startActivity(intent);
//    }

}
