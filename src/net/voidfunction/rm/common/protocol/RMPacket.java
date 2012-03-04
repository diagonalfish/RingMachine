package net.voidfunction.rm.common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A flexible, serializable packet type which consists of a type and a String->Serializable Map.
 */
public class RMPacket implements Serializable {

	public static enum Type {
		INFO
	}
	
	private RMPacket.Type type;
	private Map<String, Serializable> data;
	
	private static final long serialVersionUID = 1L;
	
	public RMPacket(RMPacket.Type type) {
		this.type = type;
		data = new HashMap<String, Serializable>();
	}
	
	public RMPacket.Type getType() {
		return type;
	}
	
	public boolean hasDataVal(String key) {
		return data.containsKey(key);
	}
	
	public Serializable getDataVal(String key) {
		return data.get(key);
	}
	
	public void setDataVal(String key, Serializable value) {
		data.put(key, value);
	}

}
