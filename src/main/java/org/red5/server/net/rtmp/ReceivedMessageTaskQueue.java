package org.red5.server.net.rtmp;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceivedMessageTaskQueue {
	/**
	 * Tasks queue
	 */
	private final Queue<ReceivedMessageTask> tasks = new ConcurrentLinkedQueue<ReceivedMessageTask>();

	/**
	 * FIXME
	 */
	private final AtomicBoolean processing = new AtomicBoolean(false);

	/**
	 * FIXME Changes processing flag atomically
	 *
	 * @param processing
	 * @return
	 */
	public boolean changeProcessing(boolean processing) {
		return this.processing.compareAndSet(!processing, processing);
	}

	/**
	 * FIXME
	 * @param task
	 */
	public void addTask(ReceivedMessageTask task) {
		tasks.add(task);
	}


	/**
	 * FIXME
	 * @return
	 */
	public ReceivedMessageTask pollTask() {
		return tasks.poll();
	}

	/**
	 * FIXME
	 */
	public void removeAllTasks() {
		tasks.clear();
	}
}
