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
    private int counter = 0;
    private int mWidth;
    private int mHeight;

    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.

        final float alpha = 0.8f;

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        System.out.println(" X = " + linear_acceleration[0] 
                + " y = " + linear_acceleration[1] 
                + " z = " + linear_acceleration[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    private void handleNetwork() throws IOException {

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
            } else if (got instanceof Start) {
                System.out.println("We have a GOOO send some data!");
                mSendFrames = true;
            } else if (got instanceof Timeout) {
                System.out.println("Too slow, so we close the session");
                wall.close();
                wall = null;
            }
        }

        // send something... NOW
        if (mSendFrames) {

            final Frame f = new Frame();
            new RainbowEllipse((mWidth / 2) - 1, (mHeight / 2) - 1, (mWidth / 2) - 2, (mWidth / 2) - 2) {

                @Override
                protected void drawPixel(int x, int y, SimpleColor c) {
                    f.add(new Pixel(x, y, c));
                }
            }.drawEllipse(1);

            f.add(new Pixel(0, 0, 255, counter++, 0));
            counter = counter % mWidth;
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
                        while (wall != null && !mSendFrames) {
                            Thread.sleep(10);
                            handleNetwork();

                        }

                        connect.setText(R.string.disconnect);

                        // TODO move the following code into the handling of
                        // motion sensors
                        while (true) {
                            Thread.sleep(50);
                            handleNetwork();
                        }

                    } catch (UnknownHostException e) {
                        System.err.println(e.getMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        wall = null;
                        connect.setText(R.string.connect);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        wall = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), R.string.stopped, Toast.LENGTH_SHORT).show();
                        wall.close();
                        wall = null;
                        connect.setText(R.string.connect);
                    }
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
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
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
