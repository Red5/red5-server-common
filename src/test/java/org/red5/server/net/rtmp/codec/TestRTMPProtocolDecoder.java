package org.red5.server.net.rtmp.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.io.utils.IOUtils;
import org.red5.server.net.protocol.RTMPDecodeState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRTMPProtocolDecoder {

    protected Logger log = LoggerFactory.getLogger(TestRTMPProtocolDecoder.class);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testDecodeBuffer() {
        log.debug("Not yet implemented");
    }

    @Test
    public void testDecodePacket() {
        log.debug("\ntestDecodePacket");
        RTMPConnection conn = new RTMPMinaConnection();
        RTMPDecodeState state = new RTMPDecodeState("junit");
        
        // connect is first, last one crashes decode
        IoBuffer p0 = IoBuffer.wrap(IOUtils.hexStringToByteArray("030000000001321400000000020007636f6e6e656374003ff00000000000000300036170700200086f666c6144656d6f0008666c61736856657202000e4c4e582032302c302c302c323836000673776655726c020029687474703a2f2f6c6f63616c686f73743a353038302f64656d6f732f6f666c615f64656d6f2e7377660005746355726c02001972746d703a2f2f6c6f63616c686f73742f6f666c6144656d6f0004667061640100000c6361706162696c697469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f6465637300406f800000000000000d766964656f46756e6374696f6e003ff000000000000000077061676555726c02002a687474703a2f2f6c6f63616c686f73743a353038302f64656d6f732f6f666c615f64656d6f2e68746d6c000009"));
        IoBuffer p1 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c3703a2f2f6c6f63616c686f73742f6f666c6144656d6f0004667061640100000c6361706162696c697469657300406de00000000000000b617564696f436f646563730040abee0000000000000b766964656f436f6465637300406f800000000000000d766964656f46756e6374696f6e003ff000000000000000077061676555c3726c02002a687474703a2f2f6c6f63616c686f73743a353038302f64656d6f732f6f666c615f64656d6f2e68746d6c000009"));
        IoBuffer p2 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c3726c02002a687474703a2f2f6c6f63616c686f73743a353038302f64656d6f732f6f666c615f64656d6f2e68746d6c000009"));
        IoBuffer p3 = IoBuffer.wrap(IOUtils.hexStringToByteArray("02ff1d00000004050000000000989680"));
        IoBuffer p4 = IoBuffer.wrap(IOUtils.hexStringToByteArray("0300016400002f140000000002002264656d6f536572766963652e6765744c6973744f66417661696c61626c65464c567300400000000000000005"));
        IoBuffer p5 = IoBuffer.wrap(IOUtils.hexStringToByteArray("4300058d0000191402000c63726561746553747265616d00400800000000000005"));
        IoBuffer p6 = IoBuffer.wrap(IOUtils.hexStringToByteArray("4200000000000a0400030000000000001388"));
        IoBuffer p7 = IoBuffer.wrap(IOUtils.hexStringToByteArray("080006f100001d1401000000020004706c61790000000000000000000502000973706565782e666c76c200030000000100001388"));
        IoBuffer p8 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c200030000000100001388"));
        IoBuffer p9 = IoBuffer.wrap(IOUtils.hexStringToByteArray("42000000000006040007af055b23"));
        IoBuffer p10 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c20007af056302"));
        IoBuffer p11 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c20007af056ac2"));
        IoBuffer p12 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c20007af05728a"));
        IoBuffer p13 = IoBuffer.wrap(IOUtils.hexStringToByteArray("c20007af057a67"));
        IoBuffer p14 = IoBuffer.wrap(IOUtils.hexStringToByteArray("480029400000181402000b636c6f736553747265616d00000000000000000005"));
        IoBuffer p15 = IoBuffer.wrap(IOUtils.hexStringToByteArray("4200000000000a04000300000000000013888300296102000c63726561746553747265616d00401000000000000005430000000000221402000c64656c657465"));
        IoBuffer p16 = IoBuffer.wrap(IOUtils.hexStringToByteArray("8300296102000c63726561746553747265616d00401000000000000005430000000000221402000c64656c657465"));
        IoBuffer p17 = IoBuffer.wrap(IOUtils.hexStringToByteArray("430000000000221402000c64656c657465"));
        IoBuffer p18 = IoBuffer.wrap(IOUtils.hexStringToByteArray("888300296102000c63726561746553747265616d00401000000000000005430000000000221402000c64656c657465"));
        
        RTMPProtocolDecoder dec = new RTMPProtocolDecoder();
        Packet pkt = dec.decodePacket(conn, state, p0);
        log.debug("Packet #1: {} state: {}", pkt, state);
        pkt = dec.decodePacket(conn, state, p1);
        log.debug("Packet #2: {} state: {}", pkt, state);
    }

    @Test
    public void testDecodeHeader() {
        log.debug("Not yet implemented");
    }

    @Test
    public void testDecodeMessage() {
        log.debug("Not yet implemented");
    }

    @Test
    public void testDecodeInvoke() {
        log.debug("Not yet implemented");
    }

}
