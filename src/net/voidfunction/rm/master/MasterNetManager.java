package net.voidfunction.rm.master;

import net.voidfunction.rm.common.JGroupsListener;
import net.voidfunction.rm.common.JGroupsManager;
import net.voidfunction.rm.common.Node;

public class MasterNetManager extends JGroupsListener {
	
	private JGroupsManager jgm;
	
	public MasterNetManager() {
		Node ourNode = Node.getInstance();
		jgm = ourNode.getJGroupsMgr();
		
		
	}
	
}
