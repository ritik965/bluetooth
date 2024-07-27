package com.example.chatBluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Set;

public class FoundDevices extends AppCompatActivity {


    ListView pairedList,availableList;
    FloatingActionButton close,rescan;
    BluetoothAdapter Badapter=BluetoothAdapter.getDefaultAdapter();
    ArrayAdapter<String> pairedAdapter;
    ArrayAdapter<String> availbleAdapter;
    Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_devices);

        close=findViewById(R.id.fabu1);

        pairedList=findViewById(R.id.pairedDevices);
        availableList=findViewById(R.id.availableDevices);

        context=this;

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });



        pairedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedList.setAdapter(pairedAdapter);
        Set<BluetoothDevice> pairedDevices = Badapter.getBondedDevices();


        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedAdapter.add("No paired devices");
        }


        showDeviceList();

        clickListeners();


    }

    private void showDeviceList() {

        Badapter.startDiscovery();

        //registering the broadcast
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(foundDevice, filter);

        filter=new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(foundDevice,filter);

        availbleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        availableList.setAdapter(availbleAdapter);


    }



    private final BroadcastReceiver foundDevice = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    availbleAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (availbleAdapter.getCount() == 0) {
                    availbleAdapter.add("No devices found");
                }
            }
        }
    };



    private void clickListeners() {

        availableList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Badapter.cancelDiscovery();
                String str = ((TextView) view).getText().toString();
                String address = str.substring(str.length() - 17);

                Intent intent=new Intent();
                intent.putExtra("address",address);
                setResult(RESULT_OK,intent);
                finish();

            }
        });

        pairedList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Badapter.cancelDiscovery();
                String str = ((TextView) view).getText().toString();
                String address = str.substring(str.length() - 17);

                Intent intent=new Intent();
                intent.putExtra("address",address);
                setResult(RESULT_OK,intent);
                finish();

            }
        });


    }


}