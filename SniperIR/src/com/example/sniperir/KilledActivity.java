package com.example.sniperir;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class KilledActivity extends Activity {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.killed_activity);
        
        ImageView image = (ImageView) findViewById(R.id.imageView1);
        image.setImageResource(R.drawable.dead);
    }
}
