package com.spectralmind.sf4android.definitions;

public class ClusterDefinitionWithPos {
	/** The definition */
	public ClusterDefinition def;
	/**  The position in the orginating XML file */
	public int pos;
	
	public ClusterDefinitionWithPos(ClusterDefinition def, int pos) {
		super();
		this.def = def;
		this.pos = pos;
	}
}
