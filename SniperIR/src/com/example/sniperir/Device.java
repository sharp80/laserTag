package com.example.sniperir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class Device extends Activity implements OnItemClickListener {

	private static final String LOG = "Device";
	private ArrayList<BluetoothDevice> devices;
	private List<Map<String, String>> listItems = new ArrayList<Map<String, String>>();
	private SimpleAdapter adapter;
	private Map<String, String> map = null;
	private ListView listView;
	private String DEVICE_NAME = "name";
	private String DEVICE_ADDRESS = "address";
	public static final int RESULT_CODE = 31;
	public final static String EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS";
	public final static String EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME";
	public final static String whiteList [] = {"C5:D0:FB:2E:FF:80", "C9:46:EC:41:4F:4E", "CE:37:57:57:9F:DA"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_list);

		setTitle("Device");

		listView = (ListView) findViewById(R.id.listView);

		devices = (ArrayList<BluetoothDevice>) MainActivity.mDevices;
		for (BluetoothDevice device : devices) {
			map = new HashMap<String, String>();
			for (int i = 0; i < whiteList.length; i++) {
				if (device.getAddress().equalsIgnoreCase(whiteList[i])) {
					map.put(DEVICE_NAME, device.getName());
					map.put(DEVICE_ADDRESS, device.getAddress());
					listItems.add(map);
					Log.i(LOG, "Added device: " + device.getName() + " with address: " + 
							device.getAddress()+"\n");
				}
				else {
					Log.i(LOG, "device: " + device.getName() + " with address: " + 
				device.getAddress() + " Is not in the bluetooth device list although exists!!!\n");
				}
					
			}
		}

		adapter = new SimpleAdapter(getApplicationContext(), listItems,
				R.layout.list_item, new String[] { "name", "address" },
				new int[] { R.id.deviceName, R.id.deviceAddr });
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		HashMap<String, String> hashMap = (HashMap<String, String>) listItems
				.get(position);
		String addr = hashMap.get(DEVICE_ADDRESS);
		String name = hashMap.get(DEVICE_NAME);

		Intent intent = new Intent(Device.this, ArtutActivity.class);
		intent.putExtra(EXTRA_DEVICE_ADDRESS, addr);
		intent.putExtra(EXTRA_DEVICE_NAME, name);
		startActivity(intent);
		MainActivity.instance.finish();
		finish();
	}
}
