package net.voidfunction.rm.worker;

import net.voidfunction.rm.common.Node;
import net.voidfunction.rm.common.RMLog;

/**
 * Main class for RingMachine worker node.
 */
public class WorkerNode extends Node {

	public static void main(String[] args) {
		WorkerNode worker = new WorkerNode();
		worker.start();
	}

	public WorkerNode() {
		super("worker.properties");
	}

	public void start() {
		RMLog.raw("RingMachine Worker Node v0.1 starting up...");
	}

}
