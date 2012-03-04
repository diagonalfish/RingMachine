package net.voidfunction.rm.common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
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
	
	public void setDataVal(String key, Serializable value) {
		data.put(key.toLowerCase(), value);
	}
	
	public Integer getInteger(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null) return null;
			return (Integer)d;
		} catch (ClassCastException e) {
			return null;
		}
	}
	
	public Double getDouble(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null) return null;
			return (Double)d;
		} catch (ClassCastException e) {
			return null;
		}
	}

	public Double getString(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null) return null;
			return (Double)d;
		} catch (ClassCastException e) {
			return null;
		}
	}
	
	public Boolean getBoolean(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null) return null;
			return (Boolean)d;
		} catch (ClassCastException e) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Object> getList(String key) {
		try {
			Object d = data.get(key.toLowerCase());
			if (d == null) return null;
			return (List<Object>)d;
		} catch (ClassCastException e) {
			System.out.println("EXCEPTION'd");
			return null;
		}
	}
	
}
