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

package org.red5.server.net.rtmp.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.protocol.ProtocolException;

/**
 * RTMP chunk header
 * https://www.adobe.com/content/dam/Adobe/en/devnet/rtmp/pdf/rtmp_specification_1.0.pdf (5.3.1.1 page 12)
 */
public class ChunkHeader implements Constants, Cloneable, Externalizable {
    private static final long serialVersionUID = 1L;

    /**
     * chunk format
     */
    private byte format;
    
    /**
     * chunk size
     */
    private byte size;
    
    /**
     * Channel
     */
    private int channelId;

    /**
     * Getter for format
     *
     * @return chunk format
     */
    public byte getFormat() {
        return format;
    }

    /**
     * Setter for format
     *
     * @param format
     *            format
     */
    public void setFormat(byte format) {
        this.format = format;
    }
    
    /**
     * Getter for channel id
     *
     * @return Channel id
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * Setter for channel id
     *
     * @param channelId
     *            Header channel id
     */
    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    /**
     * Getter for size
     *
     * @return size
     */
    public byte getSize() {
        return size;
    }

    /**
     * Setter for size
     *
     * @param size
     *            Header size
     */
    public void setSize(byte size) {
        this.size = size;
    }

    public static ChunkHeader read(IoBuffer in) {
    	ChunkHeader h = new ChunkHeader();
        final int remaining = in.remaining();
        // at least one byte for valid decode
        if (remaining < 1) {
            throw new ProtocolException("Bad chunk header, at least 1 byte is expected");
        }
        byte headerByte = in.get();
        //going to check highest 2 bits
        h.format = (byte)((0b11000000 & headerByte) >> 6);
        h.size = 1;
        h.channelId = 0x3F & headerByte;
        if ((headerByte & 0x3f) == 0) {
            // two byte header
            if (remaining < 2) {
                throw new ProtocolException("Bad chunk header, at least 2 bytes are expected");
            }
            h.channelId = h.channelId << 8 | (in.get() & 0xff);
            h.size = 2;
        } else if ((headerByte & 0x3f) == 1) {
            // three byte header
            if (remaining < 3) {
                throw new ProtocolException("Bad chunk header, at least 3 bytes are expected");
            }
            h.channelId = h.channelId << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
            h.size = 3;
        } else {
            // single byte header
        }
        if (h.channelId < 0) {
       		throw new ProtocolException("Bad channel id: " + h.channelId);
        }
        return h;
	}
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ChunkHeader)) {
            return false;
        }
        final ChunkHeader header = (ChunkHeader) other;
        return (header.getChannelId() == channelId && header.getFormat() == format);
    }

    /** {@inheritDoc} */
    @Override
    public ChunkHeader clone() {
        final ChunkHeader header = new ChunkHeader();
        header.setChannelId(channelId);
        header.setSize(size);
        header.setFormat(format);
        return header;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        format = in.readByte();
        channelId = in.readInt();
        size = (byte)(channelId > 319 ? 3 : (channelId > 63 ? 2 : 1));
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(format);
        out.writeInt(channelId);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        // if its new and props are un-set, just return that message
        if ((channelId + format) > 0d) {
            return "ChunkHeader [type=" + format + ", channelId=" + channelId + ", size=" + size + "]";
        } else {
            return "empty";
        }
    }

}
