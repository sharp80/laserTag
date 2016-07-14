package com.example.sniperir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;

public class ArtutActivity extends Activity {
	private final static String TAG = "ArtutActivity";
	MediaPlayer player = null;
	public static final String EXTRAS_DEVICE = "EXTRAS_DEVICE";
	private String mDeviceName;
	private String mDeviceAddress;
	private RBLService mBluetoothLeService;
	private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	
    public void initAr() {
        
        setContentView(R.layout.main);
        
        FrameLayout arViewPane = (FrameLayout) findViewById(R.id.ar_view_pane);
        
        ArDisplayView arDisplay = new ArDisplayView(getApplicationContext(), this);
        arViewPane.addView(arDisplay);

        OverlayView arContent = new OverlayView(getApplicationContext());
        arViewPane.addView(arContent);
        
        arViewPane.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Log.d("ArtutActivity", "SHOOT sensed");
				player.start();
				sendShotCommandToBt();
			}
		});
   }

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				getGattService(mBluetoothLeService.getSupportedGattService());
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getByteArrayExtra(RBLService.EXTRA_DATA));
				String event = getEvent(intent.getByteArrayExtra(RBLService.EXTRA_DATA));
				execEvent(event);
				Log.d(TAG, "########gotEvent:"+ event );
			}
		}
	};
	private String getEvent(byte [] byteArray) {
		String data = new String(byteArray);
		return data;
	}
	
	private void execEvent(String event){
		if (event.equals("[C")) {
			getBeaconFromServer();
		} else if (event.startsWith("[S")) {
			Log.d(TAG, "########shooterBeaconId :"+ event );
			event = event.substring(2);
			sendKilledToServer(event);
			Intent killed = new Intent(getApplicationContext(),
					KilledActivity.class);
			startActivity(killed);
		}

	}
	
	private void getBeaconFromServer(){
		Handler handler = new Handler() { 
			@Override 
			public void handleMessage(Message msg) { 
				if (msg.arg1 > -1)
					sendBeaconToBt(msg.arg1);
			} 
		}; 
		getBeacon(Secure.getString(getContentResolver(),
				Secure.ANDROID_ID), handler);	
	}

	    public void getBeacon(final String id, final Handler handler)
	    {
	    	Thread t = new Thread() {

	    		public void run() {

	    			InputStream inputStream = null;
	    			String result = "";
	    			String url = "http://212.29.223.4/getbeacon?imei="+id;
	    			try {

	    				// create HttpClient
	    				HttpClient httpclient = new DefaultHttpClient();

	    				// make GET request to the given URL
	    				HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

	    				// receive response as inputStream
	    				inputStream = httpResponse.getEntity().getContent();
	    				
	    				String res =  convertInputStreamToString(inputStream);
	    				Log.d(TAG, res);
	    				JSONObject json = new JSONObject(res);
	    				Message msg = new Message();
	    				msg.arg1 = json.getInt("beacon");
	    				handler.sendMessage(msg);
	    				
	    			} catch (Exception e) {
	    				Log.d("InputStream", e.getLocalizedMessage());
	    			}
	    		}
	    	};

	    	t.start(); 
	    }
	    
	    public static String convertInputStreamToString(InputStream inputStream) throws IOException{
	        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
	        String line = "";
	        String result = "";
	        while((line = bufferedReader.readLine()) != null)
	            result += line;
	 
	        inputStream.close();
	        return result;
	 
	    }
	 
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		AssetFileDescriptor afd = null;
		try {
			afd = getAssets().openFd("shoot.mp3");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		player = new MediaPlayer();
		 try {
			player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 try {
			player.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initAr();
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	    
		/*tv = (TextView) findViewById(R.id.textView);
		tv.setMovementMethod(ScrollingMovementMethod.getInstance());
		et = (EditText) findViewById(R.id.editText);
		btn = (Button) findViewById(R.id.send);
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				BluetoothGattCharacteristic characteristic = map
						.get(RBLService.UUID_BLE_SHIELD_TX);

				String str = et.getText().toString();
				byte b = 0x00;
				byte[] tmp = str.getBytes();
				byte[] tx = new byte[tmp.length + 1];
				tx[0] = b;
				for (int i = 1; i < tmp.length + 1; i++) {
					tx[i] = tmp[i - 1];
				}

				characteristic.setValue(tx);
				mBluetoothLeService.writeCharacteristic(characteristic);

				et.setText("");
			}
		});*/

		Intent intent = getIntent();

		mDeviceAddress = intent.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
		mDeviceName = intent.getStringExtra(Device.EXTRA_DEVICE_NAME);

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent gattServiceIntent = new Intent(this, RBLService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	void sendEventToPluggable(String event) {
		BluetoothGattCharacteristic characteristic = map
				.get(RBLService.UUID_BLE_SHIELD_TX);

		byte b = 0x00;
		byte[] tmp = event.getBytes();
		byte[] tx = new byte[tmp.length + 1];
		tx[0] = b;
		for (int i = 1; i < tmp.length + 1; i++) {
			tx[i] = tmp[i - 1];
		}

		characteristic.setValue(tx);
		mBluetoothLeService.writeCharacteristic(characteristic);

		//et.setText("");
	}

	void sendBeaconToBt(int beaconId) {
		sendEventToPluggable(String.valueOf(beaconId));
	}
	
	public void sendShotCommandToBt() {
		sendEventToPluggable("S");
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService.close();

			System.exit(0);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();

		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mBluetoothLeService.disconnect();
		mBluetoothLeService.close();
		if (player != null) {
		player.release();
		}

		System.exit(0);
	}

	private void displayData(byte[] byteArray) {
		/*if (byteArray != null) {
			String data = new String(byteArray);
			tv.append(data);
			// find the amount we need to scroll. This works by
			// asking the TextView's internal layout for the position
			// of the final line and then subtracting the TextView's height
			final int scrollAmount = tv.getLayout().getLineTop(
					tv.getLineCount())
					- tv.getHeight();
			// if there is no need to scroll, scrollAmount will be <=0
			if (scrollAmount > 0)
				tv.scrollTo(0, scrollAmount);
			else
				tv.scrollTo(0, 0);
		}*/
	}

	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null)
			return;

		BluetoothGattCharacteristic characteristic = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
		map.put(characteristic.getUuid(), characteristic);

		BluetoothGattCharacteristic characteristicRx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,
				true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

		return intentFilter;
	}

	private void sendKilledToServer(final String shooterID){
		sendKilled(Secure.getString(getContentResolver(),
				Secure.ANDROID_ID), shooterID);
	}

	public void sendKilled(final String androidID, final String shooterID)
	{
		Thread t = new Thread() {

			public void run() {

				InputStream inputStream = null;
				String result = "";
				String url = "http://212.29.223.4/hitby?imei="+androidID + "&shooter=" + shooterID;
				Log.d("sendKilled","url="+ url);
				try {

					// create HttpClient
					HttpClient httpclient = new DefaultHttpClient();

					// make GET request to the given URL
					HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
					Log.d("sendKilled",  "respond=" +httpResponse.getStatusLine());

					// receive response as inputStream
					//	inputStream = httpResponse.getEntity().getContent();

					//	result =  convertInputStreamToString(inputStream);
					//	Log.d("sendKilled",  "res=" +result);


				} catch (Exception e) {
					Log.d("InputStream", e.getLocalizedMessage());
				}
			}
		};

		t.start(); 
	}
}