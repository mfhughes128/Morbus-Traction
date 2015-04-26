package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.Fragment;
//import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * The Roster fragment gets the information from user to define a decoder.
 * When "Acquire" button pressed, Calls onRosterChange in main with a bundle
 * defining the target decoder to be passed on to the Morbus service which
 * registers the decoder as connected to the throttle passed in Arg1. When
 * the button is pressed again, a new message is sent to the service and
 * the decoder is released. 
 */
public class RosterFragment extends Fragment {
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	// Container Activity must implement this interface
	public interface OnRosterChangeListener{
		/**
		 * Called when User selects a decoder and hits the "Acquire" button
		 */
		public void onRosterChange(int tID, Bundle dcdrState);
	}
	private OnRosterChangeListener rosterListener;

	/**
	 * Link back to Main activity, set when fragment is attached.
	 */
	MainActivity mActivity;
	
	 
	/* Links to fields in the user UI */
	//TODO re-write this to use frames and handle 1 to 4 throttles.
	private EditText mDcdrName1;
	private EditText mDcdrTyp1;
    private EditText mDcdrAddr1;
    private EditText mDcdrStep1;
    private int mDcdrFkey1 = 0;
    private ToggleButton mDcdrSelBtn1;
    private EditText mDcdrName2;
    private EditText mDcdrTyp2;
    private EditText mDcdrAddr2;
    private EditText mDcdrStep2;
    private int mDcdrFkey2 = 0;
    private ToggleButton mDcdrSelBtn2;

	/* Bundle to preserve UI state. */
	private static Bundle rosterState = null;
	private static boolean btn1State = false;
	private static boolean btn2State = false;
	
	/* Fields in the status bundle */
//	private static final String THTL =	"THTL_ID";
	private static final String NAME =	"DCDR_NAME";
	private static final String ADDR =	"DCDR_ADR";
	private static final String STATE =	"DCDR_CNCT";
	private static final String TYPE =	"ADR_TYP";
	private static final String STEPS =	"SPD_STEPS";
	private static final String FKEYS=	"KEY_STATES";

	 /**
	  * Null constructor for this fragment
	  */
	 public RosterFragment() { }
	 
	 /**
	  * Returns a new instance of the ROSTER view fragment
	  */
	 public static RosterFragment newInstance() {
		 RosterFragment thisfrag = new RosterFragment();
		 return thisfrag;
	 }

	 
	 //
	 // Fragment life cycle methods
	 //

	/**
     * On main app attached
     */
   	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
        
        // Open interface to container fragment.
        try {
            rosterListener = (OnRosterChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Server Change Listener");
        }

        // Give main the section number so it can update the Action Bar title.
        this.mActivity = (MainActivity) activity;
        this.mActivity.onSectionAttached(2);

    }

	/**
     * On fragment create
     */
    @Override
    public void onCreate(Bundle saved) {
    	super.onCreate(saved);
    	if (L) Log.i(TAG, "onCreate");
    }


	/**
     * On create view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (L) Log.i(TAG, "onCreateView");

        /* Get the roster view and connect to the fields in it. */
        View rosterFragView = inflater.inflate(R.layout.fragment_roster, container, false);
        
        mDcdrName1 = (EditText) rosterFragView.findViewById(R.id.dcdrName1);
        mDcdrTyp1 = (EditText) rosterFragView.findViewById(R.id.dcdrTyp1);
        mDcdrAddr1 = (EditText) rosterFragView.findViewById(R.id.dcdrAdr1);
        mDcdrStep1 = (EditText) rosterFragView.findViewById(R.id.dcdrStep1);
		mDcdrSelBtn1 = (ToggleButton) rosterFragView.findViewById(R.id.dcdrSel1);

		mDcdrSelBtn1.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				     if (L) Log.i(TAG, "onClick Connect");
				     
				     // Handle bogus call when button state is restored in onRestoreInstance
				     if (isChecked == btn1State) {
				    	 return;
				     }
				     
				     // Legitimate button state change
				     final Bundle dcdrState = new Bundle();
				     
				     if (isChecked) {
				    	 btn1State = true;
//				    	 dcdrState.putInt(THTL, 0);
				    	 dcdrState.putBoolean(STATE, btn1State);
				    	 dcdrState.putString(NAME, mDcdrName1.getText().toString());
				    	 dcdrState.putInt(TYPE, Integer.parseInt(mDcdrTyp1.getText().toString()));
				    	 dcdrState.putInt(ADDR, Integer.parseInt(mDcdrAddr1.getText().toString()));
				    	 dcdrState.putInt(STEPS, Integer.parseInt(mDcdrStep1.getText().toString()));
				    	 dcdrState.putInt(FKEYS, mDcdrFkey1);
				     }
				     else {
				    	 btn1State = false;
//				    	 dcdrState.putInt(THTL, 0);
				    	 dcdrState.putBoolean(STATE,  btn1State);
				    	 dcdrState.putString(NAME, null);
				    	 dcdrState.putInt(ADDR, 0);
				    	 dcdrState.putInt(STEPS, 0);
				    	 dcdrState.putInt(FKEYS, 0);
				     }
				     
				     rosterListener.onRosterChange(0, dcdrState);
				}
			}
		);

        mDcdrName2 = (EditText) rosterFragView.findViewById(R.id.dcdrName2);
        mDcdrTyp2 = (EditText) rosterFragView.findViewById(R.id.dcdrTyp2);
		mDcdrAddr2 = (EditText) rosterFragView.findViewById(R.id.dcdrAdr2);
        mDcdrStep2 = (EditText) rosterFragView.findViewById(R.id.dcdrStep2);
		mDcdrSelBtn2 = (ToggleButton) rosterFragView.findViewById(R.id.dcdrSel2);

		mDcdrSelBtn2.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				     if (L) Log.i(TAG, "onClick Connect");
				     
				     // Handle bogus call when button state is restored in onRestoreInstance
				     if (isChecked == btn2State) {
				    	 return;
				     }
				     
				     // Legitimate button state change
				     final Bundle dcdrState = new Bundle();

				     if (isChecked) {
				    	 btn2State = true;
//				    	 dcdrState.putInt(THTL, 1);
				    	 dcdrState.putBoolean(STATE, btn2State);
				    	 dcdrState.putString(NAME, mDcdrName2.getText().toString());
				    	 dcdrState.putInt(TYPE, Integer.parseInt(mDcdrTyp2.getText().toString()));
				    	 dcdrState.putInt(ADDR, Integer.parseInt(mDcdrAddr2.getText().toString()));
				    	 dcdrState.putInt(STEPS, Integer.parseInt(mDcdrStep2.getText().toString()));
				    	 dcdrState.putInt(FKEYS, mDcdrFkey2);
				     }
				     else {
				    	 btn2State = false;
//				    	 dcdrState.putInt(THTL, 1);
				    	 dcdrState.putBoolean(STATE, btn2State);
				    	 dcdrState.putString(NAME, null);
				    	 dcdrState.putString(ADDR, null);
				    	 dcdrState.putInt(ADDR, 0);
				    	 dcdrState.putInt(STEPS, 0);
				    	 dcdrState.putInt(FKEYS, 0);
				     }

				     rosterListener.onRosterChange(1, dcdrState);
				}
			}
		);
                
        return rosterFragView;
    }

    
	/**
     * On activity created
     */
    @Override
    public void onActivityCreated(Bundle saved) {
    	super.onActivityCreated(saved);
    	if (L) Log.i(TAG, "onActivityCreated");
    }
    

	/**
     * On fragment start
     */
    @Override
    public void onStart() {
    	super.onStart();
    	if (L) Log.i(TAG, "onStart");
    }

    
	/**
     * On fragment resume
     */
    @Override
    public void onResume() {
    	super.onResume();
    	if (L) Log.i(TAG, "onResume");
    	onRestoreInstanceState(rosterState);
    	rosterState = null;
    }
    

	/**
     * On fragment pause
     */
    @Override
    public void onPause() {
    	super.onPause();
    	if (L) Log.i(TAG, "onPause");
    	rosterState = new Bundle();
    	onSaveInstanceState(rosterState);
    }
    

	/**
     * On fragment stop
     */
    @Override
    public void onStop() {
    	super.onStop();
    	if (L) Log.i(TAG, "onStop");
    }
    

	/**
     * On save instance state
     */
    @Override
    public void onSaveInstanceState(Bundle toSave) {
    	super.onSaveInstanceState(toSave);
    	if (L) Log.i(TAG, "onSaveInstanceState");
    	
    	final Bundle dcdr1 = new Bundle();
		dcdr1.putBoolean(STATE, btn1State);
		dcdr1.putString(NAME, mDcdrName1.getText().toString());
	   	dcdr1.putString(TYPE, mDcdrTyp1.getText().toString());
	   	dcdr1.putString(ADDR, mDcdrAddr1.getText().toString());
	   	dcdr1.putString(STEPS, mDcdrStep1.getText().toString());
	   	dcdr1.putInt(FKEYS, mDcdrFkey1);

		final Bundle dcdr2 = new Bundle();
		dcdr2.putBoolean(STATE, btn2State);
		dcdr2.putString(NAME, mDcdrName2.getText().toString());
		dcdr2.putString(TYPE, mDcdrTyp2.getText().toString());
		dcdr2.putString(ADDR, mDcdrAddr2.getText().toString());
		dcdr2.putString(STEPS, mDcdrStep2.getText().toString());
		dcdr2.putInt(FKEYS, mDcdrFkey2);
		
		toSave.putBundle("THTL1", dcdr1);
		toSave.putBundle("THTL2", dcdr2);
    }
    
	/**
     * On restore instance state
     */
    private void onRestoreInstanceState(Bundle fromSave) {
    	if (L) Log.i(TAG, "onRestoreInstanceState");
    	
		if (fromSave != null) {
			final Bundle dcdr1 = fromSave.getBundle("THTL1");
			btn1State = dcdr1.getBoolean(STATE);
			mDcdrSelBtn1.setChecked(btn1State);
			mDcdrName1.setText(dcdr1.getString(NAME));
			mDcdrTyp1.setText(dcdr1.getString(TYPE));
			mDcdrAddr1.setText(dcdr1.getString(ADDR));
			mDcdrStep1.setText(dcdr1.getString(STEPS));
			mDcdrFkey1 = dcdr1.getInt(FKEYS);

			final Bundle dcdr2 = fromSave.getBundle("THTL2");
			btn2State = dcdr2.getBoolean(STATE);
			mDcdrSelBtn2.setChecked(btn2State);
			mDcdrName2.setText(dcdr2.getString(NAME));
			mDcdrTyp2.setText(dcdr2.getString(TYPE));
			mDcdrAddr2.setText(dcdr2.getString(ADDR));
			mDcdrStep2.setText(dcdr2.getString(STEPS));
			mDcdrFkey2 = dcdr2.getInt(FKEYS);
		}
    }

}