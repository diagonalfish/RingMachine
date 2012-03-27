package net.voidfunction.rm.master;

import org.jgroups.Address;

import net.voidfunction.rm.common.RMPacket;

public class MasterPacketHandler {

	private MasterNode node;
	
	public MasterPacketHandler(MasterNode node) {
		this.node = node;
	}
	
	public void handle(Address source, RMPacket packet) {
		
	}
	
}
