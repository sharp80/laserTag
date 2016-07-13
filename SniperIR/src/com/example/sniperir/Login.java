package com.example.sniperir;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Login extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
    }
    
    
    protected void sendJson(final String id, final String nickName, final Handler onPostEnd) {
        Thread t = new Thread() {

            public void run() {
               // Looper.prepare(); //For Preparing Message Pool for the child Thread
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
                HttpResponse response;
                JSONObject json = new JSONObject();

                try {
                    HttpPost post = new HttpPost("http://hitap-oferl.redbend.com:8080/registrer");
                    json.put("id", id);
                    json.put("nickName", nickName);
                    StringEntity se = new StringEntity( json.toString());  
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    post.setEntity(se);
                    response = client.execute(post);
                    Message msg = new Message();
                    msg.obj = response.toString();
                    onPostEnd.sendMessage(msg);
                    /*Checking response */
                    if(response!=null){
                        InputStream in = response.getEntity().getContent(); //Get the data in the entity
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                //    createDialog("Error", "Cannot Estabilish Connection");0
                }
                Message msg = new Message();
                msg.obj = null;
                onPostEnd.sendMessage(msg);
              
              //  Looper.loop(); //Loop in the message queue
            }
        };

        t.start();      
    }
    
    public void initUi(){
    	Button button = (Button)findViewById(R.id.submit);
    	button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	EditText et = (EditText)findViewById(R.id.nickname);
		    	Handler handler = new Handler() { 
		    	    @Override 
		    	    public void handleMessage(Message msg) { 
		    	    	finish();
		    	    } 
		    	  }; 
				sendJson(Secure.getString(v.getContext().getContentResolver(),
                        Secure.ANDROID_ID), et.getText().toString(), handler);
			}
		});
    }
    
}