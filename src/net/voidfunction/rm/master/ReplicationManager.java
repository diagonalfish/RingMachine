/*
 * --------------------------
 * |    Ring Machine 2      |
 * |                        |
 * |         /---\          |
 * |         |   |          |
 * |         \---/          |
 * |                        |
 * | The Crowdsourced CDN   |
 * --------------------------
 * 
 * Copyright (C) 2012 Eric Goodwin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package net.voidfunction.rm.master;

import java.util.*;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

/**
 * Object responsible for managing replicas of files across the network.
 * Maintains information about file downloads and calculates how we should
 * distribute replicas across the worker nodes. Tries to maintain a minimum
 * number of replicas.
 *
 * The ReplicationManager's main task runs on an interval defined in the config
 * file.
 */
public class ReplicationManager implements FileDownloadListener {

	private MasterNode node;
	private int interval, window;
	private final int minReps, maxReps;

	private Timer timer;

	// Keep track of file downloads
	private HashMap<RMFile, Integer> lastPeriodDLs;

	// Moving averages
	private HashMap<RMFile, MovingAverage> movingAvgs;

	public ReplicationManager(MasterNode node) {
		this.node = node;

		// Fetch some information from the config file
		interval = node.getConfig().getInt("rep.interval", 60);
		window = node.getConfig().getInt("rep.window", 900);
		minReps = node.getConfig().getInt("rep.min", 1);
		
		int tempMaxReps = node.getConfig().getInt("rep.max", 3);
		if (tempMaxReps == minReps)
			tempMaxReps++;
		maxReps = tempMaxReps;

		// Create download data storage objects
		lastPeriodDLs = new HashMap<RMFile, Integer>();
		movingAvgs = new HashMap<RMFile, MovingAverage>();

		// Start timer
		timer = new Timer();
		timer.schedule(new ReplicationManagerTask(this), interval * 1000, interval * 1000);
	}

	public synchronized void fileDownloaded(RMFile file) {
		// Register this download in our storage objects
		if (lastPeriodDLs.containsKey(file))
			lastPeriodDLs.put(file, lastPeriodDLs.get(file) + 1);
		else
			lastPeriodDLs.put(file, 1);
	}

	/**
	 * The core of the replication algorithm. Using the current state, provides a list
	 * of replication decisions which can then be acted upon by the manager's main task
	 * loop.
	 * @return a list of ReplicationDecisions mapped to worker Addresses.
	 */
	private synchronized Map<Address, ReplicationDecision> getDecisions() {
		node.getLog().info("Running replication algorithm.");

		Map<Address, ReplicationDecision> decisions = new HashMap<Address, ReplicationDecision>();

		Collection<RMFile> filesColl = node.getFileRepository().getFileObjects();
		RMFile[] files = filesColl.toArray(new RMFile[filesColl.size()]);
		
		// No files
		if (files.length == 0) return decisions;

		//node.getLog().debug("Checking that all files have minimum replicas (" + minReps + ").");
		
		for (RMFile file : files) {
			// Let's at least check the minimum replicas requirement is satisfied
			int reps = node.getWorkerDirectory().countWorkersWithFile(file.getId());
			int needReps = minReps - reps;
			
			//node.getLog().debug("File " + file.getId() + " needs " + needReps + " more replicas.");
			
			if (needReps > 0) {
				assignWorkers(decisions, file, needReps, true);
			}
		}

		/* 
		 * Begin the 'relative popularity' algorithm. We keep a exponential moving average of each
		 * file's downloads over the last few minutes (configurable) and scale this average from
		 * 
		 * [minAvg, maxAvg] -> [minReps -> maxReps]
		 * 
		 * This way, popular files will have more replicas, making use of our finite resources
		 * in the most efficient way possible. This will work best when all files are being
		 * downloaded at least a few times during the period for which download data is kept, so
		 * as best to compare it to other files.
		 * 
		 * If too many replicas are out there, the master node will allow workers to remove
		 * them if they wish, but at the present time this is not implemented - we always
		 * err on the side of more replicas.  Each worker should have enough hard drive
		 * space available to contain the entire set of files if needed.
		 * 
		 * All worker assignments are random.
		 */
		
		HashMap<RMFile, MovingAverage> newMovingAvgs = new HashMap<RMFile, MovingAverage>();
		
		// First stage: determine the minimum and maximum averages.
		double minAvg = -1.0;
		double maxAvg = -1.0;
		for(RMFile file : files) {
			MovingAverage avg = movingAvgs.get(file);
			if (avg == null) {
				avg = new MovingAverage();
				avg.update(0); // Always start the average at zero.
			}
  
			// We only copy over the MovingAvg objects for files that exist, get rid of the rest.
			newMovingAvgs.put(file, avg);
			
			int dls = 0;
			if (lastPeriodDLs.containsKey(file))
				dls = lastPeriodDLs.get(file);
			
			avg.update((double)dls);
  
			if (minAvg < 0) {
				// First file we've checked; set the averages to this file's averages
				minAvg = avg.getAverage();
				maxAvg = avg.getAverage();
			}
			else {
				if (avg.getAverage() < minAvg) minAvg =
					avg.getAverage();
				else if (avg.getAverage() > maxAvg)
					maxAvg = avg.getAverage();
			}
		
		}
		
		// Discard old moving averages hashset
		movingAvgs = newMovingAvgs;
		
		if (minAvg == maxAvg) {
			// Can't scale values, give up
			return decisions;
		}
		
		// Stage two: scale each file's download average onto the range of [minReps - maxReps],
		// round to a whole number of replicas, assign nodes given the amount that we need (or
		// don't need, as the case may be).
		for (RMFile file : files) {
			int reps = node.getWorkerDirectory().countWorkersWithFile(file.getId());

			// Scaling magic occurs here
			double avg = movingAvgs.get(file).getAverage();
			double scaledAvg = ( (((double)maxReps - minReps) * (avg - minAvg)) / (maxAvg - minAvg) ) + minReps;
			int targetReps = (int)Math.floor(scaledAvg + 0.5d);
			int needReps = targetReps - reps;
			
			//node.getLog().debug("File " + file.getId() + ": avg " + avg + ", scaled avg " +
			//	scaledAvg + ", target reps " + targetReps + ", reps needed " + needReps);
			
			if (needReps < 0)
				assignWorkers(decisions, file, -needReps, false);
			else if (needReps > 0)
				assignWorkers(decisions, file, needReps, true);
		}
			
		lastPeriodDLs.clear();
		return decisions; // TODO
	}

	/**
	 * Assign worker nodes in the WorkerDirectory randomly for a given set of tasks,
	 * up to a target amount of assignments. May fail to achieve the target number
	 * of assignments if not enough workers are available.
	 * @param decisions
	 * @param file
	 * @param targetAmt
	 * @param add Whether or not this is a "GET" or "REMOVE" assignment. true = GET
	 */
	private void assignWorkers(Map<Address, ReplicationDecision> decisions, RMFile file, int targetAmt, boolean add) {
		List<Address> workers;
		if (add)
			workers = node.getWorkerDirectory().getWorkersWithoutFile(file.getId());
		else
			workers = node.getWorkerDirectory().getWorkersWithFile(file.getId());

		int assignments = 0;
		int index = 0;

		// Main assignment loop. If we already have a decision for the given worker,
		// we won't assign that worker, unless its current decision is a negative
		// (remove) decision, which is less important.
		while (assignments < targetAmt && index < workers.size()) {
			Address worker = workers.get(index);
			ReplicationDecision existingDec = decisions.get(worker);
			if (existingDec == null || !existingDec.shouldAdd()) {
				decisions.put(worker, new ReplicationDecision(file, add));
				assignments++;
			}
			index++;
		}
	}

	/**
	 * Task which is responsible for running the decision algorithm and sending out
	 * packets to worker nodes to make those decisions happen.
	 */
	private class ReplicationManagerTask extends TimerTask {

		private ReplicationManager mgr;

		public ReplicationManagerTask(ReplicationManager mgr) {
			this.mgr = mgr;
		}

		/**
		 * Run the decision algorithm and send out packets.
		 */
		public void run() {
			Map<Address, ReplicationDecision> decisions = mgr.getDecisions();
			for (Address worker : decisions.keySet()) {
				ReplicationDecision dec = decisions.get(worker);
				
				// Send out packets to the workers
				if (dec.shouldAdd())
					node.getNetManager().packetSendGetFile(worker, dec.getFile());
				else
					node.getNetManager().packetSendMayRemoveFile(worker, dec.getFile().getId());
			}
		}

	}

	/**
	 * Represents a decision made by the replication algorithm - includes
	 * a file, and whether it should be downloaded (or removed).
	 */
	private class ReplicationDecision {
		private RMFile file;
		private boolean shouldAdd;

		public ReplicationDecision(RMFile file, boolean shouldAdd) {
			this.file = file;
			this.shouldAdd = shouldAdd;
		}

		public RMFile getFile() {
			return file;
		}

		public boolean shouldAdd() {
			return shouldAdd;
		}

	}

	/* Moving average class - from JRugged project */
	
	/*
	 * MovingAverage.java
	 * 
	 * Copyright 2009-2010 Comcast Interactive Media, LLC.
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 * 
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 */

	private class MovingAverage {
		private long lastMillis;
		private double average;

		public void update(double sample) {
			long now = System.currentTimeMillis();
			if (lastMillis == 0) { // first sample
				average = sample;
				lastMillis = now;
				return;
			}
			long deltaTime = now - lastMillis;
			long windowMillis = window * 1000L;
			double coeff = Math.exp(-1.0 * ((double)deltaTime / windowMillis));
			average = (1.0 - coeff) * sample + coeff * average;
			lastMillis = now;
		}

		public double getAverage() {
			return average;
		}
	}

}
