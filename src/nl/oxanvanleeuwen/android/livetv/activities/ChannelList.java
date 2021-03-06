package nl.oxanvanleeuwen.android.livetv.activities;

import java.util.ArrayList;
import nl.oxanvanleeuwen.android.livetv.R;
import nl.oxanvanleeuwen.android.livetv.service.Channel;
import nl.oxanvanleeuwen.android.livetv.service.MediaStreamService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ChannelList extends Activity {
	private static final String TAG = "ChannelListActivity";
	
	private MediaStreamService service;
	private SharedPreferences preferences;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// init
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channellist);
        setTitle(getResources().getText(R.string.availablechannellist));
        
        // load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// start preferences
        if(!hasValidPreferences()) {
        	Log.v(TAG, "Launching preferences");
        	Intent intent = new Intent(this, Preferences.class);
        	startActivity(intent);
        	return;
        }
        
        Log.v(TAG, "Loading channel list");
        ListView lv = (ListView)findViewById(R.id.channellist);
        
        // set up service and load channel list
    	String baseurl = preferences.getString("address", "");
        Log.d(TAG, "Using " + baseurl + " as base url for the webservice");
        if(baseurl.substring(baseurl.length() - 1) != "/")
        	baseurl += "/";
        service = new MediaStreamService(baseurl + "MediaStream.svc");
        new LoadChannelListTask().execute(this);
		
		// register callback when clicked on channel
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Channel channel = service.getChannelsCached().get(position);
				try {
					String url = service.getTvStreamUrl(service.getTranscoderById(Integer.parseInt(preferences.getString("transcoder", "1"))), channel, 
							preferences.getString("username", ""), preferences.getString("password", ""));
					Log.d(TAG, "Play URL: " + url);
					Intent intent = new Intent();
					
					if(Build.VERSION.SDK_INT >= 9) {
						// android 2.3+, native player
						intent.setClass(ChannelList.this, ViewStream.class);
						intent.putExtra("url", url);
					} else {
						// use external player 
						intent.setDataAndType(Uri.parse(url), "video/M2TS");
					}
					
					startActivity(intent);
				} catch (Exception e) {
					Log.e(TAG, "Failed to start stream", e);
					Toast.makeText(ChannelList.this, getString(R.string.streamfailed), Toast.LENGTH_SHORT).show();
				}
			}
		});
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.mainmenu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {    	
    	switch (item.getItemId()) {
    		case R.id.menu_preferences:
    			Log.v(TAG, "Starting preferences");
    			Intent intent = new Intent(this, Preferences.class);
    			startActivity(intent);
    			break;
    		case R.id.menu_exit:
    			this.finish();
    			break;
    	}
    	
    	return true;
    }
    
    private boolean hasValidPreferences() {
    	return
    		!preferences.getString("address", "").equals("") &&
    		!preferences.getString("username", "").equals("") &&
    		!preferences.getString("password", "").equals("") &&
    		!preferences.getString("transcoder", "").equals("") &&
    		!preferences.getString("transcoder", "").equals("Invalid");
    }
    
    private class LoadChannelListTask extends AsyncTask<Context, Void, Boolean> {
    	private ArrayAdapter<String> items; 
    	
    	protected Boolean doInBackground(Context... contexts) {
            // show all channels
            try {
                Log.v(TAG, "Started loading channels");
                ArrayList<String> channels = new ArrayList<String>();
    			for(Channel channel : service.getChannels()) {
    				channels.add(channel.name);
    			}
    			String[] strlist = new String[]{};
    			items = new ArrayAdapter<String>(contexts[0], android.R.layout.simple_list_item_1, channels.toArray(strlist));
    			Log.v(TAG, "Loaded channels");
    			return true;
    		} catch (Exception e) {
    			Log.e(TAG, "Failed to show channel list", e);
    			return false;
    		}
    	}
    	
    	protected void onPostExecute(Boolean result) {
    		if(result) {
    			((ListView)findViewById(R.id.channellist)).setAdapter(items);
    		} else {
    			Toast.makeText(ChannelList.this, getString(R.string.channelfailed), Toast.LENGTH_LONG).show();
    		}
    	}
    }
}