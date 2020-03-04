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

public class SensorOutput extends AppCompatActivity
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
    private TextView pvar_x_textview ;
    private TextView pvar_y_textview ;
    private TextView pvar_z_textview ;
    private TextView samplecountTextView ;
    private TextView gVar_x_textview, gVar_y_textview, gVar_z_textview;

    //Arrays for data storage
    private ArrayList<Double> list_ax = new ArrayList<>();
    private ArrayList<Short> list_ay = new ArrayList<>();
    private ArrayList<Double> list_az = new ArrayList<>();
    private ArrayList<Double> list_gx = new ArrayList<>();
    private ArrayList<Double> list_gy = new ArrayList<>();
    private ArrayList<Double> list_gz = new ArrayList<>();
    private int points[] = new int[2];
    private int i = 0 ;


    private int dataCount = 0;
    private int samplePerCalculation = 35;

    //For Accel XYZ
    private double xTotal = 0;
    private double yTotal = 0;
    private double zTotal = 0;
    private double xTotalSqrt, yTotalSqrt, zTotalSqrt;
    private double xSqrtSum = 0, ySqrtSum = 0, zSqrtSum = 0;
    private double xB, yB, zB;
    private double xSS, ySS, zSS;
    private double xVar, yVar, zVar;


    //For Gyro XYZ
    private double gxTotal = 0, gyTotal = 0, gzTotal = 0;
    private double gxTotalSqrt, gyTotalSqrt, gzTotalSqrt;
    private double gxSqrtSum = 0, gySqrtSum = 0, gzSqrtSum = 0;
    private double gxB, gyB, gzB;
    private double gxSS, gySS, gzSS;
    private double gxVar, gyVar, gzVar;

    private int N = 0 ;
    private int period = 0 ;
    private double frequency = 0.0 ;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_output);

        //Init UI elements
        pvar_x_textview = findViewById(R.id.pVarX);
        pvar_y_textview = findViewById(R.id.pVarY);
        pvar_z_textview = findViewById(R.id.pVarZ);
        samplecountTextView = findViewById(R.id.samplecount);

        gVar_x_textview = findViewById(R.id.x);
        gVar_y_textview = findViewById(R.id.y);
        gVar_z_textview = findViewById(R.id.z);



        points[0] = 0;
        points[1] = 0;

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
            mBluetoothGatt = device.connectGatt(SensorOutput.this, false, mGattCallback);
            Toast.makeText(SensorOutput.this, "GATT Connection Established", Toast.LENGTH_SHORT).show();
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



    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (mBluetoothGatt != null)
        {
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


    //Alexia: Every time a data point is read, it calls this function
    void processAcceleration(BluetoothGattCharacteristic characteristic) {
//        samplecount.setText(String.format("%f",12.1));
//        pvar_x_textview.setText(Double.toString(frequency));
        // samplecountTextView.setText(Double.toString(frequency));
        samplecountTextView.setText(String.format("%3.2f",frequency));
        //pvar_x_textview.setText(Double.toString(frequency));



        //Log.i("TRU","truck");
        int i;

        N ++ ;

        byte[] data = characteristic.getValue();

        byte[] accel_x_bytes = Arrays.copyOfRange(data, 0, 2);
        byte[] accel_y_bytes = Arrays.copyOfRange(data, 2, 4);
        byte[] accel_z_bytes = Arrays.copyOfRange(data, 4, 6);
        byte[] gyro_x_bytes = Arrays.copyOfRange(data, 6, 8);
        byte[] gyro_y_bytes = Arrays.copyOfRange(data, 8, 10);
        byte[] gyro_z_bytes = Arrays.copyOfRange(data, 10, 12);
        final short[] movement_data = new short[9];

        //float f1 = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        movement_data[0] = ByteBuffer.wrap(gyro_x_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[1] = ByteBuffer.wrap(gyro_y_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[2] = ByteBuffer.wrap(gyro_z_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[3] = ByteBuffer.wrap(accel_x_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[4] = ByteBuffer.wrap(accel_y_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        movement_data[5] = ByteBuffer.wrap(accel_z_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();

        short gyro_x = movement_data[0];
        //write to text file
        short gyro_y = movement_data[1];
        short gyro_z = movement_data[2];
        short accel_x = movement_data[3];
        short accel_y = movement_data[4];
        short accel_z = movement_data[5];


        //Testing
        //pvar_x_textview.setText(String.format("%d", accel_x));

        if (accel_y > 10000 || accel_y < -8500)
        {
            //fill array with 70 data points (0...69)
            if (list_ay.size() <= 70) {
                list_ay.add(accel_y);

            } else {
                for (i = 0; i < 70; i++) {
                    //samplecount.setText(String.format("%d",frequency));
                    if (i == 69) {
                        list_ay.set(69, accel_y);
                    } else {
                        list_ay.set(i, list_ay.get(i + 1));
//                        samplecount.setText(String.format("%f", frequency));

                    }////

                    //gVar_x_textview.setText(String.format("%d", list_ay.get(0)));

                    //gVar_y_textview.setText(String.format("%d", list_ay.get(35)));

                    //gVar_z_textview.setText(String.format("%d", list_ay.get(68)));



                    //Log.i("XXXX", "" +list_ay.get(68) + "    N = "   + N + "    " + list_ay.get(69) );
                }
                if( (list_ay.get(68) < 0) )
                {
                    if (list_ay.get(69) > 0){
                        if(points[0] == 0 )
                        {
                            points[0] = N ;
                            Log.i("XXXX", "69p =  " + list_ay.get(69) + "   N111 = " + N);
                        }
                        else {
                            points[1] = N ;
                            Log.i("XXXX", "69p =  " + list_ay.get(69) + "   N2 = " + N);
                            period = points[1] - points[0] ;
                            Log.i("XXXX", "period = " + period);
                            frequency = 35.0 / period ;
                            Log.i("XXXX", "Frequency = " + frequency);
                            points[0] = 0;

                            //points[1] = 0 ;
                        }
                    }

//                    if( (list_ay.get(68) < -10000)  )
//                        {
//                            gVar_x_textview.setText(String.format("%d", list_ay.get(68)));
//                            pvar_x_textview.setText(String.format("%d", N));
//                            Log.i("XXXX", "" +list_ay.get(68) + "    N = "   + N );
//                            if((list_ay.get(69) > 10000))
//                            {
//
//                                gVar_y_textview.setText(String.format("%d", list_ay.get(69)));
//
//                                pvar_y_textview.setText(String.format("%d", N));
//
//                                Log.i("XXXX", "" +list_ay.get(68) + "    N = "   + N + "    " + list_ay.get(69) );
//                            }
//
//                        }

                }
            }

        }


//        if( list_ay.size() == 70 )
//        {
//            if( (list_ay.get(68) < -10000)  )
//            {
//                Log.e("LL", "" + list_ay.get(68));
//
//                //gVar_x_textview.setText(String.format("%d", list_ay.get(68)));
//                if((list_ay.get(69) > 10000))
//                {
//                    //gVar_y_textview.setText(String.format("%d", list_ay.get(69)));
//                }
//
//            }
//        }


        if( N ==99999)
        {
            N = 0 ;
        }
    }
}