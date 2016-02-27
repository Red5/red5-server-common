/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
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

package org.red5.server.messaging;

import java.io.IOException;
import java.util.Map;

import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple in-memory version of push-push pipe. It is triggered by an active provider to push messages through it to an event-driven consumer.
 * 
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class InMemoryPushPushPipe extends AbstractPipe {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPushPushPipe.class);

    public InMemoryPushPushPipe() {
        super();
    }

    public InMemoryPushPushPipe(IPipeConnectionListener listener) {
        this();
        addPipeConnectionListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public boolean subscribe(IConsumer consumer, Map<String, Object> paramMap) {
        if (consumer instanceof IPushableConsumer) {
            boolean success = super.subscribe(consumer, paramMap);
            if (log.isDebugEnabled()) {
                log.debug("Consumer subscribe{} {} params: {}", new Object[] { (success ? "d" : " failed"), consumer, paramMap });
            }
            if (success) {
                fireConsumerConnectionEvent(consumer, PipeConnectionEvent.CONSUMER_CONNECT_PUSH, paramMap);
            }
            return success;
        } else {
            throw new IllegalArgumentException("Non-pushable consumer not supported by PushPushPipe");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
        boolean success = super.subscribe(provider, paramMap);
        if (log.isDebugEnabled()) {
            log.debug("Provider subscribe{} {} params: {}", new Object[] { (success ? "d" : " failed"), provider, paramMap });
        }
        if (success) {
            fireProviderConnectionEvent(provider, PipeConnectionEvent.PROVIDER_CONNECT_PUSH, paramMap);
        }
        return success;
    }

    /** {@inheritDoc} */
    public IMessage pullMessage() {
        return null;
    }

    /** {@inheritDoc} */
    public IMessage pullMessage(long wait) {
        return null;
    }

    /**
     * Pushes a message out to all the PushableConsumers.
     * 
     * @param message
     *            the message to be pushed to consumers
     * @throws IOException
     *            In case IOException of some sort is occured
     */
    public void pushMessage(IMessage message) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("pushMessage: {}", message);
            log.debug("pushMessage - consumers: {}", consumers.size());
        }
        for (IConsumer consumer : consumers) {
            try {
                IPushableConsumer pcon = (IPushableConsumer) consumer;
                if (message instanceof RTMPMessage) {
                    RTMPMessage rtmpMessage = (RTMPMessage) message;
                    IRTMPEvent body = rtmpMessage.getBody();
                    int time = body.getTimestamp();
                    pcon.pushMessage(this, message);
                    body.setTimestamp(time);
                } else {
                    pcon.pushMessage(this, message);
                }
            } catch (Throwable t) {
                if (t instanceof IOException) {
                    throw (IOException) t;
                }
                log.error("Exception pushing message to consumer", t);
            }

        }
    }

}
