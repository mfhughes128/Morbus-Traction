/**
 * 
 */
package com.olinsdepot.od_traction;

/**
 * An object modeling this loco unit
 * 
 * @author mhughes
 *
 */
public class LocoUnit {
	
	//TODO add handling for function keys.
	//TODO make parcelable so can be passed.
	
	/* Loco unit characteristics */
	private final String roadName;
	private final int addrType;
	private final int dccAddr;
	private final int spdSteps;
	
	/* Constructors */
	public LocoUnit(String name) {
		this(name, 0, 3, 126);
	}
	public LocoUnit(String name, int adr) {
		this(name, 1, adr, 126);
	}
	public LocoUnit(String name, int typ, int adr) {
		this(name, typ, adr, 126);
	}
	public LocoUnit(String name, int typ, int adr, int stps) {
		roadName = name;
		addrType = typ;
		dccAddr = adr;
		spdSteps = stps;
	}
	
	/* Getter functions */
	public String getRoadName() {
		return roadName;
	}
	
	public int getTypeAddr() {
		return addrType;
	}
	
	public int getDccAddr() {
		return dccAddr;
	}
	
	public int getSpdSteps() {
		return spdSteps;
	}
}
