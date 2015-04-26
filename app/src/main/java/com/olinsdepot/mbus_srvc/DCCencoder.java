package com.olinsdepot.mbus_srvc;

import java.nio.ByteBuffer;
import android.os.Bundle;
import android.util.Log;

/**
 * The DCC encoder object is created with the characteristics, (address etc.)
 * of a specific target decoder. It's methods translate general commands to set speed
 * or functions into DCC command strings specific to this decoder.
 * 
 * @author mhughes
 *
 */
public class DCCencoder {
	
	/* Encoder parameters */
	private byte[] dcdrAdr;
	private int dcdrNumSteps;
	private DCCfunctionkeys fkState;
	
	/* Morbus Stream DCC control byte */
	//TODO rep count to be determined by an app setting
	private static final byte REP_CNT = (byte) 0x05;
	private static final byte REP_FVR = (byte) 0x80;
	
	/* Address Partition Code */
	private static enum ADR_TYP {
		MF_DCDR_SHORT	(0b00000000),
		ACC_DCDR		(0b10000000),
		MF_DCDR_LONG	(0b11000000);

		/* Constructor */
		private final int insType;		
		ADR_TYP (int theInsType) {
			this.insType = theInsType;
		}
		
		/* Return code for this instruction */
		public byte toCode() {
			return (byte) this.insType;
		}
	}
	
	/* Decoder Instruction groups */
	private static enum DCC_INS {
		DCD_CTL		(0b00000000),
		ADV_OPS		(0b00100000),
		RVS_SPD		(0b01000000),
		FWD_SPD		(0b01100000),
		F_GRP_1		(0b10000000),
		F_GRP_2		(0b10100000),
		FTR_EXP		(0b11000000),
		CV_ACCS		(0b11100000);
		
		/* Constructor */
		private final int insType;		
		DCC_INS (int theInsType) {
			this.insType = theInsType;
		}
		
		/* Return code for this instruction */
		public byte toCode() {
			return (byte) this.insType;
		}
	}
	
	/* Decoder Control type instructions */
	private static enum DCD_CTL_INS {
		RESET 			(0b00000),
		HRD_RST			(0b00001),
		SET_FLG			(0b00110),
		SET_ADV_ADR		(0b01010),
		DCD_ACQ_REQ		(0b01111),
		CNST_NORM		(0b10010),
		CNST_REV		(0b10011);
		
		/* Constructor */
		private final int insCode;		
		DCD_CTL_INS (int theInsCode) {
			this.insCode = theInsCode;
		}
		
		/* Return code for this instruction */
		public byte toCode() {
			return (byte) this.insCode;
		}
	}
	
	/* Advanced Ops type instructions */
	private static enum ADV_OPS_INS {
		EXTD_SPD_STEP	(0b11111),
		RSTD_SPD_STEP	(0b11110),
		ANALOG_FUNC		(0b11101);
		
		/* Constructor */
		private final int insCode;		
		ADV_OPS_INS (int theInsCode) {
			this.insCode = theInsCode;
		}
		
		/* Return code for this instruction */
		public byte toCode() {
			return (byte) this.insCode;
		}
	}
	
	/* Feature Expansion type instructions */
	private static enum FTR_EXP_INS {
		BIN_CTL_LONG	(0b00000),
		BIN_CTL_SHORT	(0b11101),
		F_GRP_3			(0b11110),
		F_GRP_4			(0b11111);
		
		/* Constructor */
		private final int insCode;		
		FTR_EXP_INS (int theInsCode) {
			this.insCode = theInsCode;
		}
		
		/* Return code for this instruction */
		public byte toCode() {
			return (byte) this.insCode;
		}
	}

	
	/**
	 * Constructor
	 * 
	 * sets this encoder's characteristics from argument bundle.
	 * 
	 * @param newDcdr  Bundle of decoder characteristics.
	 */
	protected DCCencoder (Bundle newDcdr) {

		int theAddr = newDcdr.getInt("DCDR_ADR");
		int theDcdrTyp = newDcdr.getInt("ADR_TYP");
		int theFkeyStates = newDcdr.getInt("KEY_STATES");
		
		/* Create byte array containing the address for target decoder in correct format. */
		switch (theDcdrTyp) {
		/* Address type 0 - Mobile decoder, short address */
		case 0:
			this.dcdrAdr = new byte[1];
			this.dcdrAdr[0] = (byte)(theAddr & 0x7F);
			break;
		/* Address type 1 - Mobile decoder, long address */
		case 1:
			this.dcdrAdr = new byte[2];
			this.dcdrAdr[0] = (byte)(ADR_TYP.MF_DCDR_LONG.toCode() | ((theAddr >> 8) & 0x3F));
			this.dcdrAdr[1] = (byte)(theAddr & 0xFF);
			break;
		/* Address type 2 - Accessory decoder, 9 and 11 bit address */
		case 2:
			this.dcdrAdr = new byte[2];
			this.dcdrAdr[0] = (byte)(ADR_TYP.ACC_DCDR.toCode() | ((theAddr >> 8) & 0x3F));
			this.dcdrAdr[1] = (byte)(theAddr & 0x1F);
			break;
		/* Unexpected decoder address type */
		default:
			Log.d("DCCencoder", "Unknown decoder type = " + theDcdrTyp);
			break;
		}
		
		/* Save speed step format */
		this.dcdrNumSteps = newDcdr.getInt("SPD_STEPS");
		
		/* Init function key states. */
		this.fkState = new DCCfunctionkeys(theFkeyStates);
	
	}
	
	
	/**
	 * DCCreset:
	 * 
	 * Encode a "soft" reset command for this decoder.
	 * 
	 * @return DCC encoded byte string.
	 */
	protected byte[] DCCreset () {
		
		/* This is a one byte command */
		ByteBuffer dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
		dccCmd.put(REP_CNT);		/* Stream command with repetition count. */
		dccCmd.put(this.dcdrAdr);	/* The decoder's address */
		dccCmd.put((byte)(DCC_INS.DCD_CTL.toCode() | DCD_CTL_INS.RESET.toCode()));
		
		return dccCmd.array();
		
	}
	
	/**
	 * DCCspeed:
	 * 
	 * Encode speed and direction command in the format for this decoder
	 * 
	 * @param speed  integer from -126 to 126
	 * @return DCC encoded byte string.
	 */
	protected byte[] DCCspeed (int speed) {
		
		ByteBuffer dccCmd = null;
		int throttleStep = 0;
		int direction = 0;
		
		/* Set direction and scale speed for this decoders speed step format */
		if (speed < 0) {
			direction = 0;
			throttleStep = (-speed * this.dcdrNumSteps)/126;
		} else {
			direction = 1;
			throttleStep = (speed * this.dcdrNumSteps)/126;
		}

		/* Format the speed step command as required by this decoder. */
		switch (this.dcdrNumSteps) {
		/* 14 step format */
		case 14:
			/* Setup buffer for address + command + 1 byte instruction */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			
			/* If Step is not 0 (STOP), command repeats forever and step is adjusted to skip Stop commands. */
			if (throttleStep > 0) {
				dccCmd.put(REP_FVR);
				throttleStep += 1;
			} else {
				dccCmd.put(REP_CNT);
			}
			
			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);
			
			/* Add direction, head light state and throttle step to create speed command. */
			if (direction == 1) {
				dccCmd.put((byte)(DCC_INS.FWD_SPD.toCode() | (this.fkState.get(0) << 4) | (throttleStep & 0x0F)));
			} else {
				dccCmd.put((byte)(DCC_INS.RVS_SPD.toCode() | (this.fkState.get(0) << 4) | (throttleStep & 0x0F)));				
			}
			break;

		/* 28 step format */
		case 28:
			/* Setup buffer for address + command + 1 byte instruction */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			
			/* If Step is not 0 (STOP), command repeats forever and step is adjusted to skip Stop commands. */
			/* If it is Stop, it is repeated for the standard number of times. */
			if (throttleStep > 0) {
				dccCmd.put(REP_FVR);
				throttleStep += 4;
			} else {
				dccCmd.put(REP_CNT);
			}
			
			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);
			
			/* Add direction and throttle step to create speed command. */
			if (direction == 1) {
				dccCmd.put((byte)(DCC_INS.FWD_SPD.toCode() | ((throttleStep & 0x01) << 4) | ((throttleStep >> 1) & 0x0F)));
			} else {
				dccCmd.put((byte)(DCC_INS.RVS_SPD.toCode() | ((throttleStep & 0x01) << 4) | ((throttleStep >> 1) & 0x0F)));				
			}
			break;
		
		/* 126 step format */
		case 126:
			/* Setup buffer for address + command + 2 byte instruction */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length +3);
			
			/* If Step is not 0 (STOP), command repeats forever and step is adjusted to skip Stop commands. */
			/* If it is Stop, it is repeated for the standard number of times. */
			if (throttleStep > 0) {
				dccCmd.put(REP_FVR);
				throttleStep += 1;
			} else {
				dccCmd.put(REP_CNT);
			}
			
			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);
			
			/* Create first byte of command from ADV_OPS command + Extended Speed step subcommand.  */
			dccCmd.put((byte)(DCC_INS.ADV_OPS.toCode() | ADV_OPS_INS.EXTD_SPD_STEP.toCode()));

			/* Add direction and throttle step in the second byte. */
			dccCmd.put((byte)((direction << 7) | (throttleStep & 0x7F)));
			break;
			
			/* unexpected speed step format */
			default:
				Log.d("DCCencoder", "Unknown speed step type" + this.dcdrNumSteps);
				
			
		}  /* switch(spdFmt) */
		
		return dccCmd.array();
	}
	
	/**
	 * DCCestop:
	 * 
	 * Encode an Emergency Stop command for this decoder.
	 * 
	 * @return DCC encoded byte string.
	 */
	protected byte[] DCCestop () {

		ByteBuffer dccCmd = null;
		final int ESTOP_CMD = 1;
		
		/* Format command for this decoder's speed type */
		switch (this.dcdrNumSteps) {
		/* estop for 14 step format */		
		case 14:
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			dccCmd.put(REP_CNT);
			dccCmd.put(this.dcdrAdr);
			dccCmd.put((byte)(DCC_INS.RVS_SPD.toCode() | ESTOP_CMD));
			break;
		/* estop for 28 step format */	
		case 28:
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			dccCmd.put(REP_CNT);
			dccCmd.put(this.dcdrAdr);
			dccCmd.put((byte)(DCC_INS.RVS_SPD.toCode() | ESTOP_CMD));
			break;
		/* estop for 126 step format */
		case 126:
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 3);
			dccCmd.put(REP_CNT);
			dccCmd.put(this.dcdrAdr);
			dccCmd.put((byte)(DCC_INS.ADV_OPS.toCode() | ADV_OPS_INS.EXTD_SPD_STEP.toCode()));
			dccCmd.put((byte)ESTOP_CMD);
			break;
			
			/* unexpected speed step format */
		default:
			Log.d("DCCencoder", "Unknown speed step type" + this.dcdrNumSteps);				
		}

		return dccCmd.array();
	}
		
	/**
	 * DCCfunc
	 * 
	 * Encode a Function command
	 * 
	 * @param funcKey =  # (from 0 to 28) of function key activated
	 * @return Byte string for function command for this decoder.
	 */
	protected byte[] DCCfunc (int funcKey) {

		ByteBuffer dccCmd = null;

		
		/* Update function key state and get bit vector */
		this.fkState.tog(funcKey);
		
		/* FL and F1 - F4 */
		if (funcKey <= 4) {
			
			/* Create a buffer for a one byte command and set the control byte. */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			dccCmd.put(REP_CNT);

			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);

			/* Create the instruction byte from the instruction opcode, the fl state and F1 - F4 state. */
			dccCmd.put((byte)(DCC_INS.F_GRP_1.toCode() | ((this.fkState.get(0)) << 4) | fkState.get(1, 4) ));
			
		}
		/* F5 - F8 */
		else if (funcKey <= 12) {
			
			/* Create a buffer for a one byte command and set the control byte. */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			dccCmd.put(REP_CNT);

			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);

			/* create the instruction byte from the instruction opcode and the F5 to F8 state. */
			dccCmd.put((byte)(DCC_INS.F_GRP_2.toCode() | this.fkState.get(5, 8) ));
		}
		/* F9 - F12 */
		else if (funcKey <= 12) {
			
			/* Create a buffer for a one byte command and set the control byte. */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 2);
			dccCmd.put(REP_CNT);

			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);

			/* create the instruction byte from the instruction opcode, the group 2 flag and the F9 to F12 state. */
			dccCmd.put((byte)(DCC_INS.F_GRP_2.toCode() | 0x10 | this.fkState.get(9, 12) ));				
		}
		/* F13 - F20 */
		else if (funcKey <= 20){
			
			/* Create a buffer for a two byte command and set the control byte. */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 3);
			dccCmd.put(REP_CNT);
			
			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);

			/* Create instruction byte from Feature Extension opcode and Function Control sub-opcode. */
			dccCmd.put((byte)(DCC_INS.FTR_EXP.toCode() | FTR_EXP_INS.F_GRP_3.toCode()));
			
			/* Create data byte with state of F13 to F20 */
			dccCmd.put((byte)fkState.get(13,20));
		}
		/* F21 - F28 */
		else if (funcKey <= 28) {
			
			/* Create a buffer for a two byte command and set the control byte. */
			dccCmd = ByteBuffer.allocate(this.dcdrAdr.length + 3);
			dccCmd.put(REP_CNT);
			
			/* Put this decoder's address into the command. */
			dccCmd.put(this.dcdrAdr);

			/* Create instruction byte from Feature Extension opcode and Function Control sub-opcode. */
			dccCmd.put((byte)(DCC_INS.FTR_EXP.toCode() | FTR_EXP_INS.F_GRP_4.toCode()));
			
			/* Create data byte with state of F21 to F28 */
			dccCmd.put((byte)fkState.get(21,28));
		}
		/* throw an invalid argument error. */
		else {
			Log.d("DCCencoder", "Invalid FuncKey arg" + funcKey);				
		}
		
		return dccCmd.array();
	}
	
	//TODO Add methods to add / remove decoder from a consist
	//TODO Add methods to write CVs.
}

