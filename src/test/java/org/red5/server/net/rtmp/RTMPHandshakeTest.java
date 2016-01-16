package org.red5.server.net.rtmp;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RTMPHandshakeTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /** Serverside test */
    @Test
    public void testInboundHandshake() {
        InboundHandshake hs = new InboundHandshake();
        hs.getHandshakeType();
    }

}
