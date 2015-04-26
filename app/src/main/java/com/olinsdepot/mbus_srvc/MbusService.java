package com.olinsdepot.mbus_srvc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.olinsdepot.mbus_srvc.CommsThread.*;
import com.olinsdepot.od_traction.MainActivity.*;


/**
 *  The MorBus Service translates the generic functions, (speed step, headlight on, etc.)
 *  produced by user action on the GUI into commands to a remote MorBus server. The service
 *  receives events from the main activity GUI and translates then into commands to the EmCAN
 *  layer below which is connected to the remote server via LAN. The service translates responses
 *  received from the EmCAN layer into events passed to the main activity which then updates the
 *  state of the GUI. Work done in the service is performed on separate threads to avoid blocking
 *  the GUI. 
 *  
 * @author mhughes
 *
 */
public class MbusService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	private static final boolean L = true;
	

	/**
	 *  Morbus service commands
	 */
	public static enum MbusSrvcCmd {
		SRVR_CNCT,
		SRVR_DSCNCT,
		SRVR_PWR_ON,
		SRVR_PWR_OFF,
		SRVR_PWR_IS,
		SRVR_EMRG_STOP,
		DCC_BCST_RESET,
		DCC_ACQ_DCDR,
		DCC_RLS_DCDR,
		DCC_RST_DCDR,
		DCC_THTL_STEP,
		DCC_HARD_STOP,
		DCC_FUNC_KEY,
		UNKNOWN;
		
		/* Returns the code for this MorBus Service command */
	   public int toCode() {
			return this.ordinal();
		}
		
		/* Returns the MorBus Service command for the code passed */
		public static MbusSrvcCmd fromCode(int cmd) {
			if(cmd < MbusSrvcCmd.UNKNOWN.ordinal()) {
				return MbusSrvcCmd.values()[cmd];
			} else {
				return MbusSrvcCmd.UNKNOWN;
			}
		}
	}
	
	/**
	 * Morbus service events
	 */
	public static enum MbusSrvcEvt {
		SRVR_CNCTD,
		SRVR_DSCNCTD,
		SRVR_PWR_IS,
		DCC_DCDR_ACQD,
		DCC_DCDR_RLSD,
		UNKNOWN;

		/* Return the code for this MorBus Service event. */
		public int toCode() {
			return this.ordinal();
		}
		
		/* Return the MorBus Service event for the code passed. */
		public static MbusSrvcEvt fromCode(int evt) {
			if(evt < MbusSrvcEvt.UNKNOWN.ordinal()) {
				return MbusSrvcEvt.values()[evt];
			} else {
				return MbusSrvcEvt.UNKNOWN;
			}
		}
		
	}
	
	/**
	 * Morbus byte stream commands
	 */
	private static enum MbusStrCmd {
		OFF		(64),
		STOP	(65),
		ON		(66),
		ONOFF	(67),
		MVAL	(68),
		DCC		(69);
		/* Constructor */
		private final int strop;
		private MbusStrCmd(int op) {
			this.strop = op;
		}
		
		/* Returns the code for this Mbus stream command. */
		public int toCode() {
			return this.strop;
		}
	}
	
	/**
	 * Morbus byte stream responses
	 */
	private static enum MbusStrRsp {
		ONOFF	(64),
		MVAL	(65),
		SHORT	(66);
		
		/* Constructor */
		private final int strrsp;
		private MbusStrRsp(int rsp) {
			this.strrsp = rsp;
		}
		
		/* Returns the code for this Mbus stream response. */
		public int toCode() {
			return this.strrsp;
		}
	}
	
	/**
	 * MorBUS broadcast frames
	 */
	private static enum MbusBcstOp {
		OFF		(0),
		STOP	(1),
		ON		(2),
		DCC		(3),
		DCCRES	(4);
		
		/* Constructor */
		private final int bcstop;
		private MbusBcstOp(int op) {
			this.bcstop = op;
		}
		
		/* Returns the code for this Mbus broadcast operation. */
		public int toCode() {
			return this.bcstop;
		}
	}
	
	/**
	 * MorBUS node-specific extended frames
	 */
	private static enum MbusNodeOp {
		DCCINIT	(0),
		DCC		(1),
		DCCRES	(2);
		
		/* Constructor */
		private final int  nodeop;
		private MbusNodeOp(int op) {
			this.nodeop = op;
		}
		
		/* Returns the code for this Mbus node-specific operation. */
		public int toCode() {
			return this.nodeop;
		}
	}


	/*
	 * Thread to handle the upward interface to Main thread. It receives commands
	 * on the SrvcFmClient messenger queue and sends responses to the SrvcToClient
	 * message handler in the Main thread.
	 */
	private HandlerThread mClientMsgHandlerThread;
	private Looper mClientMsgHandlerLooper;
	private Handler mClientMsgHandler;
	private static Messenger mSrvcFmClientMsgr;
	private static Messenger mSrvcToClientMsgr;
	
	/* 
	 * Thread to handle the downward interface to the EmCAN Comms. Responses from
	 * the Comms thread are received on the CommsToSrvc message queue. EmCAN transactions
	 * are sent to the Comms thread via the SrvcToComms messenger queue. 
	 */
	private CommsThread mCommsThread;
	protected static Handler mSrvcFmCommsHandler = new CommsMsgHandler();
	private static Messenger mSrvcToCommsMsgr;

	
	// Registered decoders for throttle commands
	private DCCencoder regDecoders[];


	/*
	 * Service life cycle call backs
	 */
	
	/**
	 *  Create new MorBus service.
	 */
	@Override
	public void onCreate() {
		if (L) Log.i(TAG, "Create MBus Service");
		/* Create thread to dispatch incoming messages from Client side interface. */
		mClientMsgHandlerThread = new HandlerThread("MBusSrvcMgr", Process.THREAD_PRIORITY_BACKGROUND);
		mClientMsgHandlerThread.start();
		mClientMsgHandlerLooper = mClientMsgHandlerThread.getLooper();
		mClientMsgHandler = new ClientMsgHandler(mClientMsgHandlerLooper);
		mSrvcFmClientMsgr = new Messenger(mClientMsgHandler);
		
	}
	
	
	/**
	 * MorBus service binder
	 */
	@Override
	public IBinder onBind(Intent intent) {
		if (L) Log.i(TAG, "Start Mbus Service");
		
		/* Pass this service's Client message handler back. */
		return mSrvcFmClientMsgr.getBinder();
	}
	
	
	/**
	 *  MorBus service unexpectedly shutdown.
	 */
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Mbus Service Stopping");
		Message msg = mClientMsgHandler.obtainMessage();
		msg.what = MbusSrvcEvt.SRVR_DSCNCTD.toCode();
		mClientMsgHandler.sendMessage(msg);

		super.onDestroy();
	}

	
	/*
	 * Handlers
	 */
	
	/**
	 *  Mbus Client Message Handler: Coordinates startup and shutdown of Service components.
	 *  Dispatches messages from the client side.
	 *  by request from the originating thread
	 *  
	 *  @param msg - Message containing request type and ip address of server.
	 */
	private final class ClientMsgHandler extends Handler {
		private final String TAG = this.getClass().getSimpleName();
		private static final boolean L = true;
	
		public ClientMsgHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i(TAG,"MBUS Client Msg Hdlr msg = " + msg.what);
			
			Message mCommsMsg;
		
			/* Dispatch the incoming message based on 'what'. */
			switch (MbusSrvcCmd.fromCode(msg.what)) {
			
			/* Connect to the server. */
			case SRVR_CNCT:
				/* Get IP information for the target server. */
				Bundle mSrvrIP = (Bundle)msg.obj;
				
				/* Register the Client's handler for messages from the service. */
				mSrvcToClientMsgr = msg.replyTo;
	
				/* Extract IP info and create a socket. Start Comms Thread on new Socket. */
				String mAddr = mSrvrIP.getString("IP_ADR");
				int mPort = Integer.parseInt(mSrvrIP.getString("IP_PORT"));
				
				/* Start the Comms thread on the socket. */
				try {
					Socket MbusSrvSocket = new Socket(InetAddress.getByName(mAddr), mPort);
					mCommsThread = new CommsThread(MbusSrvSocket);
					mCommsThread.start();				
				}
				catch (UnknownHostException e) {
					Log.d(TAG, e.getLocalizedMessage());
					//TODO notify user and Main that socket open failed.
				}
				catch (IOException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}
				
				/*Create array to hold DCC encoders registered to each throttle. (4 max); */
				regDecoders = new DCCencoder[4];

				break;
			
			/* Disconnect the server and shut down the service. */
			case SRVR_DSCNCT:
				//TODO Close socket and cancel related tasks, then shutdown the service.
				if (L) Log.i(TAG,"Disconnect command");

				Toast.makeText(getApplicationContext(), "Mbus Server Shutdown Requested", Toast.LENGTH_SHORT).show();
				break;
			
			/* Turn layout power on. */
			case SRVR_PWR_ON:
				mCommsMsg = Message.obtain();
				mCommsMsg.what = CommsCmd.SND_STREAM.toCode();
				mCommsMsg.arg1 = MbusStrCmd.ON.toCode();

				try {
					mSrvcToCommsMsgr.send(mCommsMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				
				break;
				
			/* Turn layout power off. */
			case SRVR_PWR_OFF:
				mCommsMsg = Message.obtain();
				mCommsMsg.what = CommsCmd.SND_STREAM.toCode();
				mCommsMsg.arg1 = MbusStrCmd.OFF.toCode();

				try {
					mSrvcToCommsMsgr.send(mCommsMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				
				break;
				
			/* Ask the server to report the layout power state. */
			case SRVR_PWR_IS:
				mCommsMsg = Message.obtain();
				mCommsMsg.what = CommsCmd.SND_STREAM.toCode();
				mCommsMsg.arg1 = MbusStrCmd.ONOFF.toCode();

				try {
					mSrvcToCommsMsgr.send(mCommsMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				
				break;
				
			case SRVR_EMRG_STOP:
				mCommsMsg = Message.obtain();
				mCommsMsg.what = CommsCmd.SND_STREAM.toCode();
				mCommsMsg.arg1 = MbusStrCmd.STOP.toCode();

				try {
					mSrvcToCommsMsgr.send(mCommsMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				
				break;
				
			case DCC_BCST_RESET:
				break;

			/* 
			 * Register a decoder to throttle in ARG1. OBJ = decoder characteristics
			 * Notify main that decoder was acquired for throttle in ARG1.
			 * */
			case DCC_ACQ_DCDR:
				
				//TODO Check settings to see if we need to send a RESET message to the decoder before acquisition.
				regDecoders[msg.arg1] = new DCCencoder((Bundle)msg.obj);
/*				
	    		try {
	    			mSrvcToClientMsgr.send(Message.obtain(null, MbusSrvcEvt.DCC_DCDR_ACQD.toCode(), msg.arg1));
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
*/
				break;
				
			/*
			 * De-register decoder assigned to the throttle specified by ARG1.
			 * Notify main that decoder was released for throttle in ARG1.
			 * */
			case DCC_RLS_DCDR:
				regDecoders[msg.arg1] =  null;
				
				//TODO Check settings to see if we need to send a STOP message to the decoder before release.
/*				
	    		try {
	    			mSrvcToClientMsgr.send(Message.obtain(null, MbusSrvcEvt.DCC_DCDR_RLSD.toCode(), msg.arg1));
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
*/
				break;
			
			/* Send a reset to the decoder assigned to the throttle specified by ARG1. */
			case DCC_RST_DCDR:
				if(regDecoders[msg.arg1] != null) {
					mCommsMsg = Message.obtain();
					mCommsMsg.what = CommsCmd.SND_BCST.toCode();
					mCommsMsg.arg1 = MbusBcstOp.DCC.toCode();
					mCommsMsg.obj = regDecoders[msg.arg1].DCCreset();
	
					try {
						mSrvcToCommsMsgr.send(mCommsMsg);
		    		} catch (RemoteException e) {
		    			e.printStackTrace();
		    		}
				}
				break;
				
			/* Send throttle step in ARG2 to the decoder registered to the throttle in ARG1. */
			case DCC_THTL_STEP:
				if(regDecoders[msg.arg1] != null) {
					mCommsMsg = Message.obtain();
					mCommsMsg.what = CommsCmd.SND_BCST.toCode();
					mCommsMsg.arg1 = MbusBcstOp.DCC.toCode();
					mCommsMsg.obj = regDecoders[msg.arg1].DCCspeed(msg.arg2);
	
					try {
						mSrvcToCommsMsgr.send(mCommsMsg);
		    		} catch (RemoteException e) {
		    			e.printStackTrace();
		    		}
				}
				
				break;

			/* Send a hard stop command to the decoder registered to the throttle in Arg1. */
			case DCC_HARD_STOP:
				if(regDecoders[msg.arg1] != null) {
					mCommsMsg = Message.obtain();
					mCommsMsg.what = CommsCmd.SND_BCST.toCode();
					mCommsMsg.arg1 = MbusBcstOp.DCC.toCode();
					mCommsMsg.obj = regDecoders[msg.arg1].DCCestop();
	
					try {
						mSrvcToCommsMsgr.send(mCommsMsg);
		    		} catch (RemoteException e) {
		    			e.printStackTrace();
		    		}
				}
				break;
			
			/* Send the function key specified by ARG2 to the decoder registered to the throttle in ARG1. */
			case DCC_FUNC_KEY:
				if(regDecoders[msg.arg1] != null) {
					mCommsMsg = Message.obtain();
					mCommsMsg.what = CommsCmd.SND_BCST.toCode();
					mCommsMsg.arg1 = MbusBcstOp.DCC.toCode();
					mCommsMsg.obj = regDecoders[msg.arg1].DCCfunc(msg.arg2);
	
					try {
						mSrvcToCommsMsgr.send(mCommsMsg);
		    		} catch (RemoteException e) {
		    			e.printStackTrace();
		    		}
				}
				break;
				
			/* Unknown command in message */
			default:
				Log.d("MsgFromClient", "Unknown command type = " + msg.what);
				break;				
			} /* switch(mbusSrvcCmd) */			
		} /* handler method */
	} /* handler class */

	
	/**
	 * Handle messages from CommsThread. Parses server message and generates events
	 * to be sent to the client.
	 * 
	 * @param msg - Message containing byte count and byte data sent from server.
	 */
	static class CommsMsgHandler extends Handler {
		private final String TAG = this.getClass().getSimpleName();
		private static final boolean L = true;

		@Override
		public void handleMessage(Message msg)
		{
			if(L) Log.i(TAG, "Msg from Comms = " + msg.what);
			Message mClientMsg;
			
			/* Dispatch event received from CommsThread. */
			switch (CommsEvt.fromCode(msg.what)) {
			
			/* CommsThread has started, save the messenger thread to send to. */
			case START:
				
				// TODO add state to prevent sending messages to CommsThread before it's connected.
				
				mSrvcToCommsMsgr = msg.replyTo;
				break;
			
			/* CommsThread has received a disconnect command and has stopped. */
			case STOP:
				// TODO think about what to do here.
				break;

			/* CommsThread received ID from server, verifying connection. Notify main. */ 
			case CONNECT:
				
				// TODO CommsThread will send remaining string from ID. Verify == MorBus.
				mClientMsg = Message.obtain();
				mClientMsg.what = MbusSrvcEvt.SRVR_CNCTD.toCode();
				try {
	    			mSrvcToClientMsgr.send(mClientMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				break;
				
			/* All other events. */
    		default:
				/*Unknown event in message */
				Log.d("CommsToSrvcHandler", "Unknown event type");
				super.handleMessage(msg);
    			break;
    			
			}  /*switch (CommsEvt) */
			
			/* Dispatched this message. */
//			msg.recycle();

		}  /* handleMessage(msg) */

	};  /* CommsToSrvcHandler */

}