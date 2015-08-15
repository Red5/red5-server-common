package org.red5.server.net.rtmp;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public final class ReceivedMessageTask implements Callable<Boolean> {

	private final static Logger log = LoggerFactory.getLogger(ReceivedMessageTask.class);
	
	private final RTMPConnection conn;
	
	private final IRTMPHandler handler;

	private final String sessionId;
	
	private Packet message;

	// flag representing handling status
	private final AtomicBoolean done = new AtomicBoolean(false);

	// maximum time allowed to process received message
	private long maxHandlingTime = 500L;
	
	public ReceivedMessageTask(String sessionId, Packet message, IRTMPHandler handler) {
		this(sessionId, message, handler, (RTMPConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId));
	}
	
	public ReceivedMessageTask(String sessionId, Packet message, IRTMPHandler handler, RTMPConnection conn) {
		this.sessionId = sessionId;
		this.message = message;
		this.handler = handler;
		this.conn = conn;
	}	

	public Boolean call() throws Exception {
		// set connection to thread local
		Red5.setConnectionLocal(conn);
		try {
			// don't run the deadlock guard if timeout is <= 0
			if (maxHandlingTime > 0) {
				// run a deadlock guard so hanging tasks will be interrupted
				ThreadPoolTaskScheduler deadlockGuard = conn.getDeadlockGuardScheduler();
				if (deadlockGuard != null) {
					try {
						deadlockGuard.schedule(new DeadlockGuard(Thread.currentThread()),
								new Date(System.currentTimeMillis() + maxHandlingTime));
					} catch (TaskRejectedException e) {
						log.warn("DeadlockGuard task is rejected for " + sessionId, e);
					}
				} else {
					log.error("Deadlock guard is null for {}", sessionId);
				}
			}
			// pass message to the handler
			handler.messageReceived(conn, message);
		} catch (Exception e) {
			log.error("Error processing received message {} on {}", message, sessionId, e);
		} finally {
			//log.info("[{}] run end", sessionId);
			// clear thread local
			Red5.setConnectionLocal(null);
			// set done / completed flag
			done.set(true);
		}
		return done.get();
	}
	
	/**
	 * Sets maximum handling time for an incoming message.
	 * 
	 * @param maxHandlingTimeout maximum handling timeout
	 */
	public void setMaxHandlingTimeout(long maxHandlingTimeout) {
		this.maxHandlingTime = maxHandlingTimeout;
	}

	/**
	 * Prevents deadlocked message handling.
	 */
	private class DeadlockGuard implements Runnable {
		
		// executor task thread
		private final Thread taskThread;
		
		/**
		 * Creates the deadlock guard to prevent a message task from taking too long to process.
		 * @param thread
		 */
		private DeadlockGuard(Thread taskThread) {
			// executor thread ref
			this.taskThread = taskThread;
			if (log.isTraceEnabled()) {
				log.trace("DeadlockGuard is created for {}", sessionId);
			}
		}
		
		/**
		 * Save the reference to the thread, and wait until the maxHandlingTimeout has elapsed.
		 * If it elapsed, kill the other thread.
		 * */
		public void run() {
			if (log.isTraceEnabled()) {
				log.trace("DeadlockGuard is started for {}", sessionId);
			}
			// if the message task is not yet done interrupt
			if (!done.get()) {
				// if the task thread hasn't been interrupted check its live-ness
				// if the task thread is alive, interrupt it
				if (!taskThread.isInterrupted() && taskThread.isAlive()) {
					log.warn("Interrupting unfinished active task on {}", sessionId);
					taskThread.interrupt();
				} else {
					log.debug("Unfinished active task on {} already interrupted", sessionId);					
				}
			}
		}
	}
	
}