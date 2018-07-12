package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Button getdata_btn, connected_btn, browser_btn;

    private TextView mConnectionState,mBattery;
    private TextView mHRField,mRRField;
    private TextView Server_Status;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private String MyID;


    private LineData HR_DATA = new LineData();
    private LineChart chart_line;


    private Socket ClientSocket;

    private boolean Connected_Status = false, GetDatastatus = false;

    private SQLiteManager SQLiteManag;
    private final static int SQLiteVersion = 1;
    private final static String SQLiteName = "HRV_DATA.db";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = "";
                data += intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                data = data + "," + intent.getStringExtra(BluetoothLeService.RR_INTERVAL);
                displayData(data);
            }
        }
    };

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mHRField.setText("No data");
        mRRField.setText("No data");
        if(SQLiteManag.exportDB())
            Toast.makeText(this, "DB Export !!", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "DB Export false", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        SQLiteManag = new SQLiteManager(this, SQLiteName, null, SQLiteVersion);
        GetTime();
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mBattery = (TextView) findViewById(R.id.battery_view);
        mHRField = (TextView) findViewById(R.id.hr_value);
        mRRField = (TextView) findViewById(R.id.rr_value);
        Server_Status = (TextView) findViewById(R.id.server_status);

        getdata_btn = (Button) findViewById(R.id.getdata_btn);
        connected_btn = (Button) findViewById(R.id.connected_btn);
        browser_btn = (Button) findViewById(R.id.browser_btn);

        getdata_btn.setOnClickListener(Button_Listener);
        connected_btn.setOnClickListener(Button_Listener);
        browser_btn.setOnClickListener(Button_Listener);

        chart_line = (LineChart)findViewById(R.id.chart_line);

        chart_line.setVisibility(View.GONE);

        HR_DATA = new LineData();

        chart_line.clear();

        chart_line.setData(HR_DATA);

        File file = new File(this.getFilesDir().getPath() + "/Config.properties");
        if (!file.exists()){
            Toast.makeText(this, "ID建立失敗，請重啟APP", Toast.LENGTH_SHORT).show();
            finish();
        }
        else{
            Log.d("Config","Config is exists");
            Get_App_ID();
        }

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                mBattery.setText("No data");
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            final String[] SplitData = data.split(",");
            mHRField.setText(SplitData[0]);
            InsertData(SplitData[1]);
            putChartData(Integer.valueOf(SplitData[0]));
            mRRField.setText(SplitData[1]);
            if (Connected_Status){
                new Send_Data().execute(SplitData[1]);
            }
        }
    }

    private void putChartData(int data){

        LineData XData = chart_line.getData();

        chart_line.setVisibility(View.VISIBLE);

        if (XData != null){
            ILineDataSet set = XData.getDataSetByIndex(0);

            if (set == null){
                set = createSet();
                XData.addDataSet(set);
            }
            chart_line.getXAxis().setEnabled(false);
            chart_line.getDescription().setEnabled(false);
            XData.addEntry(new Entry(set.getEntryCount(),data), 0);
            XData.notifyDataChanged();
            chart_line.notifyDataSetChanged();
            chart_line.moveViewToX(XData.getEntryCount());

        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Heart Rate");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.BLACK);
        set.setLineWidth(2f);
        set.setCircleRadius(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.BLACK);
        set.setValueTextSize(9f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        return set;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private Button.OnClickListener Button_Listener = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.getdata_btn:
                    if (!GetDatastatus) {
                        int a, b, x = -1, y = -1;
                        String get_HRV_UUID, HRV_UUID = "00002a37-0000-1000-8000-00805f9b34fb";
                        boolean HRV_Service = false;

                        chart_line.clear();
                        chart_line.invalidate();
                        HR_DATA.clearValues();

                        chart_line.setData(HR_DATA);

                        a = mGattCharacteristics.size();

                        for (int i = 0; i < a - 1; i++) {
                            b = mGattCharacteristics.get(i).size();
                            for (int j = 0; j < b - 1; j++) {
                                get_HRV_UUID = mGattCharacteristics.get(i).get(j).getUuid().toString();
                                if (HRV_UUID.equals(get_HRV_UUID)) {
                                    HRV_Service = true;
                                    x = i;
                                    y = j;
                                }
                            }
                        }

                        if (HRV_Service) {
                            GetDatastatus = true;
                            getdata_btn.setText("Cancel");
                            if (mGattCharacteristics != null && x != -1 && y != -1) {
                                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(x).get(y);
                                final int charaProp = characteristic.getProperties();
                                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                    // If there is an active notification on a characteristic, clear
                                    // it first so it doesn't update the data field on the user interface.
                                    if (mNotifyCharacteristic != null) {
                                        mBluetoothLeService.setCharacteristicNotification(
                                                mNotifyCharacteristic, false);
                                        mNotifyCharacteristic = null;
                                    }
                                    mBluetoothLeService.readCharacteristic(characteristic);
                                }
                                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                    mNotifyCharacteristic = characteristic;
                                    mBluetoothLeService.setCharacteristicNotification(
                                            characteristic, true);
                                }
                            }
                        } else {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceControlActivity.this);
                            dialog.setTitle("錯誤訊息");
                            dialog.setMessage("該設備不支援心率偵測");
                            dialog.setNegativeButton("確認", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                            dialog.show();
                        }
                    }
                    else {
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        clearUI();
                        GetDatastatus = false;
                        getdata_btn.setText("Get Data");
                    }
                    break;
                /*------------------------------------------------------------------------------------*/
                case R.id.connected_btn:
                    if (!Connected_Status) {
                        new Connection_Server().execute("");
                    }
                    else
                        new DisConnection().execute("");
                    break;
                /*------------------------------------------------------------------------------------*/
                case R.id.browser_btn:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://isuhrv.azurewebsites.net/DataView?SearchString=" + MyID ));
                    startActivity(browserIntent);
                    break;
            }
        }
    };

    private class Connection_Server extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
            try{
                Log.d("ServerConnected","Connecting");
                String HOST_IP = "140.127.196.75";
                int PORT = 6666;
                ClientSocket = new Socket();
                ClientSocket.connect(new InetSocketAddress(HOST_IP,PORT),5000);
                Log.d("ServerConnected","Connected: " + ClientSocket.getInetAddress().toString());
            }
            catch (Exception e){
                Log.d("ConnectedException",e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String value){
            if (ClientSocket.isConnected()){
                Connected_Status = true;
                connected_btn.setText("Disconnected");
                Server_Status.setText("Connected");
                new Send_ID().execute("");
            }
        }
        @Override
        protected void onPreExecute() {
        }
    }

    private class DisConnection extends AsyncTask<String,String,String>{
        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected())
                    ClientSocket.close();
            }
            catch (Exception e){
                Log.d("ConnectedException",e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String value){
            if (ClientSocket.isClosed()){
                Log.d("ServerConnected","Disconnected: " + ClientSocket.getInetAddress().toString());
                Connected_Status = false;
                connected_btn.setText("Connected");
                Server_Status.setText("DisConnected");
            }
        }
        @Override
        protected void onPreExecute() {
        }
    }

    private class Send_ID extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected()){
                    if (!params[0].equals("null")) {
                        byte[] Data = ("DeviceID:" + MyID + ";").getBytes("UTF-8");
                        OutputStream Os = ClientSocket.getOutputStream();
                        Os.write(Data);
                        Log.d("Send_ID", new String(Data));
                    }
                }
            }
            catch (Exception e){
                Log.d("SendException",e.getMessage());
            }
            return null;
        }
    }

    private class Send_Data extends AsyncTask<String,String,String>{

        @Override
        protected String doInBackground(String... params) {
            try{
                if (ClientSocket.isConnected()){
                    if (!params[0].equals("null")) {
                        byte[] Data = ("DeviceData:" + params[0] + "," + GetTime() + ";").getBytes("UTF-8");
                        OutputStream Os = ClientSocket.getOutputStream();
                        Os.write(Data);
                        Log.d("Send_Data", params[0]);
                    }
                }
            }
            catch (Exception e){
                Log.d("SendException",e.getMessage());
            }
            return null;
        }
    }

    private void Get_App_ID(){
        Properties prop = new Properties();
        String propertiesPath = this.getFilesDir().getPath() + "/Config.properties";
        try {
            FileInputStream inputStream = new FileInputStream(propertiesPath);
            prop.load(inputStream);
            Log.d("ID_Get",prop.getProperty("APP_ID"));
            MyID = prop.getProperty("APP_ID");
        } catch (IOException e) {
            System.err.println("Failed to open app.properties file");
            e.printStackTrace();
        }
    }

    private void InsertData(String Data){
        if (!Data.equals("null"))
        {
            ContentValues content = new ContentValues();
            content.put("DeviceData", Data);
            content.put("StoreDate", GetTime());
            long id = SQLiteManag.getWritableDatabase().insert("RR_interval", null, content);
            Log.d("ADD", id+"");
        }
    }

    private String GetTime(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
        Date time = new Date(System.currentTimeMillis());
        return format.format(time);
    }



}
