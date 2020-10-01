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
	public boolean antMedia = false;
	private long audioDecoderFactory = -1;
	
  @Override
  public long createNativeAudioDecoderFactory() {
     audioDecoderFactory = nativeCreateBuiltinAudioDecoderFactory(antMedia);
     return audioDecoderFactory;
  }
  
  public void setBufferAndCallback(ByteBuffer packetBuffer) {
	  nativeSetBufferAndCallback(this, audioDecoderFactory, packetBuffer);
  }
  
  @CalledByNative void callback(String event) {
	  System.out.println("\n\n**********************************");
	  System.out.println(event);
	  System.out.println("*********************************\n\n");

  }

  private static native long nativeCreateBuiltinAudioDecoderFactory(boolean ant);
  private static native void nativeSetBufferAndCallback(BuiltinAudioDecoderFactoryFactory thisObj, long audioDecoderFactory, ByteBuffer byteBuffer);

}
