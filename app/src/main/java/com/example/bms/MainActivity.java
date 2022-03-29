package com.example.bms;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import static android.R.layout.*;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<String> pairedDeviceArrayList;
    ListView listViewPairedDevice;
    FrameLayout ButPanel;
    ArrayAdapter<String> pairedDeviceAdapter;
    private UUID myUUID;
    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    private StringBuilder sb = new StringBuilder();
    public TextView textInfo, B1,B2,B3,B4,B5,B6,B7,B8,I,Ah;
    public String SendStr="R1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
        textInfo = (TextView)findViewById(R.id.textInfo);
        B1 = (TextView)findViewById(R.id.B1);
        B2 = (TextView)findViewById(R.id.B2);
        B3 = (TextView)findViewById(R.id.B3);
        B4 = (TextView)findViewById(R.id.B4);
        B5 = (TextView)findViewById(R.id.B5);
        B6 = (TextView)findViewById(R.id.B6);
        B7 = (TextView)findViewById(R.id.B7);
        B8 = (TextView)findViewById(R.id.B8);
        I = (TextView)findViewById(R.id.I);
        Ah = (TextView)findViewById(R.id.Ah);
        listViewPairedDevice = (ListView)findViewById(R.id.pairedlist);
        ButPanel = (FrameLayout) findViewById(R.id.ButPanel);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String stInfo = bluetoothAdapter.getName() + " " + bluetoothAdapter.getAddress();
        //textInfo.setText(String.format("Это устройство: %s", stInfo));
        textInfo.setText(String.format("Выберите устройство:"));
    } // END onCreate

    @Override
    protected void onStart() { // Запрос на включение Bluetooth
        super.onStart();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        setup();
        Timer timer = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                if(myThreadConnected!=null) {
                    String cmd = SendStr;
                    byte[] bytesToSend = cmd.getBytes();
                    myThreadConnected.write(bytesToSend );
                }
            }
        };
        timer.scheduleAtFixedRate(t,500, 500);
    }

    private void setup() { // Создание списка сопряжённых Bluetooth-устройств
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) { // Если есть сопряжённые устройства
            pairedDeviceArrayList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            }
            pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);
            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() { // Клик по нужному устройству
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    listViewPairedDevice.setVisibility(View.GONE); // После клика скрываем список
                    String  itemValue = (String) listViewPairedDevice.getItemAtPosition(position);
                    String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес
                    BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(MAC);
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                    myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() { // Закрытие приложения
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){ // Если разрешили включить Bluetooth, тогда void setup()
            if(resultCode == Activity.RESULT_OK) {
                setup();
            }
            else { // Если не разрешили, тогда закрываем приложение
                Toast.makeText(this, "BlueTooth не включён", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
        private BluetoothSocket bluetoothSocket = null;
        private ThreadConnectBTdevice(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() { // Коннект
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            }
            catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Нет соединения, проверьте Bluetooth-устройство с которым хотите соединиться!", Toast.LENGTH_LONG).show();
                        listViewPairedDevice.setVisibility(View.VISIBLE);
                    }
                });
                try {
                    bluetoothSocket.close();
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ButPanel.setVisibility(View.VISIBLE); // открываем панель с кнопками
                    }
                });
                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }

        public void cancel() {
            Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();
            try {
                bluetoothSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    } // END ThreadConnectBTdevice:

    private class ThreadConnected extends Thread {    // Поток - приём и отправка данных
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private String sbprint;

        public ThreadConnected(BluetoothSocket socket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() { // Приём данных
            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // "\r\n\" // определяем конец строки
                    int cmdOfLineIndex = sb.indexOf(SendStr);

                    if (endOfLineIndex > -1) {
                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());

                        runOnUiThread(new Runnable() { // Вывод данных
                            @Override
                            public void run() {
                                String[] words = sbprint.split(" ");

                                if (sbprint.indexOf("R1")>-1) {
                                    SendStr = "R2";
                                    B5.setText(words[1]);
                                    B6.setText(words[2]);
                                    B7.setText(words[3]);
                                    B8.setText(words[4]);
                                    I.setText(words[5]);
                                    Ah.setText(words[6]);
                                }
                                if(sbprint.indexOf("R2")>-1){
                                    SendStr = "R1";
                                    B1.setText(words[1]);
                                    B2.setText(words[2]);
                                    B3.setText(words[3]);
                                    B4.setText(words[4]);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
/////////////////// Нажатие кнопок /////////////////////
/////////////////////////D10////////////////////////////

}

