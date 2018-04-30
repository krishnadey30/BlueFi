package com.toothless.bluefi;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    public static final String TAG = "BluetoothConnection";
    public static final String appName="MYAPP";
    private static final UUID MY_UUID_INSECURE =UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final BluetoothAdapter mAdapter;
    Context myContext;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread myConnectThread;
    private BluetoothDevice myDevice;
    private UUID deviceUUID;
    ProgressDialog myProgressDialog;
    private ConnectedThread myConnectedThread;

    public BluetoothConnectionService(Context context) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        myContext = context;
        start();
    }

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mySocket;

        public AcceptThread()
        {
            BluetoothServerSocket temp=null;
            try{
                temp=mAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up the Server using "+ MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOException "+e.getMessage());
            }
            mySocket = temp;
        }

        public void run() {
            Log.d(TAG, "run: Accept Thread run");
            BluetoothSocket socket = null;

            try {
                Log.d(TAG, "run: RECON server Socket Start");
                socket = mySocket.accept();
                Log.d(TAG, "run: RECON server socket accepted connection");
            } catch (IOException e) {
                Log.e(TAG, "run: IOException "+e.getMessage());
            }
            if(socket!=null)
            {
                connected(socket,myDevice);
            }
            Log.i(TAG, "End : mAccept Thread");
        }

        public void cancel(){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        Log.d(TAG, "Cancel: Cancelling AcceptThread");
                        try {
                            mySocket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed" + e.getMessage());
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("deb","run: exception cancel Connect Thread");
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mySocket;


        public ConnectThread(BluetoothDevice device,UUID uuid)
        {
            Log.i(TAG, "ConnectThread: started");
            myDevice = device;
            deviceUUID = uuid;
            Log.d(TAG, "ConnectThread: DEvice UUID "+deviceUUID);
        }
        public void run()
        {
            BluetoothSocket temp = null;
            mAdapter.cancelDiscovery();
            Log.i(TAG, "Running myConnectThread");
            try {
                Log.d(TAG, "ConnectThread: Trying to connect InsecureRfCommSocket using UUID:" + MY_UUID_INSECURE);
                temp = myDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }
            mySocket = temp;

            Log.d(TAG, "run: mysocket in connect thread "+mySocket);
            //mAdapter.cancelDiscovery();
            try {
                Log.d(TAG, "run: mysocket in connect thread "+mySocket);
                mySocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                Log.e(TAG, "run: connect Thread exception "+e.getMessage() );
                try {
                    mySocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "myConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.e(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }
            connected(mySocket, myDevice);

        }
        public void cancel(){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        try {
                            Log.d(TAG, "cancel: Closing Client Socket ");
                            mySocket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "cancel:close() of mySocket in ConnectThread failed " + e.getMessage());
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("deb","run: exception cancel Connected Thread");
                        e.printStackTrace();
                    }
                }
            });
            t.start();

        }
    }


    public synchronized void start(){
        Log.d(TAG, "start ");

        if(myConnectThread != null){
            myConnectThread.cancel();
            myConnectThread = null;
        }
        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device,UUID uuid){
        Log.d(TAG, "startClient: Started.");
        myProgressDialog = ProgressDialog.show(myContext, "Connecting Bluetooth ", "Please Wait...", true);
        myConnectThread = new ConnectThread(device, uuid);
        myConnectThread.start();
    }


    private class ConnectedThread extends Thread{
        private final BluetoothSocket mySocket;
        private final InputStream myInStream;
        private final OutputStream myOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "ConnectedThread: Starting");

            mySocket = socket;
            Log.d(TAG, "ConnectedThread: socket "+mySocket);
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                myProgressDialog.dismiss();
            }
            catch (NullPointerException e){
                e.printStackTrace();
            }


            try {
                tmpIn = mySocket.getInputStream();
                tmpOut=mySocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            myInStream = tmpIn;
            myOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true){
                try {
                    bytes = myInStream.read(buffer);
                    String incomingMessage = new String(buffer,0,bytes);
                    Log.d(TAG, "InputStream: "+incomingMessage);

                    Intent incomingMessageIntent=new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage",incomingMessage);
                    LocalBroadcastManager.getInstance(myContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading inputstream "+e.getMessage() );
                    break;
                }
            }
        }


        public void write(final byte[] bytes ){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "Writing to outputStream: " + text);
            Thread t =new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "checking Outstream bluetooth"+myOutStream);
                    try{
                        try {

                            myOutStream.write(bytes);
                        } catch (IOException e) {
                            Log.e(TAG, "write: Error writing to outputstream "+e.getMessage() );
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("deb","run: exception cancel Connect Thread");
                        e.printStackTrace();
                    }
                }
            });
            t.start();

        }
        public void cancel()
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        try {
                            mySocket.close();
                        } catch (IOException e) {

                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("deb","run: exception cancel Connect Thread");
                        e.printStackTrace();
                    }
                }
            });
            t.start();

        }
    }

    private void connected(final BluetoothSocket mySocket, BluetoothDevice myDevice) {
        Log.d(TAG, "Connected... Starting...");
        myConnectedThread = new ConnectedThread(mySocket);
        myConnectedThread.start();

    }

    public void write(byte[] out ){

        ConnectedThread r;
        synchronized (this) {
            r = myConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        // Create temporary object

        Log.d(TAG, "write: Write Called.");
    }

}
