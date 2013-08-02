package de.c3ma.fc.android.fcshake;

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import de.c3ma.animation.RainbowEllipse;
import de.c3ma.fullcircle.RawClient;
import de.c3ma.proto.fctypes.Frame;
import de.c3ma.proto.fctypes.FullcircleSerialize;
import de.c3ma.proto.fctypes.InfoAnswer;
import de.c3ma.proto.fctypes.Meta;
import de.c3ma.proto.fctypes.Pixel;
import de.c3ma.proto.fctypes.Start;
import de.c3ma.proto.fctypes.Timeout;
import de.c3ma.types.SimpleColor;

public class WallConnector extends Activity implements SensorEventListener {

    private RawClient wall = null;
    private boolean mSendFrames;
    private int mWidth;
    private int mHeight;

    private long lastUpdate;
    
    private int numberX;
    private int numberY;
    private int speed;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
        
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
//        Log.e("Sensor", "" + x + "\t" + y + "\t" +z);
        // X-axis
        if (Math.abs(x) > 5) {
            numberX ++;
            speed = (int) x;
        } else if (Math.abs(y) > 5) {
            numberY++;
            speed += (int) y;
        }
        
        long actualTime = System.currentTimeMillis();
        if (actualTime - lastUpdate < 50) {
            return;
        }
        lastUpdate = actualTime;
        try {
            handleNetwork();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            wall.close();
            wall = null;
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    private void handleNetwork() throws IOException {
        if (wall == null)
            return;
        FullcircleSerialize got = wall.readNetwork();
        if (got != null) {
            System.out.println(got);
            if (got instanceof InfoAnswer) {
                /*
                 * Extract the expected resolution and use these values for the
                 * request
                 */
                InfoAnswer ia = ((InfoAnswer) got);
                Meta meta = ia.getMeta();
                mWidth = ia.getWidth();
                mHeight = ia.getHeight();

                /*
                 * when we got the resolution of the map, in this example we now
                 * want to start to send something
                 */
                wall.requestStart("android", 1, meta);
                numberX = (mWidth / 2);
                numberY = (mHeight / 2);
            } else if (got instanceof Start) {
                System.out.println("We have a GOOO send some data!");
                mSendFrames = true;
            } else if (got instanceof Timeout) {
                System.out.println("Too slow, so we close the session");
                wall.close();
                wall = null;
            }
        }

        Log.d("Wall", "Width " + mWidth + ", height " + mHeight + "\t" + (speed / 10) + "\t" + numberX + "x" + numberY + "\tsendFrames=" + mSendFrames);
        
        // send something... NOW
        if (mSendFrames) {
            final Frame f = new Frame();
            
            
                new RainbowEllipse(numberX, numberY,(mWidth / 2) - 1, (mWidth / 2) - 1) {
                
                    @Override
                    protected void drawPixel(int x, int y, SimpleColor c) {
                        f.add(new Pixel(x, y, c));
                    }
                }.drawEllipse(Math.abs(speed / 10));
            wall.sendFrame(f);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wall_connector);

        final Button connect = (Button) findViewById(R.id.btnConnect);
        connect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                /* Generate a new connection to the Wall */
                if (wall == null) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String domain = settings.getString("wallip", null);
                    try {
                        wall = new RawClient(domain);
                        Toast.makeText(getApplicationContext(), R.string.connecting, Toast.LENGTH_LONG).show();

                        wall.requestInformation();


                        connect.setText(R.string.disconnect);

                    } catch (UnknownHostException e) {
                        System.err.println(e.getMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        wall = null;
                        connect.setText(R.string.connect);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        wall = null;
                    }
                } else {
                    wall.close();
                    wall = null;
                    connect.setText(R.string.connect);
                }
            }
        });

        /*
         * Rotation Sensor
         * 
         * Source: -
         * http://developer.android.com/guide/topics/sensors/sensors_motion.html
         * - com.example.android.apis.os.RotationVectorDemo.java
         */
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, 10000);
    }

    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.action_settings:
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.wall_connector, menu);
        return true;
    }

}
