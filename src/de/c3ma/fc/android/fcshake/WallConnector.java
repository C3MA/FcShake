package de.c3ma.fc.android.fcshake;

import java.io.IOException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import de.c3ma.fullcircle.RawClient;
import de.c3ma.proto.fctypes.Frame;
import de.c3ma.proto.fctypes.FullcircleSerialize;
import de.c3ma.proto.fctypes.InfoAnswer;
import de.c3ma.proto.fctypes.Meta;
import de.c3ma.proto.fctypes.Pixel;
import de.c3ma.proto.fctypes.Start;
import de.c3ma.proto.fctypes.Timeout;

public class WallConnector extends Activity {

    private RawClient wall = null;
    private boolean mSendFrames;
    private int counter = 0;
    private int mWidth;
    private int mHeight;

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
            Frame f = new Frame();
            f.add(new Pixel(0, 0, 255, counter++, 0));
            counter = counter % mWidth;
            wall.sendFrame(f);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wall_connector);
        
        final Button connect =  (Button) findViewById(R.id.btnConnect);
        connect.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                /* Generate a new connection to the Wall */
                if (wall == null)
                {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String domain = settings.getString("wallip", null);
                    try {
                        wall = new RawClient(domain);
                        Toast.makeText(getApplicationContext(),
                                R.string.connecting,
                                Toast.LENGTH_LONG).show();

                        wall.requestInformation();
                        while(wall != null && !mSendFrames)
                        {
                            Thread.sleep(10);
                            handleNetwork();

                        }
                        
                        connect.setText(R.string.disconnect);
                        
                    } catch (UnknownHostException e) {
                        System.err.println(e.getMessage());
                        Toast.makeText(getApplicationContext(),
                                e.getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                        wall = null;
                        connect.setText(R.string.connect);
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        Toast.makeText(getApplicationContext(),
                                e.getLocalizedMessage(),
                                Toast.LENGTH_SHORT).show();
                        wall = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                R.string.stopped,
                                Toast.LENGTH_SHORT).show();
                        wall.close();
                        wall = null;
                        connect.setText(R.string.connect);
                    }
                }
            }
        });
    }

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
