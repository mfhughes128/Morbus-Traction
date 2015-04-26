package com.olinsdepot.od_traction;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.olinsdepot.od_traction.vertical_seekbar.VerticalSeekBar;
import com.rapsacnz.ToggleGroup;


public class ThrottleFragment extends Fragment {
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	/* Container Activity must implement this interface */
	public interface OnThrottleChangeListener{
		/**
		 * Called when UI detects a change in throttle setting.
		 * @param tID
		 * @param tCmd TODO
		 * @param arg
		 */
		public void onThrottleChange(int tID, int tCmd, int arg);
	}
	private OnThrottleChangeListener tListener;
	
	//////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////

	/**
	 * Keys for Argument bundle.
	 */
	private static final String ARG_TID = "Throttle_ID";
	private static final String ARG_LOCO = "Loco_Unit";
	
	/**
	 * Keys for State bundle.
	 */
	private static final String STE_YARD = "Yard_Mode";
	private static final String STE_DIR = "Loco_Dir";
	private static final String STE_SET = "Loco_Step";

	//////////////////////////////////////////////////////////////////////
	// Local Variables
	//////////////////////////////////////////////////////////////////////
	/**
	 *  This throttle's View components
	 */
	private ToggleButton	mYardButton;
	private TextView		mSpdLabel;
	private VerticalSeekBar	mbarThrottle;
	private ToggleGroup		mMotionBtnGroup;
	private ToggleButton	mReverseButton;
	private ToggleButton	mStopButton;
	private ToggleButton	mForwardButton;
	private TableLayout		mFncBtnGrp1;
	private TableLayout		mFncBtnGrp2;
	
	
	/**
	 * This throttle's state variables
	 */
	private int tID;			/* ID for this throttle */
	
	private String unitID;		/* Unit line and #, or consist ID */
	private boolean tYard;		/* Yard mode flag */
	private int tDir;			/* Current selected direction (+1=FOR,-1=REV, 0=STOP)*/
	private int tSet;			/* Current throttle setting (value= 0-126)*/

	private LinkedHashMap<Integer, String> fKeyLabel;	/* Map function key number to text */
	private LinkedHashMap<Integer, Boolean> fKeyToggle;	/* Map function key number to mode */
	private LinkedHashMap<Integer, Button> fKeyButton;	/* Map function key number to button */

	
	/////////////////////////////////////////////////////////////////////////////////////////
	// Public Interface Methods
	////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Factory method to return a new instance of the throttle fragment
     * @param - id for this throttle.
     * @return - instance of this fragment.
     */
    public static ThrottleFragment newInstance(int id, String name) {
        ThrottleFragment tFrag = new ThrottleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TID, id);
        args.putString(ARG_LOCO, name);
        tFrag.setArguments(args);
        return tFrag;
    }
    

    /////////////////////////////////////////////////////////////////////////////////////////
	// Life Cycle Methods
	////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     *  On Attach method
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
 
        // Open interface to container fragment.
        try {
            tListener = (OnThrottleChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Throttle Change Listener");
        }

   }
	
	/**
	 *  On Create method(non-Javadoc)
	 * @see android.app.Fragment#onCreate(android.os.Bundle)
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (L) Log.i(TAG, "onCreate" + (null == savedInstanceState ? " No saved state" : " Restored state") + " ID = " + tID);
        
		/* Get Throttle ID argument passed when instance created. */
        tID = getArguments().getInt(ARG_TID);

        /* Restore instance state: Init to default first time, to saved values thereafter */
		if (savedInstanceState == null) {
			tYard = false;
			tDir = 0;
			tSet = 0;
		} else {
			tYard = savedInstanceState.getBoolean(STE_YARD);
			tDir = savedInstanceState.getInt(STE_DIR);
			tSet = savedInstanceState.getInt(STE_SET);
		}
	}
	
	/**
	 *  On Create View method
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		if (L) Log.i(TAG, "onCreateView" + (null == savedInstanceState ? " No saved state" : " Restored state") + " ID = " + tID);

        final View tFragView = inflater.inflate(R.layout.fragment_throttle, container, false);
        
        unitID = getArguments().getString(ARG_LOCO);
        
        if (unitID != null) {
        	
	        TextView tRoadID = (TextView) tFragView.findViewById(R.id.hdr_unit_id);
			tRoadID.setText(getArguments().getString(ARG_LOCO));
	
			mSpdLabel = (TextView) tFragView.findViewById(R.id.speed_step);
			
			mbarThrottle = (VerticalSeekBar)tFragView.findViewById(R.id.Throttle);
			mbarThrottle.setMax(126);
			mbarThrottle.setProgress(tSet);
			mbarThrottle.setOnSeekBarChangeListener(
				new VerticalSeekBar.OnSeekBarChangeListener() {

					int speed;

					@Override
					public void onStopTrackingTouch(VerticalSeekBar seekBar) {
						//TODO Add sound or haptic feedback to throttle.
					}
	
					@Override
					public void onStartTrackingTouch(VerticalSeekBar seekBar) {
						//TODO Add sound or haptic feedback to throttle.
					}
	
					@Override
					public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
						tSet = progress;
						speed = tDir * tSet;
						mSpdLabel.setText(Integer.toString(tSet));
						tListener.onThrottleChange(tID, 0, speed);
			
					}
				}
			);

			mMotionBtnGroup = (ToggleGroup) tFragView.findViewById(R.id.motion_btn_grp);
			mMotionBtnGroup.setOnCheckedChangeListener(
					new ToggleGroup.OnCheckedChangeListener() {
						
						@Override
						public void onCheckedChanged(ToggleGroup group, int checkedId) {

							int speed;
							
							switch (checkedId) {

							case R.id.BTNSTOP:
								tDir = 0;
								tSet = 0;
								mbarThrottle.setProgress(tSet);
								//TODO should send eStop
								break;
									
							case R.id.BTNFWD:
								tDir = 1;
								break;
								
							case R.id.BTNREV:
								tDir = -1;
								break;
								
							default :
								tDir = 0;
								tSet = 0;
								mbarThrottle.setProgress(tSet);
								break;
							}			
							speed = tDir * tSet;
							tListener.onThrottleChange(tID, 0, speed);
						}
					});
						
			mForwardButton = (ToggleButton) tFragView.findViewById(R.id.BTNFWD);
			if (tDir == 1) {
				mForwardButton.setChecked(true);
			}
			mForwardButton.setEnabled(true);

			mReverseButton = (ToggleButton) tFragView.findViewById(R.id.BTNREV);
			if (tDir == -1) {
				mReverseButton.setChecked(true);
			}
			mReverseButton.setEnabled(true);
	
			mStopButton = (ToggleButton) tFragView.findViewById(R.id.BTNSTOP);
			if (tDir == 0) {
				mStopButton.setChecked(true);
			}
			mStopButton.setEnabled(true);
			
			setFkeys(tFragView);

        }
		return tFragView;
	}
	
	/**
	 *  On Activity created
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
        if (L) Log.i(TAG, "onActivityCreated" + (null == savedInstanceState ? " No saved state" : " Restored state") + " ID = " + tID);
	}
	
	/**
	 *  On Start
	 */
	@Override
	public void onStart() {
		super.onStart();
        if (L) Log.i(TAG, "onStart");
	}
	
	/**
	 *  On Resume method
	 */
	@Override
	public void onResume()
	{
		super.onResume();
        if (L) Log.i(TAG, "onResume");
	}
	
	/**
	 *  On Pause method
	 */
	@Override
	public void onPause()
	{
		super.onPause();
        if (L) Log.i(TAG, "onPause");
	}
	
	/**
	 *  On Stop method
	 */
	@Override
	public void onStop() {
		super.onStop();
		if (L) Log.i(TAG, "onStop");
	}
	
	/**
	 *  Save Instance State method
	 */
	@Override
	public void onSaveInstanceState(Bundle toSave) {
		super.onSaveInstanceState(toSave);
		if (L) Log.i(TAG, "onSaveInstanceState");
		
		toSave.putBoolean(STE_YARD, tYard);
		toSave.putInt(STE_DIR, tDir);
		toSave.putInt(STE_SET, tSet);
	}
	
	
    ////////////////////////////////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Set Function Keys
	 * 
	 * Assign function key numbers, text and handlers
	 * to the arrays of function buttons.
	 * 
	 * @param view
	 * @return void
	 */
	private void setFkeys(View throttleView) {
		ViewGroup fKeyTbl;
		ViewGroup fKeyRow;
		fncBtnListener ftbl;
		Button fKeyBtn;
		
		/* For now, just assign F0, F1 and F2 to Light Bell and Horn and blank the rest */
		int btnNmbr = 0;
		
		/* Setup the group 1 table */
		fKeyTbl = (ViewGroup) throttleView.findViewById(R.id.fnc_btn_grp1);
		for(int row = 0; row < fKeyTbl.getChildCount(); row++) {
			fKeyRow = (ViewGroup) fKeyTbl.getChildAt(row);
			for (int col = 0; col < fKeyRow.getChildCount(); col++) {
				fKeyBtn = (Button) fKeyRow.getChildAt(col);
				if (btnNmbr == 0) {
					fKeyBtn.setText("Light");
					fKeyBtn.setOnClickListener(new fncBtnListener(btnNmbr));
//					fKeyBtn.setVisibility(View.VISIBLE);
					fKeyBtn.setEnabled(true);
				} else if (btnNmbr == 1) {
					fKeyBtn.setText("Bell");
					fKeyBtn.setOnClickListener(new fncBtnListener(btnNmbr));
//					fKeyBtn.setVisibility(View.VISIBLE);
					fKeyBtn.setEnabled(true);					
				} else if (btnNmbr == 2) {
					fKeyBtn.setText("Horn");
					fKeyBtn.setOnClickListener(new fncBtnListener(btnNmbr));
//					fKeyBtn.setVisibility(View.VISIBLE);
					fKeyBtn.setEnabled(true);
				} else {
					fKeyBtn.setEnabled(false);
//					fKeyBtn.setVisibility(View.GONE);
				}
				++btnNmbr;
			}
		}

		/* Setup the group 2 table */
		fKeyTbl = (ViewGroup) throttleView.findViewById(R.id.fnc_btn_grp2);
		for(int row = 0; row < fKeyTbl.getChildCount(); row++) {
			fKeyRow = (ViewGroup) fKeyTbl.getChildAt(row);
			for (int col = 0; col < fKeyRow.getChildCount(); col++) {
				fKeyBtn = (Button) fKeyRow.getChildAt(col);
				fKeyBtn.setEnabled(false);
//				fKeyBtn.setVisibility(View.GONE);
			}
		}
	}
	
	/**
	 * Function Button Handler
	 * 
	 * Sends function number assigned to this button to Morbus service.
	 * @author mhughes
	 *
	 */
	public class fncBtnListener implements OnClickListener {
		
		/* State for this button */
		private int funcNum;
		
		/* Constructor assigns func number to this instance*/
		public fncBtnListener(int func) {
			funcNum = func;
		}
		
		/* On Click Function sends function message*/
		public void onClick(View btn) {
			tListener.onThrottleChange(tID, 1, funcNum);			
		}
		
	}
}
