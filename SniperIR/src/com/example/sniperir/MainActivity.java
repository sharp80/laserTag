package com.example.sniperir;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.example.sniperir.Device;
import com.example.sniperir.MainActivity;
import com.example.sniperir.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String LOG = "Mainactivity";
	private BluetoothAdapter mBluetoothAdapter;
	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 3000;
	private Dialog mDialog;
	public static List<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();
	public static MainActivity instance = null;
	
	private final boolean mShouldRun = true; // If the Runnable should keep on running
	private final Handler mHandler = new Handler();
	private Runnable mUpdateHits;
	private int mMyLastHitsSize;
	private MainActivity mMyContext = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mMyContext  = this;
        
        if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		Button btn = (Button)findViewById(R.id.btn);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				scanLeDevice();

				showRoundProcessDialog(MainActivity.this, 
						R.layout.loading_process_dialog_anim);

				Timer mTimer = new Timer();
				mTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						Intent deviceListIntent = new Intent(getApplicationContext(),
								Device.class);
						startActivity(deviceListIntent);
						Log.i(LOG, "run(1) ");
						mDialog.dismiss();
					}
				}, SCAN_PERIOD);
				
				scanLeDevice();

				showRoundProcessDialog(MainActivity.this, R.layout.loading_process_dialog_anim);

				Timer mTimer2 = new Timer();
				mTimer2.schedule(new TimerTask() {

					@Override
					public void run() {
						Intent deviceListIntent = new Intent(getApplicationContext(),
								Device.class);
						startActivity(deviceListIntent);
						Log.i(LOG, "run(2) ");
						if (mDialog != null)
						mDialog.dismiss();
					}
				}, SCAN_PERIOD);
			}
		});
		
		
		
		// This runnable will schedule itself to run at 1 second intervals
        // if mShouldRun is set true.
        Log.d(LOG,"before mShouldRun=" + mShouldRun); // Call the method to actually update the clock

        mUpdateHits = new Runnable() {
                public void run() {
                        if(mShouldRun) {
                                checkMyHits(); // Call the method to check my hits
                                mHandler.postDelayed(mUpdateHits, 10000); // 1 second
                        }
                }
        };
        
        mHandler.post(mUpdateHits);
		
		instance = this;
		Intent intent = new Intent(this, Login.class);
		startActivity(intent);
    }
    
    private void checkMyHits(){
		Thread t = new Thread() {

			public void run() {

				InputStream inputStream = null;
				String result = "";
				String IMEI = Secure.getString(getContentResolver(),Secure.ANDROID_ID);
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
						Log.d("checkMyHits",  "jsonResult=" +jsonResult);
	    				
						int len = jsonResult.length();
						if (mMyLastHitsSize < len)
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
						.show();    	            }
    	        }
    	    );
    }
    
    
	public void showRoundProcessDialog(Context mContext, int layout) {
		OnKeyListener keyListener = new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_HOME
						|| keyCode == KeyEvent.KEYCODE_SEARCH) {
					return true;
				}
				return false;
			}
		};

		mDialog = new AlertDialog.Builder(mContext).create();
		mDialog.setOnKeyListener(keyListener);
		mDialog.show();
		mDialog.setContentView(layout);
	}

	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);

				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}.start();
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (device != null) {
						if (mDevices.indexOf(device) == -1)
							mDevices.add(device);
					}
				}
			});
		}
	};
}
