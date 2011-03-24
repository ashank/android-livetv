package nl.oxanvanleeuwen.android.livetv.activities;

import java.util.ArrayList;
import nl.oxanvanleeuwen.android.livetv.R;
import nl.oxanvanleeuwen.android.livetv.service.Channel;
import nl.oxanvanleeuwen.android.livetv.service.MediaStreamService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
        ListView lv = (ListView)findViewById(R.id.channellist);
        
        // load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String baseurl = preferences.getString("address", "");
        if(baseurl.equals("") || preferences.getString("username", "").equals("") || preferences.getString("password", "").equals("")) {
        	Intent intent = new Intent(this, Preferences.class);
        	startActivity(intent);
        }
        
        // set up service
        Log.d(TAG, "Using " + baseurl + " as base url for the webservice");
        if(baseurl.substring(baseurl.length() - 1) != "/")
        	baseurl += "/";
        service = new MediaStreamService(baseurl + "MediaStream.svc");
        
        // show all channels
        try {
            Log.v(TAG, "Started loading channels");
            ArrayList<String> channels = new ArrayList<String>();
			for(Channel channel : service.getChannels()) {
				channels.add(channel.name);
			}
			String[] strlist = new String[]{};
			ArrayAdapter<String> items = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, channels.toArray(strlist));
			lv.setAdapter(items);
			Log.v(TAG, "Loaded channels");
		} catch (Exception e) {
			Log.e(TAG, "Failed to show channel list", e);
			Toast.makeText(ChannelList.this, getString(R.string.channelfailed), Toast.LENGTH_SHORT);
		}
		
		// register callback when clicked on channel
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Channel channel = service.getChannelsCached().get(position);
				try {
					String url = service.getTvStreamUrl(service.getTranscoder(), channel, preferences.getString("username", ""), preferences.getString("password", ""));
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
					Toast.makeText(ChannelList.this, getString(R.string.streamfailed), Toast.LENGTH_SHORT);
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
    	}
    	
    	return true;
    }
}