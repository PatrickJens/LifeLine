package jensdev.lifeline;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.hardware.Sensor;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import jensdev.lifeline.R;

public class sensorOutput2 extends AppCompatActivity
{
    /* Acceleration Service */
    private static final UUID MOVE_SERVICE = UUID.fromString("f000aa80-0451-4000-b000-000000000000");
    private final UUID MOVE_DATA = UUID.fromString("f000aa81-0451-4000-b000-000000000000");
    private static final UUID MOVE_CONFIG = UUID.fromString("f000aa82-0451-4000-b000-000000000000");
    private static final UUID SENSHUB_MOVE_PERI_CHAR_UUID = UUID.fromString("f000aa83-0451-4000-b000-000000000000");

    /* Client Configuration Descriptor */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //"Global" class variables
    private BluetoothAdapter mBluetoothAdapter ;
    private BluetoothGatt mBluetoothGatt ;

    //UI Elements
    private Button saveTrainingButton ;
    private EditText foldername_plaintext ;
    private TextView sensorReads;
    private String foldername;

    //Arrays for data storage
    private ArrayList<Short> list_ax = new ArrayList<>();
    private ArrayList<Short> list_ay = new ArrayList<>();
    private ArrayList<Short> list_az = new ArrayList<>();
    private ArrayList<Short> list_gx = new ArrayList<>();
    private ArrayList<Short> list_gy = new ArrayList<>();
    private ArrayList<Short> list_gz = new ArrayList<>();
    private int i = 0 ;
    private int numSamples = 0 ;





    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_output2);

        //Init UI elements
        foldername_plaintext = findViewById(R.id.foldername_plaintext);
        saveTrainingButton = findViewById(R.id.save_train_button);
        sensorReads = findViewById(R.id.sensor_reads);

        //Get BLE device
        BluetoothDevice device = Objects.requireNonNull(getIntent().getExtras()).getParcelable("Bluetooth_Device");

        //Get Android BLE service manager
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        assert manager != null;
        mBluetoothAdapter = manager.getAdapter();

        //make GATT connection
        if (mBluetoothGatt == null)
        {
            //make Gatt connection
            mBluetoothGatt = device.connectGatt(sensorOutput2.this, false, mGattCallback);
            Toast.makeText(sensorOutput2.this, "GATT Connection Established", Toast.LENGTH_SHORT).show();
        }

        //Check that Android device storage is available
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state))
        {
            //Toast.makeText(this, "NO MEDIA MOUNTED", Toast.LENGTH_SHORT).show();
        }
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            //Toast.makeText(this, "MEDIA IS MOUNTED", Toast.LENGTH_SHORT).show();
        }

        mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);


        //Add button functionality. When you hit save, write the arrays to files
        saveTrainingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                //Print number of data points to screen
                sensorReads.setText(String.format("%d", list_ax.size()));

                //Get folder name from user and print where sensor reads output
                foldername = foldername_plaintext.getText().toString();
                //sensorReads.setText(foldername);

                //Disconnect
                if (mBluetoothGatt != null)
                {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }

                //Find folder or create new folder
                File myfolder = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), foldername);
                if(!myfolder.exists())
                {
                    myfolder.mkdirs();
                }

                String delimiter = "\n";
                //Create new fileoutputstream with append mode on
                FileOutputStream fos = null;
                //Create text files
                final File axfile = new File(myfolder, "ax.txt");
                final File ayfile = new File(myfolder, "ay.txt");
                final File azfile = new File(myfolder, "az.txt");
                final File gxfile = new File(myfolder, "gx.txt");
                final File gyfile = new File(myfolder, "gy.txt");
                final File gzfile = new File(myfolder, "gz.txt");

                //write data to files
                try
                {
                    fos = new FileOutputStream(axfile, true);
                    for(i = 0 ; i < list_ax.size() ; i ++ )
                    {
                        String number = list_ax.get(i)+"";
                        fos.write(number.getBytes());
                        fos.write(delimiter.getBytes());
                    }
                    fos.flush();
                    fos.close();

                    fos = new FileOutputStream(ayfile, true);
                    for(i = 0 ; i < list_ay.size() ; i ++ )
                    {
                        String number = list_ay.get(i)+"";
                        fos.write(number.getBytes());
                        fos.write(delimiter.getBytes());
                    }
                    fos.flush();
                    fos.close();

                    fos = new FileOutputStream(azfile, true);
                    for(i = 0 ; i < list_az.size() ; i ++ )
                    {
                        String number = list_az.get(i)+"";
                        fos.write(number.getBytes());
                        fos.write(delimiter.getBytes());
                    }
                    fos.flush();
                    fos.close();

                    fos = new FileOutputStream(gxfile, true);
                    for(i = 0 ; i < list_gx.size() ; i ++ )
                    {
                        String number = list_gx.get(i)+"";
                        fos.write(number.getBytes());
                        fos.write(delimiter.getBytes());
                    }
                    fos.flush();
                    fos.close();
                    fos = new FileOutputStream(gyfile, true);

                    for(i = 0 ; i < list_gy.size() ; i ++ )
                    {
                        String number = list_gy.get(i)+"";
                        fos.write(number.getBytes());
                        fos.write(delimiter.getBytes());
                    }
                    fos.flush();
                    fos.close();

                    fos = new FileOutputStream(gzfile, true);
                    for(i = 0 ; i < list_gz.size() ; i ++ )
                    {
                        String number = list_gz.get(i)+"";
                        fos.write(number.getBytes());
                        fos.write(delimiter.getBytes());
                    }
                    fos.flush();
                    fos.close();


                }
                catch (FileNotFoundException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

        });

    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        finish();
        Toast.makeText(getApplicationContext(),"Disconnected from the device.",Toast.LENGTH_SHORT).show();
    }



    /*Start GattCallback */
    /*BluetoothGattCallback contains state machine performing sensor reads */
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /* State Machine */
        private int mState = 0;
        private void reset() { mState = 0; }
        private void advance() { mState++; }

        // If connection state changes, it reports to the terminal and meets the if statement
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED)
            {
                gatt.discoverServices();
            }
            else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                //If we disconnect, we clear user interface
                //mHandler.sendEmptyMessage(MSG_CLEAR);
            }
            else if (status != BluetoothGatt.GATT_SUCCESS)
            {
                gatt.disconnect();
                finish();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            //mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors..."));
            reset();
            enableNextSensor(gatt);
        }

        private void enableNextSensor(BluetoothGatt gatt)
        {
            BluetoothGattCharacteristic characteristic;
            switch (mState)
            {

                case 0:
                    characteristic = gatt.getService(MOVE_SERVICE).getCharacteristic(MOVE_CONFIG);
                    characteristic.setValue(new byte[] {0x01}); //set byte to 1 to enable a sensor
                    break;
                default:
                    //mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }
            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            readNextSensor(gatt);
        }

        private void readNextSensor(BluetoothGatt gatt)
        {
            BluetoothGattCharacteristic characteristic;
            switch (mState)
            {
                case 0:
                    characteristic = gatt.getService(MOVE_SERVICE).getCharacteristic(MOVE_CONFIG);
                    break;
                default:
                    //mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }
            gatt.readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            if(MOVE_DATA.equals(characteristic.getUuid()))
            {
                //mHandler.sendMessage(Message.obtain(null, MSG_MOVE, characteristic));
            }
            setNotifyNextSensor(gatt);
        }

        private void setNotifyNextSensor(BluetoothGatt gatt)
        {
            BluetoothGattCharacteristic characteristic;
            switch (mState)
            {
                case 0:
                    characteristic = gatt.getService(MOVE_SERVICE).getCharacteristic(MOVE_DATA);
                    break;
                default:
                    //mHandler.sendEmptyMessage(MSG_DISMISS);
                    return;
            }
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            advance();
            enableNextSensor(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            processAcceleration(characteristic);
        }
    };
    /* End GattCallback */


    void processAcceleration(BluetoothGattCharacteristic characteristic)
    {
        //increment samples counter
        numSamples ++ ;
        //Store characteristic in byte array
        byte[] data = characteristic.getValue();

        //Extract 16 bit (2 byte) data from characteristic and put into array
        byte[] accel_x_bytes = Arrays.copyOfRange(data, 0, 2);
        byte[] accel_y_bytes = Arrays.copyOfRange(data, 2, 4);
        byte[] accel_z_bytes = Arrays.copyOfRange(data, 4, 6);
        byte[] gyro_x_bytes = Arrays.copyOfRange(data, 6, 8);
        byte[] gyro_y_bytes = Arrays.copyOfRange(data, 8, 10);
        byte[] gyro_z_bytes = Arrays.copyOfRange(data, 10, 12);

        //Declare a 9 element short array (2 bytes per sensor data). 6 for ax,ay,az,gx,gy,gz, and 3 unused magnetic data points
        final short[] movement_data = new short[9];

        //Feed the bytes least significant bit first (little endian). NOTICE THE GYRO AND ACCEL INDICES
        movement_data[0] = ByteBuffer.wrap(gyro_x_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[1] = ByteBuffer.wrap(gyro_y_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[2] = ByteBuffer.wrap(gyro_z_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[3] = ByteBuffer.wrap(accel_x_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[4] = ByteBuffer.wrap(accel_y_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[5] = ByteBuffer.wrap(accel_z_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();

        short gyro_x = movement_data[0];
        short gyro_y = movement_data[1];
        short gyro_z = movement_data[2];
        short accel_x = movement_data[3];
        short accel_y = movement_data[4];
        short accel_z = movement_data[5];

        //add data points to their respective arrays
        list_ax.add(accel_x);
        list_ay.add(accel_y);
        list_az.add(accel_z);
        list_gx.add(gyro_x);
        list_gy.add(gyro_y);
        list_gz.add(gyro_z);

        //print list size to screen
        sensorReads.setText(String.format("%d",list_ax.size()));
    }
}