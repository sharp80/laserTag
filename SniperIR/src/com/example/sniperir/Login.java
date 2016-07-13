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
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
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
        setContentView(R.layout.login);
        SharedPreferences sharedPref = this.getSharedPreferences(
                "laser", MODE_PRIVATE);
        if (sharedPref.getInt("nickName", 0) == 0)
        	initUi();	
        else
        	finish();
    }
    
    private JSONObject createJson(final String id, final String nickName) 
    		throws JSONException
    {
        JSONObject json = new JSONObject();
    	json.put("id", id);
        json.put("nickName", nickName);
    	return json;
    	
    }
    static public void sendJson(final JSONObject json, final Handler onPostEnd) {
        Thread t = new Thread() {

            public void run() {
               // Looper.prepare(); //For Preparing Message Pool for the child Thread
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
                HttpResponse response;

                try {
                    HttpPost post = new HttpPost("http://212.29.223.4/registrer");
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
		    	    	SharedPreferences sharedPref = getSharedPreferences(
		    	    			"laser", MODE_PRIVATE);
		    	    	SharedPreferences.Editor editor = sharedPref.edit();
		    	    	editor.putInt("nickName", 1);
		    	    	editor.commit();
		    	    	finish();
		    	    } 
		    	  }; 
		    	  
		    	JSONObject json;
				try {
					json = createJson(Secure.getString(v.getContext().getContentResolver(),
					            Secure.ANDROID_ID), et.getText().toString());
					sendJson(json, handler);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
    }
    
}