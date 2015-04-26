package com.olinsdepot.mbus_srvc;

/**
 * DCCfunction keys class
 * 
 * Maintains a mapping of the state of the 29 function keys for a decoder to an int.
 * 
 * @author mhughes
 *
 */
public class DCCfunctionkeys {
	
	private int fkeyState;
	
	/**
	 * Constructor
	 * 
	 * arg = none  Constructor returns with all functions off
	 * arg = int   Constructor returns with functions initialized to values in int.
	 */
	
	public DCCfunctionkeys() {
		this.fkeyState = 0;
	}
	
	public DCCfunctionkeys(int theState) {
		this.fkeyState = theState;
	}
	
	/*
	 * Methods
	 */
	
	/**
	 * Initialize all function keys to 0 = OFF
	 */
	public void clear() {
		this.fkeyState = 0;
	}
	
	/**
	 * Set specified function key state to 0 = OFF
	 * 
	 * @param fkey
	 */
	public void clear(int fkey) {
		this.fkeyState &= ~(1 << fkey);
	}
	
	/**
	 * Set function key state
	 * 
	 * @param fkey Set key state to 1 = ON
	 */
	public void set(int fkey) {
		this.fkeyState |= 1 << fkey;
	}
	
	/**
	 * Set specified function key state to specified value
	 * @param fkey
	 * @param fval
	 */
	public void set(int fkey, boolean fval) {
		if (fval) {
			this.set(fkey);
		} else {
			this.clear(fkey);
		}
	}
	
	/**
	 * Return the state of all function keys
	 * @return Integer where bit 0 = FL and bit 28 = F28.
	 */
	public int get() {
		return this.fkeyState;
	}
	
	/**
	 * Return the state of the specified function key.
	 * @param fkey  Integer that specifies function key
	 * @return Integer where bit 0 has requested state.
	 */
	public int get(int fkey) {
		return (this.fkeyState >> fkey) & 0x01;
	}
	
	/**
	 * Return the state of the specified range of keys
	 * @param skey Integer specifying the starting key
	 * @param nkey Integer specifying the ending key
	 * @return Integer where bit 0 = skey state and bit n = end key state
	 */
	public int get(int skey, int nkey) {
		int keyMask = 0;
		
		/* Make a mask for this number of bits */
		for (int i = skey; i <= nkey; i++) {
			keyMask = keyMask << 1;
			keyMask |= 1;
		}
		return (this.fkeyState >> skey) & keyMask;
	}

	/**
	 * Set specified function key state to specified value
	 * @param fkey
	 * @param fval
	 */
	public int tog(int fkey) {
		int fval = this.get(fkey);
		if (fval == 0) {
			this.set(fkey);
		} else {
			this.clear(fkey);
		}
		
		return fval;
	}

}
