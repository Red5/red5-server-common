package org.red5.server.net.rtmp;

public interface IReceivedMessageTaskQueueListener
{
	void onTaskQueueChanged(ReceivedMessageTaskQueue queue);
}
