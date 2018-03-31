package com.dgsw.iot.home;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int REQUEST_BLUETOOTH_ENABLE = 100;

    private TextView mConnectionStatus;
    private EditText mInputEditText;

    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";

    String[] str;

    LinearLayout tv;
    LinearLayout gas;
    LinearLayout air;
    LinearLayout fan;
    LinearLayout li1;
    LinearLayout li2;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendButton = (Button)findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                String sendMessage = mInputEditText.getText().toString();
                if ( sendMessage.length() > 0 ) {
                    sendMessage(sendMessage);
                }
            }
        });

        tv =(LinearLayout)findViewById(R.id.tv);
        gas =(LinearLayout)findViewById(R.id.gas);
        air =(LinearLayout)findViewById(R.id.air);
        fan =(LinearLayout)findViewById(R.id.fan);
        li1 =(LinearLayout)findViewById(R.id.li1);
        li2 =(LinearLayout)findViewById(R.id.li2);

        tv.setOnClickListener(this);
        gas.setOnClickListener(this);
        air.setOnClickListener(this);
        fan.setOnClickListener(this);
        li1.setOnClickListener(this);
        li2.setOnClickListener(this);

        tv.setEnabled(false);
        gas.setEnabled(false);
        air.setEnabled(false);
        fan.setEnabled(false);
        li1.setEnabled(false);
        li2.setEnabled(false);

        mConnectionStatus = (TextView)findViewById(R.id.connection_status_textview);
        mInputEditText = (EditText)findViewById(R.id.input_string_edittext);
        ListView mMessageListview = (ListView) findViewById(R.id.message_listview);

        mConversationArrayAdapter = new ArrayAdapter<>( this,
                android.R.layout.simple_list_item_1 );
        mMessageListview.setAdapter(mConversationArrayAdapter);


        Log.d( TAG, "Initalizing Bluetooth adapter...");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("블루투스가 장착되어있지 않습니다.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
        else {
            Log.d(TAG, "Initialisation successful.");

            showPairedDevicesListDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if ( mConnectedTask != null ) {

            mConnectedTask.cancel(true);
        }
    }

    @Override
    public void onClick(View view) {
        tv.setEnabled(false);
        gas.setEnabled(false);
        air.setEnabled(false);
        fan.setEnabled(false);
        li1.setEnabled(false);
        li2.setEnabled(false);
        switch(view.getId()){
            case R.id.tv:
                if(((TextView)findViewById(R.id.tv_stat)).getText().toString().equals("TV 켜기"))
                    sendMessage("TVON");
                else
                    sendMessage("TVOFF");
                break;
            case R.id.gas:
                if(((TextView)findViewById(R.id.gas_stat)).getText().toString().equals("가스 열기"))
                    sendMessage("GASON");
                else
                    sendMessage("GASOFF");
                break;
            case R.id.air:
                if(((TextView)findViewById(R.id.air_stat)).getText().toString().equals("에어컨 켜기"))
                    sendMessage("CONDON");
                else
                    sendMessage("CONDOFF");
                break;
            case R.id.fan:
                if(((TextView)findViewById(R.id.fan_stat)).getText().toString().equals("선풍기 켜기"))
                    sendMessage("FANON");
                else
                    sendMessage("FANOFF");
                break;
            case R.id.li1:
                if(((TextView)findViewById(R.id.li1_stat)).getText().toString().equals("전등 켜기"))
                    sendMessage("LI1ON");
                else
                    sendMessage("LI1OFF");
                break;
            case R.id.li2:
                if(((TextView)findViewById(R.id.li2_stat)).getText().toString().equals("스탠드 켜기"))
                    sendMessage("LI2ON");
                else
                    sendMessage("LI2OFF");
                break;
        }
    }

    //runs while listening for incoming connections.
    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            } catch (IOException e) {
                Log.e( TAG, "socket create failed " + e.getMessage());
            }

            mConnectionStatus.setText("연결중");
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            mBluetoothAdapter.cancelDiscovery();

            // 소켓생성
            try {
                // 성공 / 실패 반환
                mBluetoothSocket.connect();
            } catch (IOException e) {
                // 소켓 닫기
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "소켓 닫기 실패", e2);
                }

                return false;
            }

            return true;
        }


        @Override
        protected void onPostExecute(Boolean isSucess) {

            if ( isSucess ) {
                connected(mBluetoothSocket);
            }
            else{

                isConnectionError = true;
                Log.d( TAG,  "Unable to connect device");
                showErrorDialog("연결이 불가능합니다");
            }
        }
    }


    public void connected( BluetoothSocket socket ) {
        mConnectedTask = new ConnectedTask(socket);
        mConnectedTask.execute();
    }


    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket){

            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e );
            }

            Log.d( TAG, "connected to "+mConnectedDeviceName);
            mConnectionStatus.setText(mConnectedDeviceName+"과 연결되었습니다.");
            tv.setEnabled(true);
            gas.setEnabled(true);
            air.setEnabled(true);
            fan.setEnabled(true);
            li1.setEnabled(true);
            li2.setEnabled(true);
        }



        @Override
        protected Boolean doInBackground(Void... params) {

            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;

            // Keep listening to the InputStream while connected
            while (true) {

                if ( isCancelled() ) return false;

                try {

                    int bytesAvailable = mInputStream.available();

                    if(bytesAvailable > 0) {

                        byte[] packetBytes = new byte[bytesAvailable];
                        // Read from the InputStream
                        mInputStream.read(packetBytes);

                        for(int i=0;i<bytesAvailable;i++) {

                            byte b = packetBytes[i];
                            if(b == '\n')
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                publishProgress(recvMessage);
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {

                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }

        }

        @Override
        protected void onProgressUpdate(String... recvMessage) {
            //mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMessage[0], 0);
            //Toast.makeText(getApplicationContext(), recvMessage[0], Toast.LENGTH_SHORT).show();
            str = recvMessage[0].split("&");
            if(Integer.parseInt(str[0].replaceAll("\\s+", "")) == 0) {
                ((TextView) findViewById(R.id.tv_stat)).setText("TV 켜기");
            }

            else if(Integer.parseInt(str[0].replaceAll("\\s+", "")) == 1) {
                ((TextView) findViewById(R.id.tv_stat)).setText("TV 끄기");
            }

            if(Integer.parseInt(str[1].replaceAll("\\s+", "")) == 0) {
                ((TextView) findViewById(R.id.gas_stat)).setText("가스 열기");
            }
            else if(Integer.parseInt(str[1].replaceAll("\\s+", "")) == 1) {
                    ((TextView) findViewById(R.id.gas_stat)).setText("가스 닫기");
                }
            if(Integer.parseInt(str[2].replaceAll("\\s+", "")) == 0) {
                ((TextView) findViewById(R.id.air_stat)).setText("에어컨 켜기");
            }
            else if(Integer.parseInt(str[2].replaceAll("\\s+", "")) == 1) {
                    ((TextView) findViewById(R.id.air_stat)).setText("에어컨 끄기");
                }
            if(Integer.parseInt(str[3].replaceAll("\\s+", "")) == 0) {
                ((TextView) findViewById(R.id.fan_stat)).setText("선풍기 켜기");
            }
            else if(Integer.parseInt(str[3].replaceAll("\\s+", "")) == 1) {
                    ((TextView) findViewById(R.id.fan_stat)).setText("선풍기 끄기");
                }
            if(Integer.parseInt(str[4].replaceAll("\\s+", "")) == 0) {
                ((TextView) findViewById(R.id.li1_stat)).setText("전등 켜기");
            }
            else if(Integer.parseInt(str[4].replaceAll("\\s+", "")) == 1) {
                    ((TextView) findViewById(R.id.li1_stat)).setText("전등 끄기");
                }
            if(Integer.parseInt(str[5].replaceAll("\\s+", "")) == 0) {
                ((TextView) findViewById(R.id.li2_stat)).setText("스탠드 켜기");
            }
            else if(Integer.parseInt(str[5].replaceAll("\\s+", "")) == 1) {
                ((TextView) findViewById(R.id.li2_stat)).setText("스탠드 끄기");
            }

                    tv.setEnabled(true);
                    gas.setEnabled(true);
                    air.setEnabled(true);
                    fan.setEnabled(true);
                    li1.setEnabled(true);
                    li2.setEnabled(true);
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);

            if ( !isSucess ) {


                closeSocket();
                Log.d(TAG, "Device connection was lost");
                isConnectionError = true;
                showErrorDialog("연결이 끊김");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);

            closeSocket();
        }

        void closeSocket(){

            try {

                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");

            } catch (IOException e2) {

                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }

        void write(String msg){

            msg += "\n";

            try {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during send", e );
                recreate();
            }

            mInputEditText.setText(" ");
        }
    }


    public void showPairedDevicesListDialog()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if ( pairedDevices.length == 0 ){
            showQuitDialog( "아무런 디바이스와 페어링되지않음.\n"
                    +"하나이상의 디바이스와 페어링 해야함");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++) {
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("기기를 선택해주세요.");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                // Attempt to connect to the device
                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }



    public void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("종료");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("확인",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ( isConnectionError  ) {
                    isConnectionError = false;
                    finish();
                }
            }
        });
        if(!MainActivity.this.isFinishing()) {
            builder.create().show();
        }
    }


    public void showQuitDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("종료");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("확인",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    void sendMessage(String msg){

        if ( mConnectedTask != null ) {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
            mConversationArrayAdapter.insert("Me:  " + msg, 0);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if (resultCode == RESULT_OK){
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if(resultCode == RESULT_CANCELED){
                showQuitDialog( "블루투스를 활성화해야합니다");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reconnect) {
            recreate();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}