package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * The Roster fragment gets the decoder address and characteristics from user.
 */
public class NetFragment extends Fragment {
	
	//Logging
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	// Container Activity must implement this interface
	public interface OnServerChangeListener{
		public void onServerChange(Bundle srvrIP);
	}

	/**
	 * Called when User hits the connect button with a valid IP
	 */
	private OnServerChangeListener netListener;

	/**
	 * Link back to Main activity, set when fragment is attached.
	 */
	private MainActivity mActivity;
	
	// Links to fields in the user UI.
	private EditText mSrvrName;
    private EditText mHostName;
    private EditText mHostPort;    
	private ToggleButton mSrvrCnctBtn;

	// Bundle to preserve and communicate UI status.
	private static Bundle srvrState = null;
	private static boolean btnState = false;
	
	// Fields in the status bundle
	private static final String NAME = "SRV_NAME";
	private static final String ADDR = "IP_ADR";
	private static final String PORT = "IP_PORT";
	private static final String STATE = "IP_CNCT";

	 
	 /**
	  * Null constructor for this fragment
	  */
	 public NetFragment() { }
	 
	 /**
	  * Returns a new instance of the NET view fragment
	  */
	 public static NetFragment newInstance() {
		 NetFragment thisfrag = new NetFragment();
		 return thisfrag;
	 }


	//
    // Life cycle methods for the NET fragment
     //
    
	/**
	 * On Attach method(non-Javadoc)
	 * @see android.app.Fragment#onAttach(android.app.Activity)
	 */
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
        
        // Open interface to container fragment.
        try {
            netListener = (OnServerChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Server Change Listener");
        }

        // Give main the section number so it can update the Action Bar title.
        this.mActivity = (MainActivity) activity;
        this.mActivity.onSectionAttached(1);

    }

    /**
     *  On Create method
     */
    @Override
    public void onCreate(Bundle fromSave) {
    	super.onCreate(fromSave);
    	if (L) Log.i(TAG, "onCreate");
    	
//    	onRestoreInstanceState(fromSave);
    	
    }


	/**
	 *  On CreateView method
	 */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle fromSave) {
        if (L) Log.i(TAG, "onCreateView");

        View netFragView = inflater.inflate(R.layout.fragment_net, container, false);
        mSrvrName = (EditText) netFragView.findViewById(R.id.srvrName);
        mHostName = (EditText) netFragView.findViewById(R.id.hostName);
        mHostPort = (EditText) netFragView.findViewById(R.id.hostPort);
		mSrvrCnctBtn = (ToggleButton) netFragView.findViewById(R.id.srvrConnect);
		
		mSrvrCnctBtn.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (L) Log.i(TAG, "onClick Connect Host");

				     // Handle bogus call when button state is restored in onRestoreInstance
				     if (isChecked == btnState) {
				    	 return;
				     }
				     
				     // Legitimate button state change
					Bundle srvrIP = new Bundle();
					
					if (isChecked) {
						btnState = true;
						srvrIP.putBoolean(STATE, true);
						srvrIP.putString(NAME, mSrvrName.getText().toString());
						srvrIP.putString(ADDR, mHostName.getText().toString());
						srvrIP.putString(PORT, mHostPort.getText().toString());
					}
					else {
						btnState = false;
						srvrIP.putBoolean(STATE, false);
						srvrIP.putString(NAME, null);
						srvrIP.putString(ADDR, null);
						srvrIP.putString(PORT, null);
					}

					netListener.onServerChange(srvrIP);
				}
			}
		);

 //   	onRestoreInstanceState(fromSave);

    	return netFragView;
    }

    
    // On ActivityCreated method
    @Override
    public void onActivityCreated(Bundle fromSave) {
    	super.onActivityCreated(fromSave);
    	if (L) Log.i(TAG, "onActivityCreated");
    	
//    	onRestoreInstanceState(fromSave);

    }
    

    // On Start method
    @Override
    public void onStart() {
    	super.onStart();
    	if (L) Log.i(TAG, "onStart");
    }

    
    // On Resume method
    @Override
    public void onResume() {
    	super.onResume();
    	if (L) Log.i(TAG, "onResume");
    	
    	onRestoreInstanceState(srvrState);
    	srvrState = null;

    }
    

    // On Pause method
    @Override
    public void onPause() {
    	super.onPause();
    	if (L) Log.i(TAG, "onPause");
    	
    	srvrState = new Bundle();
    	onSaveInstanceState(srvrState);
    }
    

    // On Stop method
    @Override
    public void onStop() {
    	super.onStop();
    	if (L) Log.i(TAG, "onStop");
    }
    

    // On Save Instance State method
    @Override
    public void onSaveInstanceState(Bundle toSave) {
    	super.onSaveInstanceState(toSave);
    	if (L) Log.i(TAG, "onSaveInstanceState");
    	toSave.putBoolean(STATE, btnState);
    	toSave.putString(NAME, mSrvrName.getText().toString());
    	toSave.putString(ADDR, mHostName.getText().toString());
    	toSave.putString(PORT, mHostPort.getText().toString());
     }
    
    //On Restore Instance State method
    private void onRestoreInstanceState(Bundle fromSave) {
    	if (L) Log.i(TAG, "onRestoreInstanceState");
		if (fromSave != null) {
			mSrvrCnctBtn.setChecked(fromSave.getBoolean(STATE));
			mSrvrName.setText(fromSave.getString(NAME));
			mHostName.setText(fromSave.getString(ADDR));
			mHostPort.setText(fromSave.getString(PORT));
		}

    	
    }
}