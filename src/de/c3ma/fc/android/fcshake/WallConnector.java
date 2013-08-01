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
import de.c3ma.fullcircle.RawClient;

public class WallConnector extends Activity {

    private RawClient wall = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wall_connector);
        
        Button connect =  (Button) findViewById(R.id.btnConnect);
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
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        wall = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                        wall = null;
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
