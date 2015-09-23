package org.red5.server.net.rtmp;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceivedMessageTaskQueue {
	/**
	 * Tasks queue
	 */
	private final Queue<ReceivedMessageTask> tasks = new ConcurrentLinkedQueue<ReceivedMessageTask>();

	/**
	 * Listener
	 */
	private final IReceivedMessageTaskQueueListener listener;

	/**
	 * Channel id
	 */
	private final int channelId;

	public ReceivedMessageTaskQueue(int channelId, IReceivedMessageTaskQueueListener listener) {
		this.listener = listener;
		this.channelId = channelId;
	}

	/**
	 * FIXME
	 * @param task
	 */
	public void addTask(ReceivedMessageTask task) {
		tasks.add(task);
		if (listener != null) {
			listener.onTaskQueueChanged(this);
		}
	}

	/**
	 * FIXME
	 *
	 * @param task
	 */
	public void removeTask(ReceivedMessageTask task) {
		if (tasks.remove(task) && listener != null) {
			listener.onTaskQueueChanged(this);
		}
	}

	/**
	 * FIXME
	 * @return
	 */
	public ReceivedMessageTask getTaskToProcess() {
		ReceivedMessageTask task = tasks.peek();
		if (task != null && task.process()) {
			return task;
		}

		return null;
	}

	/**
	 * FIXME
	 */
	public void removeAllTasks() {
		tasks.clear();
	}

	public int getChannelId() {
		return channelId;
	}
}
