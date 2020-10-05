/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import java.nio.ByteBuffer;

/**
 * Creates a native {@code webrtc::AudioDecoderFactory} with the builtin audio decoders.
 */
public class BuiltinAudioDecoderFactoryFactory implements AudioDecoderFactoryFactory {

	public interface AudioPacketListener {
		public void onAudioPacketData(ByteBuffer data, long timestamp);
	}

	private AudioPacketListener audioPacketListener;
	public boolean antMedia = false;
	private long audioDecoderFactory = -1;
	ByteBuffer buffer = ByteBuffer.allocateDirect(4096);

	@Override
	public long createNativeAudioDecoderFactory() {

		System.out.println("buffer size:"+buffer.capacity());

		audioDecoderFactory = nativeCreateBuiltinAudioDecoderFactory(this, antMedia, buffer);
		return audioDecoderFactory;
	}


	@CalledByNative void callback(String event, int size, long timestamp) {
		System.out.println("\n\n**********************************");

		System.out.println(event+" size:"+size);
		buffer.rewind();
		for (int i = 0; i < size; i++) {
			System.out.print(buffer.get()+" ");		
		}
		System.out.println("*********************************\n\n");
		byte data[] = new byte[size];
		buffer.rewind();
		buffer.get(data, 0, size);
		audioPacketListener.onAudioPacketData(ByteBuffer.wrap(data), timestamp);
		

	}

	public void setAudioPacketListener(AudioPacketListener audioPacketListener) {
		this.audioPacketListener = audioPacketListener;
	}

	private static native long nativeCreateBuiltinAudioDecoderFactory(BuiltinAudioDecoderFactoryFactory thisObj, boolean ant, ByteBuffer byteBuffer);
}
