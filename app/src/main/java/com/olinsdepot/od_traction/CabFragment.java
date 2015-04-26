package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;


//TODO Customize view for current device orientation

/**
 * The "Cab" fragment containing throttle and I/O views.
 */
public class CabFragment extends Fragment {
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;
	
	/**
	 * Constants
	 */
	private static final int CAB_SECT_NUM = 4;
	/* Max number of Throttles allowed and tags for throttle instances. */
	private static final int T_NUM_MAX = 4;
	private static final String THRTL_TAG[] = {"THRTL1","THRTL2","THRTL3","THRTL4"};

	/**
	 * Cab view state variables
	 */
	private int tNum;	/* Number of throttle views assigned to the cab. */
 	private static String rosterUnit[] = new String[T_NUM_MAX]; /* The locos assigned to the throttles. */

	/**
	 * Keys for argument and instance state bundles.
	 */
	private static final String NUM_T = "NUM_THROTTLES";
	private static final String U_RSTR = "UNITS";
	
	
	//////////////////////////////////////////////////////////////////////
	// Public Methods
	//////////////////////////////////////////////////////////////////////
	
    /**
     * Returns a new instance of the CAB view fragment
     * 
     * @param Number of throttle views to display
     * @return an instance of CabFragment
     */
    public static CabFragment newInstance(int numT) {
        CabFragment fragment = new CabFragment();
        Bundle args = new Bundle();
        args.putInt(NUM_T, numT);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Assigns a loco unit to the throttle specified by tID.
     * @param tID
     * @param loco
     */
    public static void cabAssign(int tID, String loco) {
    	if (tID < T_NUM_MAX) {
    	rosterUnit[tID] = loco;
    	}
    }
    
    /**
     * De-assign the throttle specified by tID
     */
    public static void cabRelease(int tID) {
    	if (tID < T_NUM_MAX) {
    	rosterUnit[tID] = null;
    	}
    }
    
    
    //////////////////////////////////////////////////////////////////////
    // Life cycle methods for the CAB fragment
    //////////////////////////////////////////////////////////////////////
    
    /**
     * Fragment is associated with an Activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
        
        /* Give main the section number so it can update the Action Bar title. */
        ((MainActivity) activity).onSectionAttached(CAB_SECT_NUM);

    }

    /**
     * Create
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	if (L) Log.i(TAG, "onCreate" + (null == savedInstanceState ? " No saved state" : " Restored state") + " tNUM = " + tNum);
        tNum = getArguments().getInt(NUM_T);
        
    }
   

    /**
     * CreateView
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (L) Log.i(TAG, "onCreateView" + (null == savedInstanceState ? " No saved state" : " Restored state") + " tNUM = " + tNum);

        final View rootView = inflater.inflate(R.layout.fragment_cab, container, false);

        FragmentManager fragMgr = getChildFragmentManager();
        FragmentTransaction ft = fragMgr.beginTransaction();
        
        /* Handle #1 throttle */
        ThrottleFragment tFrag0 = (ThrottleFragment) fragMgr.findFragmentByTag(THRTL_TAG[0]);
        if (tFrag0 == null) {
        	tFrag0 = ThrottleFragment.newInstance(0, rosterUnit[0]);
        	ft.add(R.id.LEFT_THROTTLE_FRAME, tFrag0, THRTL_TAG[0]);
        } else {
        	ft.attach(tFrag0);
        }
        
        /* Handle #2 throttle */
        ThrottleFragment tFrag1 = (ThrottleFragment) fragMgr.findFragmentByTag(THRTL_TAG[1]);
        if (tFrag1 == null) {
        	tFrag1 = ThrottleFragment.newInstance(1, rosterUnit[1]);
        	ft.add(R.id.RIGHT_THROTTLE_FRAME,  tFrag1, THRTL_TAG[1]);
        } else {
        	ft.attach(tFrag1);
        }

        ft.commit();
        
        return rootView;
    }
    
    /**
     * ActivityCreated
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	if (L) Log.i(TAG, "onActivityCreated" + (null == savedInstanceState ? " No saved state" : " Restored state") + " tNUM = " + tNum);
    }
    
    /**
     * Start notification
     */
    @Override
    public void onStart() {
    	super.onStart();
    	if (L) Log.i(TAG, "onStart");
    }
    
    /**
     * Resume notification
     */
    @Override
    public void onResume() {
    	super.onResume();
    	if (L) Log.i(TAG, "onResume");
    }
    
    /**
     * Pause notification
     */
    @Override
    public void onPause() {
    	super.onPause();
    	if (L) Log.i(TAG, "onPause");
    }
    
    /**
     * Stop notification
     */
    @Override
    public void onStop() {
    	super.onStop();
    	if (L) Log.i(TAG, "onStop");
    }
    
    /**
     * Notification to save instance state
     */
    @Override
    public void onSaveInstanceState(Bundle toSave) {
    	super.onSaveInstanceState(toSave);
    	if (L) Log.i(TAG, "onSaveInstanceState");
    }
}