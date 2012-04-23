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

import java.util.List;

import org.jgroups.Address;

import net.voidfunction.rm.common.FileLocator;

/**
 * Implementation of FileLocator for the master node's FileServlet. Looks in
 * the master node's WorkerDirectory to see if a worker has the file, and if
 * so we redirect to a random worker instead of serving it ourselves.
 */
public class MasterFileLocator implements FileLocator {
	
	private MasterNode node;
	
	public MasterFileLocator(MasterNode node) {
		this.node = node;
	}
	
	public String locateURL(String fileId, String fileName) {
		List<Address> workers = node.getWorkerDirectory().getWorkersWithFile(fileId);
		if (workers.size() == 0)
			return null;
		
		Address worker = workers.get(0); // List is pre-randomized by the worker directory
		return "http://" + node.getWorkerDirectory().getWorkerHostAndPort(worker) +
			"/files/" + fileId + "/" + fileName;
	}
	
}
