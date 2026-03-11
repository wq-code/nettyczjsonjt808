package net.map591.utils;

import io.netty.buffer.ByteBuf;
/**
 * JT/T 808 报文包装类
 */
public class JTT808Packet {
    private final ByteBuf data;

    public JTT808Packet(ByteBuf data) {
        this.data = data.retain();
    }

    public ByteBuf getData() { return data; }

    public byte[] toBytes() {
        byte[] bytes = new byte[data.readableBytes()];
        data.readBytes(bytes);
        return bytes;
    }

    @Override
    public String toString() {
        return "JTT808Packet{length=" + data.readableBytes() + "}";
    }

    public void release() {
        if (data.refCnt() > 0) data.release();
    }
}
