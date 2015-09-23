/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2015 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Wraps processing of incoming messages.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class ReceivedMessageTask implements Callable<Packet> {

	private final static Logger log = LoggerFactory.getLogger(ReceivedMessageTask.class);

	private final RTMPConnection conn;

	private final IRTMPHandler handler;

	private final String sessionId;

	private Packet packet;

	private long packetNumber;

	private final AtomicBoolean processing = new AtomicBoolean(false);

	private Thread taskThread;

	private ScheduledFuture<Runnable> deadlockFuture;

	public ReceivedMessageTask(String sessionId, Packet packet, IRTMPHandler handler) {
		this(sessionId, packet, handler, (RTMPConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId));
	}

	public ReceivedMessageTask(String sessionId, Packet packet, IRTMPHandler handler, RTMPConnection conn) {
		this.sessionId = sessionId;
		this.packet = packet;
		this.handler = handler;
		this.conn = conn;
	}

	@SuppressWarnings("unchecked")
    public Packet call() throws Exception {
		//keep a ref for executor thread
		taskThread = Thread.currentThread();
		// set connection to thread local
		Red5.setConnectionLocal(conn);
		try {
			// pass message to the handler
			handler.messageReceived(conn, packet);
			// if we get this far, set done / completed flag
			packet.setProcessed(true);
		} finally {
			// clear thread local
			Red5.setConnectionLocal(null);
		}
		if (log.isDebugEnabled()) {
			log.debug("Processing message for {} is processed: {} packet #{}", sessionId, packet.isProcessed(), packetNumber);
		}
		return packet;
	}

	/**
	 * Creates and runs deadlock guard task
	 *
	 * @param deadlockGuardTask
	 */
	public void runDeadlockFuture(Runnable deadlockGuardTask) {
		if (deadlockFuture == null) {
			ThreadPoolTaskScheduler deadlockGuard = conn.getDeadlockGuardScheduler();
			if (deadlockGuard != null) {
				if (log.isDebugEnabled()) {
					log.debug("Creating deadlock guard from: {} for: {}", Thread.currentThread().getName(), sessionId);
				}
				try {
					deadlockFuture = (ScheduledFuture<Runnable>) deadlockGuard.schedule(deadlockGuardTask, new Date(packet.getExpirationTime()));
				} catch (TaskRejectedException e) {
					log.warn("DeadlockGuard task is rejected for {}", sessionId, e);
				}
			} else {
				log.error("Deadlock guard is null for {}", sessionId);
			}
		} else {
			log.error("Deadlock future is already create for {}", sessionId);
		}
	}

	/**
	 * Cancels deadlock future if it was created
	 */
	public void cancelDeadlockFuture() {
		// kill the future for the deadlock since processing is complete
		if (deadlockFuture != null) {
			deadlockFuture.cancel(true);
		}
	}

	/**
	 * Marks task as processing if it is not prosessing yet.
	 *
	 * @return true if successful, or false otherwise
	 */
	public boolean setProcessing() {
		return processing.compareAndSet(false, true);
	}

	public long getPacketNumber() {
		return packetNumber;
	}

	public void setPacketNumber(long packetNumber) {
		this.packetNumber = packetNumber;
	}

	public Packet getPacket() {
		return packet;
	}

	public Thread getTaskThread() {
		return taskThread;
	}

	@Override
	public String toString() {
		return "[sessionId: " + sessionId + "; packetNumber: " + packetNumber + "; processing: " + processing.get() + "]";
	}
	
}