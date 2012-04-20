package net.voidfunction.rm.master;

import java.util.*;

import org.jgroups.Address;

import net.voidfunction.rm.common.*;

public class ReplicationManager implements FileDownloadListener {
	
	private MasterNode node;
	private int interval, window;
	final int minReps, maxReps;
	
	private Timer timer;
	
	// Keep track of file downloads
	private HashMap<RMFile, Integer> lastPeriodDLs;
	
	// Moving averages
	private HashMap<RMFile, MovingAverage> movingAvgs;
	
	public ReplicationManager(MasterNode node) {
		this.node = node;
		
		interval = node.getConfig().getInt("rep.interval", 60);
		window = node.getConfig().getInt("rep.window", 900);
		minReps = node.getConfig().getInt("rep.min", 1);
		maxReps = node.getConfig().getInt("rep.max", 3);
		
		lastPeriodDLs = new HashMap<RMFile, Integer>();
		movingAvgs = new HashMap<RMFile, MovingAverage>();
		
		// Start timer
		timer = new Timer();
		timer.schedule(new ReplicationManagerTask(this), interval * 1000, interval * 1000);
	}
	
	public synchronized void fileDownloaded(RMFile file) {
		if (lastPeriodDLs.containsKey(file))
			lastPeriodDLs.put(file, lastPeriodDLs.get(file) + 1);
		else
			lastPeriodDLs.put(file, 1);
	}
	
	protected synchronized Map<Address, ReplicationDecision> getDecisions() {
		node.getLog().debug("Determining replication decisions.");
		
		Map<Address, ReplicationDecision> decisions = new HashMap<Address, ReplicationDecision>();
		
		ArrayList<RMFile> files = new ArrayList<RMFile>(node.getFileRepository().getFileObjects());
		
		node.getLog().debug("Checking that all files have minimum replicas (" + minReps + ").");
		for(RMFile file : files) {
			// Let's at least check the minimum replicas requirement is satisfied
			int reps = node.getWorkerDirectory().countWorkersWithFile(file.getId());
			int needReps = minReps - reps;
			if (needReps > 0) {
				node.getLog().debug("File " + file.getId() + " needs " + needReps + " more replicas.");
				assignWorkers(decisions, file, needReps, true);
			}
		}
		
		/*
		if (minReps == maxReps) return decisions; // No need for fancy scaling math...
		
		HashMap<RMFile, MovingAverage> newMovingAvgs = new HashMap<RMFile, MovingAverage>();
		
		double minAvg = -1.0;
		double maxAvg = -1.0;
		for(RMFile file : files) {
			MovingAverage avg = movingAvgs.get(file);
			if (avg == null)
				avg = new MovingAverage();
			
			newMovingAvgs.put(file, avg);
			
			if (lastPeriodDLs.containsKey(file))
				avg.update(lastPeriodDLs.get(file).doubleValue());
			else
				avg.update(0.0);
			
			if (minAvg < 0) {
				minAvg = avg.getAverage();
				maxAvg = avg.getAverage();
			}
			else {
				if (avg.getAverage() < minAvg)
					minAvg = avg.getAverage();
				else if (avg.getAverage() > maxAvg)
					maxAvg = avg.getAverage();
			}
		}
		
		movingAvgs = newMovingAvgs;
		
		if (minReps == maxReps) maxReps++; // Prevent divide by zero
		
		for (RMFile file : files) {
			int reps = node.getWorkerDirectory().countWorkersWithFile(file.getId());
			
			double avg = movingAvgs.get(file).getAverage();
			double scaledAvg = (((avg - minReps) / (maxReps - minReps)) * 2.0) - 1.0;
			int targetReps = (int)Math.floor(scaledAvg + 0.5d);
			
			int needReps = targetReps - reps;
			if (needReps < 0)
				assignWorkers(decisions, file, -needReps, false);
			else if (needReps > 0)
				assignWorkers(decisions, file, needReps, true);
		}
		
		lastPeriodDLs.clear();
		*/
		
		return decisions; //TODO
	}
	
	private void assignWorkers(Map<Address, ReplicationDecision> decisions,
			RMFile file, int targetAmt, boolean add) {
		node.getLog().debug("Assigning workers (up to maximum of " + targetAmt + ")");
		List<Address> workers;
		if (add) workers = node.getWorkerDirectory().getWorkersWithoutFile(file.getId());
		else workers = node.getWorkerDirectory().getWorkersWithFile(file.getId());
		
		node.getLog().debug("Available workers: " + workers.size());
		
		int assignments = 0;
		int index = 0;
		
		while (assignments < targetAmt && index < workers.size()) {
			Address worker = workers.get(index);
			if (!decisions.containsKey(worker)) {
				node.getLog().debug("Assigning " + worker);
				decisions.put(worker, new ReplicationDecision(file, add));
				assignments++;
			}
			index++;
		}
		
		node.getLog().debug("Done assigning workers.");
	}
	
	private class ReplicationManagerTask extends TimerTask {

		private ReplicationManager mgr;
		
		public ReplicationManagerTask(ReplicationManager mgr) {
			this.mgr = mgr;
		}
		
		public void run() {
				Map<Address, ReplicationDecision> decisions = mgr.getDecisions();
				for(Address worker : decisions.keySet()) {
					ReplicationDecision dec = decisions.get(worker);
					node.getLog().debug("Decision: " + worker + " -> " + dec.getFile().getId());
				}
		}
		
	}
	
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
	
	
	/* MovingAverage.java
	 * 
	 * Copyright 2009-2010 Comcast Interactive Media, LLC.
	 * 
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *     http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
	
	private class MovingAverage {
	    private long lastMillis;
	    private double average;
		
		public void update(double sample) {
			long now = System.currentTimeMillis();
			if (lastMillis == 0) {  // first sample
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
