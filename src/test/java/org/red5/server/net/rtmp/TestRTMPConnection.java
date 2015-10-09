package org.red5.server.net.rtmp;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.IConnection;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;

public class TestRTMPConnection {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

//	@Test
//	public void testGetNextAvailableChannelId() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testIsChannelUsed() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetChannel() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testCloseChannel() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testReserveStreamId() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testReserveStreamIdNumber() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testIsValidStreamId() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testNewPlaylistSubscriberStream() {
//		System.out.println("\n testNewPlaylistSubscriberStream");
//		RTMPConnection conn = new RTMPMinaConnection();
//		
//		Number streamId = 0;
//		
//		IPlaylistSubscriberStream stream = conn.newPlaylistSubscriberStream(streamId);
//		System.out.printf("PlaylistSubscriberStream for stream id 0: %s\n", stream);
//		
//	}

//	@Test
//	public void testRemoveClientStream() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetUsedStreamCount() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetStreamById() {
//		System.out.println("\n testGetStreamById");
//		RTMPConnection conn = new RTMPMinaConnection();
//		
//		IClientStream stream = conn.getStreamById(0);
//		System.out.printf("Stream for stream id 0: %s\n", stream);
//		assertNull(stream);
//		stream = conn.getStreamById(1);
//		System.out.printf("Stream for stream id 1: %s\n", stream);
//		
//	}

	@Test
	public void testGetStreamIdForChannelId() {
		System.out.println("\n testGetStreamIdForChannelId");
		RTMPConnection conn = new RTMPMinaConnection();
		System.out.printf("Starting stream id: %f\n", conn.getStreamId().doubleValue());
		assertEquals(0.0d, conn.getStreamId().doubleValue(), 0d);
		
		assertEquals(0.0d, conn.getStreamIdForChannelId(4).doubleValue(), 0d);
		assertEquals(0.0d, conn.getStreamIdForChannelId(5).doubleValue(), 0d);
		assertEquals(0.0d, conn.getStreamIdForChannelId(6).doubleValue(), 0d);
		assertEquals(1.0d, conn.getStreamIdForChannelId(7).doubleValue(), 0d);
		assertEquals(1.0d, conn.getStreamIdForChannelId(8).doubleValue(), 0d);
		assertEquals(1.0d, conn.getStreamIdForChannelId(9).doubleValue(), 0d);
		
		System.out.printf("Stream id - cid 0: %f cid 12: %f\n", conn.getStreamIdForChannelId(0).doubleValue(), conn.getStreamIdForChannelId(12).doubleValue());
	}

//	@Test
//	public void testGetStreamByChannelId() {
//		System.out.println("\n testGetStreamByChannelId");
//		RTMPConnection conn = new RTMPMinaConnection();
//		// any channel less than 4 should be null
//		assertNull(conn.getStreamByChannelId(3));		
//		// stream id 0
//		assertNotNull(conn.getStreamByChannelId(4));
//		assertNotNull(conn.getStreamByChannelId(5));
//		// stream id 1
//		assertNotNull(conn.getStreamByChannelId(7));
//	}

	@Test
	public void testGetChannelIdForStreamId() {
		System.out.println("\n testGetChannelIdForStreamId");
		RTMPConnection conn = new RTMPMinaConnection();
		assertEquals(conn.getStreamId().intValue(), 0);

		// channel returned is 1 less than what we actually need
//		assertEquals(3, conn.getChannelIdForStreamId(0));
//		assertEquals(6, conn.getChannelIdForStreamId(1));
//		assertEquals(9, conn.getChannelIdForStreamId(2));
//		assertEquals(12, conn.getChannelIdForStreamId(3));
		assertEquals(4, conn.getChannelIdForStreamId(0));
		assertEquals(7, conn.getChannelIdForStreamId(1));
		assertEquals(10, conn.getChannelIdForStreamId(2));
		assertEquals(13, conn.getChannelIdForStreamId(3));
		
		System.out.printf("Channel id - sid 20: %d sid 33: %d\n", conn.getChannelIdForStreamId(20), conn.getChannelIdForStreamId(33));
	}

//	@Test
//	public void testUnreserveStreamId() {
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testDeleteStreamById() {
//		fail("Not yet implemented");
//	}

}
