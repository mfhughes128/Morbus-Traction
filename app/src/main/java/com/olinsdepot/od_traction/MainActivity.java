package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.PopupWindow;
import android.widget.Toast;
import android.support.v4.widget.DrawerLayout;

import com.olinsdepot.mbus_srvc.MbusService.*;
import com.olinsdepot.od_traction.LocoUnit;


/**
 * Main Activity Olins Depot Throttle Application
 * 
 * @author mhughes
 *
 */
public class MainActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks,
		NetFragment.OnServerChangeListener,
		RosterFragment.OnRosterChangeListener,
        ThrottleFragment.OnThrottleChangeListener {
	
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;
	
	//////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////
	private static final int tNum = 2; /* Number of throttles fixed at 2 for now */
	
	//////////////////////////////////////////////////////////////////////
	// Local variables
	/////////////////////////////////////////////////////////////////////
	
    /**
     * Nav Drawer - Fragment managing the behaviors, interactions and presentation.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    
    /**
     * Rail Service - Interface to layout server
     */
    private Bundle mSrvrIP;
    private ServiceConnection mRailSrvcConnection = null;
    private boolean mSrvcBound = false;
    private Messenger mClientToSrvcMsgr = null;
 	final Messenger mClientFmSrvcMsgr = new Messenger(new SrvcMsgHandler());
 	


	//////////////////////////////////////////////////////////////////////
	// MAIN Life Cycle Call Backs
	//////////////////////////////////////////////////////////////////////
	
	/**
	 * OnCreate method
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (L) Log.i(TAG, "onCreate" + (null == savedInstanceState ? " No saved state" : " Restored state"));

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        
		//Check if network is up -
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null) {
			Toast.makeText(getApplicationContext(), "No Network Connection", Toast.LENGTH_SHORT).show();
		}
    }

	@Override
	public void onRestart() {
		super.onRestart();
		if (L) Log.i(TAG, "onRestart");
	}

	@Override
	public void onStart() {
		super.onStart();
		if (L) Log.i(TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (L) Log.i(TAG, "onResume");
//		new CreateCommThreadTask().execute();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (L) Log.i(TAG, "onPause");
//		new CloseSocketTask().execute();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (L) Log.i(TAG, "onStop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (L) Log.i(TAG, "onDestroy");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (L) Log.i(TAG, "onSaveInstanceState");
	}

	@Override
	public void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		if (L) Log.i(TAG, "onRestoreInstanceState");
	}


	
	//
	// Page navigation Interface
	//
	
	/**
	 * Nav Item Selected: Notification that an item has been selected from the nav drawer.
	 */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
    	
    	FragmentManager fMgr = getFragmentManager();
    	fMgr.enableDebugLogging(true);
    	FragmentTransaction SetMainView = fMgr.beginTransaction();
    	int bStack;

    	// update the main content by replacing fragments
    	switch (position) {
	    	case 0:
	            SetMainView.replace(R.id.main_container, NetFragment.newInstance())
	            .commit();
	    		break;
	    	case 1:
	    		RosterFragment rFrag = (RosterFragment) fMgr.findFragmentByTag("ROSTER");
	    		if (null == rFrag) {
	    			rFrag = RosterFragment.newInstance();
	    		}
	            SetMainView.replace(R.id.main_container, rFrag, "ROSTER");
	            SetMainView.addToBackStack("ROSTER");
	            SetMainView.commit();

	            Fragment frag0 = fMgr.findFragmentById(R.id.main_container);
	    		bStack = fMgr.getBackStackEntryCount();
	    		break;
	    	case 2:
	            SetMainView.replace(R.id.main_container, PlaceholderFragment.newInstance(3))
	            .commit();
	    		break;
	    	case 3:
	    		CabFragment cFrag = (CabFragment) fMgr.findFragmentByTag("CAB");
	    		if (null == cFrag) {
	    			cFrag = CabFragment.newInstance(tNum);
	    		}
    			SetMainView.replace(R.id.main_container, cFrag, "CAB");
    			SetMainView.addToBackStack("CAB");
    			SetMainView.commit();
    			
	    		Fragment frag1 = fMgr.findFragmentById(R.id.main_container);
	    		bStack = fMgr.getBackStackEntryCount();
	    		break;

    	}
    }

    /**
     * Nav Section Attached: Called when new page is attached. Sets Page title to show in action bar.
     * @param number - Page number
     */
    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4);
                break;
        }
    }


    //
    // Action bar interface.
    //

    /**
     * onCreateOptionsMenu notification
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (L) Log.i(TAG, "onCreateOptionsMenu");

        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Restore ActionBar after page change with updated title
     */
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    /**
     * on OptionsItemSelected notification
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(getApplicationContext(), "Settings Dialog TBD", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_about:
                final LayoutInflater puff =
                        (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final PopupWindow abtWin =
                        new PopupWindow(
                                puff.inflate(R.layout.dialogue_about, null, false),
                                620,
                                500);
                abtWin.showAtLocation(
                        this.findViewById(R.id.main_container),
                        Gravity.CENTER,
                        0,0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    //
    // Application page interfaces
    //
    
    /**
     * Server change listener.
     */
    @Override
    public void onServerChange(Bundle srvrIP) {
    	if (L) Log.i(TAG,"onServerChange");
    	
    	/* Save server IP info for after the service is started.*/
    	mSrvrIP = srvrIP;
    	
    	if (mSrvrIP.getBoolean("IP_CNCT")) {
	    	// TODO Implement a JMRI service. Service type will be in the Bundle.
	    	// For now assume MorBus always
	    	// TODO Restart service if in connected state but no service bound.
    		
	    	/* Start up the service unless there's one running already */
			if (!mSrvcBound) {
		    	mRailSrvcConnection = new MBusService();
		    	Intent mbusIntent = new Intent(this, com.olinsdepot.mbus_srvc.MbusService.class);
				bindService(mbusIntent, mRailSrvcConnection, Context.BIND_AUTO_CREATE);
			 }
    	} else {
    		/* Start process to shut down the service if it's running. */
    		if (mSrvcBound) {
    			Message msg = Message.obtain();
    			msg.what = MbusSrvcCmd.SRVR_DSCNCT.toCode();
    			try {
    				mClientToSrvcMsgr.send(msg);
    			} catch (RemoteException e) {
    				e.printStackTrace();
    			}
    		}
    	}
    }

	/**
	 * Roster change listener. Acquire or release a DCC decoder.
	 */
	public void onRosterChange(int tID, Bundle dcdrState) {
    	if (L) Log.i(TAG,"onRosterChange");
        
        /* If rail service not connected, do nothing. */
        if (!mSrvcBound) return;
        
        /* Send message to the service to acquire or release the decoder 
         * passed in the bundle. Assign loco to a throttle in the Cab.
         */
        //TODO Re-write roster and Morbus service to use LocoUnit class.
        Message msg = Message.obtain();
        if (dcdrState.getBoolean("DCDR_CNCT")) {
            msg.what = MbusSrvcCmd.DCC_ACQ_DCDR.toCode();
            CabFragment.cabAssign(tID, dcdrState.getString("DCDR_NAME"));
        } else {
            msg.what = MbusSrvcCmd.DCC_RLS_DCDR.toCode();
            CabFragment.cabRelease(tID);
        }
        msg.arg1 = tID;
        msg.obj = dcdrState;
        try {
            mClientToSrvcMsgr.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
	}

    /**
	 * Throttle change listener
	 */
	@Override
	public void onThrottleChange(int tID, int tCmd, int arg) {

        // If no Morbus service connected, do nothing.
        if (!mSrvcBound) return;
        if(tCmd == 0) {
//            Toast.makeText(getApplicationContext(), "ID="+tID+" Speed="+arg, Toast.LENGTH_SHORT).show();
	        // Create and send a message to the service, using a supported 'what' value
	        Message msg = Message.obtain(null, MbusSrvcCmd.DCC_THTL_STEP.toCode(), tID, arg);
	        try {
	            mClientToSrvcMsgr.send(msg);
	        } catch (RemoteException e) {
	            e.printStackTrace();
	        }
        } else if(tCmd == 1) {
	        // Create and send a message to the service, using a supported 'what' value
	        Message msg = Message.obtain(null, MbusSrvcCmd.DCC_FUNC_KEY.toCode(), tID, arg);
	        try {
	            mClientToSrvcMsgr.send(msg);
	        } catch (RemoteException e) {
	            e.printStackTrace();
	        }
        	
        }
	}
    
    //
    // MorBus service interface.
    //
    
    /**
     * Class for connecting to the MBus service.
     */
    private class MBusService implements ServiceConnection {
    	
    	/* Called when service connects. */
    	public void onServiceConnected(ComponentName className, IBinder service) {
    		if(L) Log.i(TAG, "onServiceConnected - " + className);
    		
    		/* Register the service's Client message handler. */
    		mClientToSrvcMsgr = new Messenger(service);
    		mSrvcBound = true;
    		
    		/* Send the service the connect command. Include saved server info
    		 * and the handler for messages from the service.
    		 */
    		Message msg = Message.obtain();
    		msg.what = MbusSrvcCmd.SRVR_CNCT.toCode();
    		msg.obj = mSrvrIP;
    		msg.replyTo  = mClientFmSrvcMsgr;
            try {
               mClientToSrvcMsgr.send(msg);
            } catch (RemoteException e) {
               e.printStackTrace();
            }
    	}
    	
    	/* Called when service disconnects unexpectedly. */
    	public void onServiceDisconnected(ComponentName className) {
    		Log.d(TAG, "onServiceDisconnected - " + className);
    		mClientToSrvcMsgr = null;
    		mSrvcBound = false;
    	}
    };
    
    
	/**
	 * Handler for messages from MBus service to Client. Receives events from
	 * MBus service to Main thread to be parsed for display on the GUI.
	 * 
	 */
	class SrvcMsgHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i("MsgToClient", "Event Received = " + msg.what);
			
			switch (MbusSrvcEvt.fromCode(msg.what)) {
			
			case SRVR_CNCTD:
				/* Announce service startup */
				Toast.makeText(getApplicationContext(), "Mbus Service Started", Toast.LENGTH_SHORT).show();
				break;
				
			case SRVR_DSCNCTD:
				break;
				
			case SRVR_PWR_IS:
				break;
				
			case DCC_DCDR_ACQD:
				break;
				
			case DCC_DCDR_RLSD:
				break;

			default:
				super.handleMessage(msg);

			}
		}
	}

}
