package com.example.chatBluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    FloatingActionButton on,off,searchDevice,send;
    ListView l1;
    EditText e1;
    BluetoothAdapter Badapter;
    SharingData sharingData;
    BluetoothDevice cdevice;

    private ArrayList<String> MessagesList;
    private ArrayAdapter<String> MessageAdapter;

    int SELECTDEVICES=102;

    public static final int CHANGE = 1;
    public static final int READ = 2;
    public static final int WRITE = 3;
    public static final int DNAME = 4;
    public static final int TOAST = 5;
    public static final String DEVICE = "DEVICE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        on=findViewById(R.id.fab1);
        off=findViewById(R.id.fab2);
        searchDevice=findViewById(R.id.fab3);
        send=findViewById(R.id.fab4);
        l1=findViewById(R.id.list1);
        e1=findViewById(R.id.edit);



        //getting its method
        Badapter=BluetoothAdapter.getDefaultAdapter();

        //turn the bluetooth
        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enable();
            }
        });



        //turn off bluetooth
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disable();
            }
        });

        MessagesList = new ArrayList<>();
        MessageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, MessagesList);
        l1.setAdapter(MessageAdapter);

        searchDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Badapter.startDiscovery();
                Intent intent=new Intent(MainActivity.this, FoundDevices.class);
                startActivityForResult(intent,SELECTDEVICES);

            }
        });

        sharingData=new SharingData(this,handler);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(e1.getText().toString());
                e1.setText("");
            }
        });

    }

    private void sendMessage(String message) {
        if (sharingData.getStatedata() != SharingData.CONNECTED) {
            Toast.makeText(this, "Oops Connection Lost try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            sharingData.write(send);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==SELECTDEVICES && resultCode==RESULT_OK){
            Badapter.cancelDiscovery();
            String address=data.getStringExtra("address");
            cdevice=Badapter.getRemoteDevice(address);
            sharingData.connect(cdevice);
        }
    }



    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            switch (msg.what){
                case CHANGE:
                    switch (msg.arg1) {
                        case SharingData.NONE:
                            set("not connected");
                            break;
                        case SharingData.LISTEN:
                            set("Not connected");
                            break;
                        case SharingData.CONNECTING:
                            set("connecting pls wait..");
                            break;
                        case SharingData.CONNECTED:
                            set("connected to device");
                            break;
                    }
                    break;

                case READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    String readMessage = new String(readBuf, 0, msg.arg1);
                    MessagesList.add(cdevice.getName()+":  " + readMessage);
                    MessageAdapter.notifyDataSetChanged();
                    break;

                case WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    MessagesList.add("Me: " + writeMessage);
                    MessageAdapter.notifyDataSetChanged();
                    break;

                case DNAME:
                    Toast.makeText(MainActivity.this,msg.getData().getString(DEVICE), Toast.LENGTH_SHORT).show();
                    break;

                case TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString("toast"),Toast.LENGTH_SHORT).show();

                    break;
            }
            return false;
        }
    });

    private void set(CharSequence s){
        getSupportActionBar().setSubtitle(s);
    }


    //for turning on bluetooth
    public void enable() {
        if(Badapter==null){
            Toast.makeText(this, "device not supported bluetooth", Toast.LENGTH_SHORT).show();
        }
        if(!Badapter.isEnabled()){
            Badapter.enable();
            Toast.makeText(this, "Bluetooth enabled Sucessfully", Toast.LENGTH_SHORT).show();
        }

    }

    //for turning off bluetooth
    public void disable() {
        if(Badapter.isEnabled()){
            Badapter.disable();
            Toast.makeText(this, "Bluetooth off", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sharingData != null)
            sharingData.stop();
    }
}







