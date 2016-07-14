package com.example.sniperir;

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class OverlayView extends View implements SensorEventListener {

    public static final String DEBUG_TAG = "OverlayView Log";
    String accelData = "Accelerometer Data";
    String compassData = "Compass Data";
    String gyroData = "Gyro Data";
    
    private final boolean mShouldRun = true; // If the Runnable should keep on running
	private final Handler mHandler = new Handler();
    private Runnable mUpdateHits;
	private int mMyLastHitsSize = -1;
	private Context mMyContext = null;
    
    public OverlayView(Context context) {
        super(context);    

        mMyContext = context;
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        boolean isAccelAvailable = sensors.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        boolean isCompassAvailable = sensors.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);      
        boolean isGyroAvailable = sensors.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        
        // This runnable will schedule itself to run at 1 second intervals
        // if mShouldRun is set true.
        Log.d(DEBUG_TAG,"before mShouldRun=" + mShouldRun); // Call the method to actually update the clock

        mUpdateHits = new Runnable() {
            public void run() {
                    if(mShouldRun) {
                            checkMyHits(); // Call the method to check my hits
                            mHandler.postDelayed(mUpdateHits, 10000); // 10 second
                    }
            }
	    };
	    
	    mHandler.post(mUpdateHits);
   }
    
    @Override
    protected void onDraw(Canvas canvas) {
       // Log.d(DEBUG_TAG, "onDraw");
        super.onDraw(canvas);
           
        // Draw something fixed (for now) over the camera view
        Paint contentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentPaint.setTextAlign(Align.CENTER);
        contentPaint.setTextSize(20);
        contentPaint.setColor(Color.RED);
       // canvas.drawCircle(canvas.getWidth()/2, canvas.getHeight()/2, 50, contentPaint);
        
        Resources res = mMyContext.getResources();

        Bitmap b = BitmapFactory.decodeResource(res, R.drawable.cross);
        //BitmapDrawable bitmap = new BitmapDrawable("@drawable/cross");
        //Bitmap b = new Bitmap("@drawable/cross");
		canvas.drawBitmap(b , canvas.getWidth()/3, canvas.getHeight()/6, contentPaint);
        //canvas.drawText(accelData, canvas.getWidth()/2, canvas.getHeight()/4, contentPaint);
        //canvas.draw(compassData, canvas.getWidth()/2, canvas.getHeight()/2, contentPaint);
        // canvas.drawText(gyroData, canvas.getWidth()/2, (canvas.getHeight()*3)/4, contentPaint);
     }
    
    private void checkMyHits(){
		Thread t = new Thread() {

			public void run() {

				InputStream inputStream = null;
				String result = "";
				String IMEI = Secure.getString(mMyContext.getContentResolver(),Secure.ANDROID_ID);
				String url = "http://212.29.223.4/myhits?imei="+ IMEI;
				Log.d("checkMyHits","url="+ url);
				try {

					// create HttpClient
					HttpClient httpclient = new DefaultHttpClient();

					// make GET request to the given URL
					HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
					Log.d("checkMyHits",  "respond=" +httpResponse.getStatusLine());

					// receive response as inputStream
						inputStream = httpResponse.getEntity().getContent();

						result =  ArtutActivity.convertInputStreamToString(inputStream);
						Log.d("checkMyHits",  "res=" +result);

						JSONArray jsonResult = new JSONArray(result);
	    				
						int len = jsonResult.length();
						if (mMyLastHitsSize != -1 && mMyLastHitsSize < len)
							showTost( jsonResult.getString(len-1));
							
						mMyLastHitsSize = jsonResult.length();
						
						Log.d("checkMyHits",  "mMyHitsSize=" +mMyLastHitsSize);

				} catch (Exception e) {
					Log.d("checkMyHits", e.getLocalizedMessage());
				}
			}
		};

		t.start(); 
	}
    
    public void showTost(String name)
    {
    	final String mName = name;
    	Handler handler = new Handler(Looper.getMainLooper());
    	handler.post(
    	        new Runnable()
    	        {
    	            @Override
    	            public void run()
    	            {
    	            	Toast.makeText(mMyContext, "You Killed " + mName, Toast.LENGTH_SHORT)
						.show();    	          
    	            }
    	        }
    	    );
    }
    

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        Log.d(DEBUG_TAG, "onAccuracyChanged");
        
    }

    public void onSensorChanged(SensorEvent event) {
    	
    	//Log.d(DEBUG_TAG, "onSensorChanged");
        
        StringBuilder msg = new StringBuilder(event.sensor.getName()).append(" ");
        for(float value: event.values)
        {
            msg.append("[").append(value).append("]");
        }
        
        switch(event.sensor.getType())
        {
        case Sensor.TYPE_ACCELEROMETER:
            accelData = msg.toString();
            break;
        case Sensor.TYPE_GYROSCOPE:
            gyroData = msg.toString();
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            compassData = msg.toString();
            break;              
        }
        
        
       this.invalidate();
        
    }

}
