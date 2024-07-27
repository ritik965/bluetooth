package com.example.chatBluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class SharingData {

    public Context context;
    public Handler handler;
    private int statedata;
    private BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

    private static final String APP_NAME = "BluetoothChatApp";
    private static final UUID uuid = UUID.fromString("76145e9b-d1e7-46d6-a6aa-5c906c61da7c");

    public static final int NONE = 0;
    public static final int LISTEN = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;

    private ClientClass clientClass;
    private ServerClass serverClass;
    private ReadAndWrite readAndWrite;


    public SharingData(Context context, Handler handler){
        this.context=context;
        this.handler=handler;
        statedata=NONE;
    }

    public int getStatedata() {
        return statedata;
    }

    public synchronized void setStatedata(int statedata) {
        this.statedata = statedata;
        handler.obtainMessage(MainActivity.CHANGE,statedata,-1).sendToTarget();
    }

    private synchronized void start() {
        if (clientClass != null) {
            clientClass.cancel();
            clientClass = null;
        }
        if (readAndWrite != null) {
            readAndWrite.cancel();
            readAndWrite = null;
        }
        if (serverClass == null) {
            serverClass = new ServerClass();
            serverClass.start();
        }

        setStatedata(LISTEN);
    }

    public synchronized void stop() {
        if (clientClass != null) {
            clientClass.cancel();
            clientClass = null;
        }
        if (readAndWrite != null) {
            readAndWrite.cancel();
            readAndWrite = null;
        }
        if (serverClass != null) {
            serverClass.cancel();
            serverClass = null;
        }
        setStatedata(NONE);
    }

    public void connect(BluetoothDevice device) {
        if (statedata == CONNECTING) {
            clientClass.cancel();
            clientClass = null;
        }
        if (readAndWrite != null) {
            readAndWrite.cancel();
            readAndWrite = null;
        }
        clientClass = new ClientClass(device);
        clientClass.start();

        setStatedata(CONNECTING);
    }

    private class ClientClass extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ClientClass(BluetoothDevice device) {
            Toast.makeText(context, "in clientclass", Toast.LENGTH_SHORT).show();
            this.device = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }

            socket = tmp;
        }

        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                connFail();
                return;
            }

            synchronized (SharingData.this) {
                clientClass = null;
            }

            transferData(device,socket);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }

            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    e.printStackTrace();
                }
            }

            if (socket != null) {
                switch (statedata) {
                    case LISTEN:
                    case CONNECTING:
                        transferData(socket.getRemoteDevice(),socket);
                        break;
                    case NONE:
                    case CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private synchronized void transferData(BluetoothDevice device,BluetoothSocket socket) {
        if (clientClass != null) {
            clientClass.cancel();
            clientClass = null;
        }
        if (serverClass != null) {
            serverClass.cancel();
            serverClass = null;
        }

        if (readAndWrite != null) {
            readAndWrite.cancel();
            readAndWrite = null;
        }
        readAndWrite = new ReadAndWrite(socket);
        readAndWrite.start();

        Message message = handler.obtainMessage(MainActivity.DNAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setStatedata(CONNECTED);
    }

    private synchronized void connFail() {
        Message message = handler.obtainMessage(MainActivity.TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Cant connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        SharingData.this.start();
    }

    private class ReadAndWrite extends Thread{

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ReadAndWrite(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(MainActivity.READ, bytes, -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    LostConn();
                    SharingData.this.start();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void LostConn() {
        Message msg = handler.obtainMessage(MainActivity.TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);


        SharingData.this.start();
    }

    public void write(byte[] out) {
        ReadAndWrite r;
        synchronized (this) {
            if (statedata != CONNECTED)
                return;
            r = readAndWrite;
        }
        r.write(out);
    }

}
