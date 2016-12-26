package com.tosatto.r2pi0_test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter blueAdapter = null;
    private BluetoothDevice blueDev = null;
    private BluetoothSocket sock = null;
    private PrintWriter blueOut = null;

    private final String BLUE_DEV_NAME = "rpi2";

    private void blueSetup ()
    {
        blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!blueAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 100);
        }

        Set<BluetoothDevice> pairedDevices = blueAdapter.getBondedDevices();

        StringBuilder sb = new StringBuilder();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(BLUE_DEV_NAME))
                {
                    blueDev = device;
                }
            }
        }

        if (blueDev != null)
        {
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                sock = blueDev.createInsecureRfcommSocketToServiceRecord( UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                sock.connect();

                blueOut = new PrintWriter (sock.getOutputStream());

            } catch (IOException e) {
                Log.e("AAA", "Socket's create() method failed", e);


                try {
                    Method createMethod = blueDev.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
                    sock = (BluetoothSocket)createMethod.invoke(blueDev, 1);

                    sock.connect();

                    blueOut = new PrintWriter (sock.getOutputStream());

                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                } catch (InvocationTargetException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JoystickView joy = (JoystickView)findViewById(R.id.joy);

        final TextView tv = (TextView)findViewById(R.id.tv);

        joy.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                tv.setText(Integer.toString(angle) + "° " + Integer.toString(power));

                move(power, angle);
                rotate(angle, power);
            }
        }, 100);

        blueSetup();

        Set<BluetoothDevice> pairedDevices = blueAdapter.getBondedDevices();

        StringBuilder sb = new StringBuilder();



        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {

                sb.append(device.getName());
                sb.append(";");
            }
        }

        tv.setText(sb.toString());
    }

    private void rotate (int degree, int power)
    {
        int rot = 0;


        if (degree > 180)
            degree = 360-degree;


        rot = ((90 - degree)*power)/90;


        if (blueOut != null) {
            blueOut.write("r " + Integer.toString(rot) + ";");
            blueOut.flush();
        }
    }

    private void move (int power, int degree)
    {
        if (blueOut != null) {

            double rad = 57.2957795;

            power = (int)(Math.sin(degree/rad)*power);

            blueOut.write("s " + Integer.toString(power) + ";");
            blueOut.flush();
        }
    }
}
