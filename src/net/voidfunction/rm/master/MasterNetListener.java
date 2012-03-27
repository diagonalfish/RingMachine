package net.voidfunction.rm.master;

import net.voidfunction.rm.common.JGroupsListener;
import net.voidfunction.rm.common.JGroupsManager;
import net.voidfunction.rm.common.RMLog;

public class MasterNetListener extends JGroupsListener {
	
	private MasterNode node;
	private JGroupsManager jgm;
	
	public MasterNetListener(MasterNode node) {
		this.node = node;
		jgm = node.getJGroupsMgr();
		jgm.addListener(this);
	}
	
	/* JGroups events */
	
	public void onConnect() {
		RMLog.info("Connected to cluster.");
	}
	
}
