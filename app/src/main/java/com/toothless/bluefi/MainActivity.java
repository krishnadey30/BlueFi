package com.toothless.bluefi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    BluetoothAdapter mAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<BluetoothDevice>();
    List<WifiP2pDevice> peers= new ArrayList<WifiP2pDevice>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView BltDeviceLst,WifiDeviceLst;
    Button btnStartConnection;
    Button btnSend,WifiONOFF, DiscoverWifiDevices,BltONOFF,btnDiscoverable,DiscoverBltDev;
    BluetoothConnectionService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    EditText etext;
    TextView incomingMessages;
    StringBuilder messages;
    BluetoothDevice mBTDevice;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiverblt;
    IntentFilter mIntentFilter;
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    static final int MESSAGE_READ=1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendRecieve sendRecieve;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mAdapter.ACTION_STATE_CHANGED))
            {
                final int state= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,mAdapter.ERROR);
                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"on Receive: State OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG," mBroadcastReceiver1 STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG," mBroadcastReceiver1 STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"mBroadcastReceiver1 STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                BltDeviceLst.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ");

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mdevice = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                if(mdevice.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mdevice;
                }
                if(mdevice.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                if(mdevice.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Called");
        super.onDestroy();
//        unregisterReceiver(mBroadcastReceiver1);
//        unregisterReceiver(mBroadcastReceiver2);
//        unregisterReceiver(mBroadcastReceiver3);
//        unregisterReceiver(mBroadcastReceiver4);
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4,filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));
        exqListener();
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case MESSAGE_READ:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg = new String(readBuff,0,msg.arg1);
                    incomingMessages.setText(tempMsg);
                    byte[] bytes=tempMsg.getBytes(Charset.defaultCharset());
                    if(mBluetoothConnection!=null)
                        mBluetoothConnection.write(bytes);
                    break;
            }
            return true;
        }
    });

    private void exqListener() {
        WifiONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifiManager.isWifiEnabled())
                {
                    wifiManager.setWifiEnabled(false);
                    //WifiONOFF.setText("ON");
                }
                else
                {
                    wifiManager.setWifiEnabled(true);
                    //WifiONOFF.setText("Off");

                }
            }
        });

        btnDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");
                // Toast.makeText(getApplicationContext(),"Making device discoverable for 300 seconds",Toast.LENGTH_SHORT).show();
                Discoverable();
            }
        });

        DiscoverWifiDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Searching Devices",Toast.LENGTH_SHORT).show();
                        //connectionStatus.setText("Searching Devices");
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Cannot Search Devices",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        BltDeviceLst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mAdapter.cancelDiscovery();
                Log.d(TAG, "onItemClick:  You Clicked a device");
                String deviceName=mBTDevices.get(i).getName();
                String deviceAddress = mBTDevices.get(i).getAddress();

                Log.d(TAG, "onItemClick: deviceName = "+deviceName);
                Log.d(TAG, "onItemClick: deviceAddress = "+deviceAddress);

                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
                {
                    Log.d(TAG, "Trying to pair with  "+ deviceName);
                    mBTDevices.get(i).createBond();


                    mBTDevice = mBTDevices.get(i);
                    mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
                }
            }
        });
        WifiDeviceLst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config=new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"You are Connected to "+device.deviceName,Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Not able to Connect to"+device.deviceName,Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        BltONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Enabling/Disabling Bluetooth");
                incomingMessages.setText("");
                enableDisableBT();
            }
        });

        DiscoverBltDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                incomingMessages.setText("");
                Log.d(TAG, "DiscoverBltDev onClick: Looking for unpaired Devices");
                FindDevices();
            }
        });

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = etext.getText().toString();
                byte[] bytes = msg.getBytes(Charset.defaultCharset());



                Log.d(TAG, "Checking send receive"+sendRecieve);
                Log.d(TAG, "checking mbluettooth"+mBluetoothConnection);
                if(mBluetoothConnection!=null)
                    mBluetoothConnection.write(bytes);
                if(sendRecieve!=null)
                  sendRecieve.write(msg.getBytes());
                etext.setText("");
            }
        });

    }


    private void initialWork() {
        WifiONOFF = (Button) findViewById(R.id.WifiONOFF);
        DiscoverWifiDevices = (Button) findViewById(R.id.DiscoverWifiDevices);
        WifiDeviceLst = (ListView) findViewById(R.id.WifiDeviceLst);
        BltONOFF=(Button) findViewById(R.id.BltONOFF);
        btnDiscoverable = (Button) findViewById(R.id.BltDiscovery);
        DiscoverBltDev = (Button) findViewById(R.id.DiscoverBltDev);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiverblt = new WifiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        btnStartConnection=(Button) findViewById(R.id.CreateBltConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etext = (EditText)findViewById(R.id.editText);
        incomingMessages = (TextView) findViewById(R.id.incomingMessage);
        messages = new StringBuilder();
        BltDeviceLst = (ListView) findViewById(R.id.BltDeviceLst);
        mBTDevices = new ArrayList<BluetoothDevice>();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            incomingMessages.setText(text);
            if(sendRecieve!=null)
                sendRecieve.write(text.getBytes());
        }
    };

    private void startConnection() {
        startBTConnection(mBTDevice,MY_UUID_INSECURE );
    }


    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);
    }



    public void enableDisableBT()
    {
        if(mAdapter == null)
        {
            Log.d(TAG," Sorry Your Device Does NOT Support Bluetooth");
        }
        if(!mAdapter.isEnabled())
        {
            Log.d(TAG, "enableDisableBT: enabling BT");
            mBTDevices = new ArrayList<BluetoothDevice>();
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1,BTIntent);
        }
        if(mAdapter.isEnabled())
        {
            Log.d(TAG, "enableDisableBT: Disabling BT");
            BltDeviceLst.setAdapter(null);
            mAdapter.disable();
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1,BTIntent);
        }
    }
    public void Discoverable() {

        if(mAdapter.isEnabled()) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);

           /* IntentFilter intentFilter = new IntentFilter(mAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(mBroadcastReceiver2, intentFilter);*/
        }
        else
        {
            Toast.makeText(getApplicationContext(),"Bluetooth is Turned Off. Please Turn it On",Toast.LENGTH_SHORT).show();
        }

    }
    public void FindDevices()
    {
        if(mAdapter.isEnabled())
        {
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
                Log.d(TAG, "FindDevices: Cancel Discovery");
                BltDeviceLst.setAdapter(null);
                mBTDevices = new ArrayList<BluetoothDevice>();
                checkBTPermissions();
                mAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            }
            if (!mAdapter.isDiscovering()) {
                checkBTPermissions();
                mAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(),"Can't search Devices. Bluetooth is Turned Off. Please Turn it On",Toast.LENGTH_SHORT).show();
        }
    }
    WifiP2pManager.PeerListListener peerListListener=new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if(!peerList.getDeviceList().equals(peers))
            {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList())
                {
                    deviceNameArray[index]=device.deviceName;
                    deviceArray[index]=device;
                    index++;
                }

                ArrayAdapter<String> adapter= new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                WifiDeviceLst.setAdapter(adapter);
            }
            if(peers.size()==0)
            {
                Toast.makeText(getApplicationContext(),"No Devices Found",Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener= new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress= wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            {
                Toast.makeText(getApplicationContext(),"Host",Toast.LENGTH_SHORT).show();
                serverClass = new ServerClass();
                serverClass.start();
            }
            else if(wifiP2pInfo.groupFormed)
            {
                Toast.makeText(getApplicationContext(),"Client",Toast.LENGTH_SHORT).show();
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };


    @Override
    protected void onPostResume() {
        super.onPostResume();
        registerReceiver(mReceiverblt,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiverblt);
    }

    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendRecieve extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendRecieve(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null)
            {
                try {
                    bytes=inputStream.read(buffer);
                    if(bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void write(final byte[] bytes)
        {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: checking the outputstream of wifi"+outputStream);
                    try{
                        try {
                            outputStream.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("deb", "run: exception");
                        e.printStackTrace();
                    }
                }
            });
            thread.start();

        }
    }
    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress)
        {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void checkBTPermissions() {
        Log.d(TAG, "checkBTPermissions: "+Build.VERSION.SDK_INT);
        Toast.makeText(getApplicationContext(),"checkBTPermissions: "+Build.VERSION.SDK_INT,Toast.LENGTH_SHORT).show();
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }



}
