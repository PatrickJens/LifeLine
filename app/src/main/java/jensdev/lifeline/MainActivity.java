package jensdev.lifeline;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//Java starts by defining a class. MainActivity extends/inherits from AppCompatActivity
public class MainActivity extends AppCompatActivity {

    // Creating and turning on  BluetoothAdapter object
    private BluetoothAdapter mBluetoothAdapter;

    // Make the user press back twice to exit the app
    private static boolean userPressedBackAgain = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkBluetoothSupport();
        askBluetoothPermission();
        askCoarsePermission();
        initializeBluetoothAdapter();
        initializeButton();
        super.onResume();
    }
    /*
    @Override
    protected void onResume() {
        checkBluetoothSupport(); //Check if BLE supported //Does not slow app
        askBluetoothPermission(); //Tell user to turn on BLE if BLE off //Makes extremely slow
        askCoarsePermission(); //Makes extremely slow
        initializeBluetoothAdapter();
        initializeButton();
        //Close "interrupt" //See Activity Lifecycle
        //super.method() is a method from the super class
        super.onResume();
    }*/
    //After Activity creation: check if BLE supported, ask for permissions, and init BLE and button
    //This is an init() function
    //Function calls in OnResume swapped. See ttu example if it breaks



    /* FUNCTIONS */
    //If BLE not supported display message
    private void checkBluetoothSupport()
    {
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
        }
    }
    //Ensures Bluetooth is available on the device and it is enabled. If not, displays a dialog requesting user permission to enable BLE
    private void askBluetoothPermission(){
        if( mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled() ){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //REQUEST CODE is a constant passed to startActivityForResult(..)
            //Is a locally defined integer (must be >0) that the system passes back to you in your onActivityResult  implementation as the requestCode parameter
            int REQUEST_CODE_BLUETOOTH_PERMISSION = 2 ;
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_PERMISSION);
        }
    }

    //Need coarse location for NETWORK_PROVIDER
    private void askCoarsePermission()
    {
        if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            int REQUEST_CODE_COARSE_PERMISSION = 1;
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_COARSE_PERMISSION);
        }
    }
    //Initializes BLE adapter
    private void initializeBluetoothAdapter ()
    {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }
    // Initializing button push method
    private void initializeButton()
    {

        // Initialize ScanButton functionality
        Button scanButton = findViewById(R.id.startScanButton);
        Button scanButton2 = findViewById(R.id.startTrainButton);

        scanButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                openScanBle();
            }
            private void openScanBle(){
                Intent intent;
                intent = new Intent(getApplicationContext(), Scan.class);
                startActivity(intent);
            }
        });

        scanButton2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                openScanBle();
            }
            private void openScanBle(){
                Intent intent;
                intent = new Intent(getApplicationContext(), Scan2.class);
                startActivity(intent);
            }
        });
    }
}
