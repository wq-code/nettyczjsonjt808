package net.map591.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 单端口混合协议解码器
 * 文本json
 * jtt808
 */
public class HybridDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有可读字节
        if (in.readableBytes() < 1) {
            return;
        }

        // 查看第一个字节，不移动 readerIndex
        byte firstByte = in.getByte(in.readerIndex());
        if (firstByte == (byte) 0x7E) {
            // 处理 JT/T 808 二进制协议
            decodeJTT808(in, out);
        } else {
            // 处理文本协议（JSON、XML、自定义字符串）
            decodeString(in, out);
        }
    }

    /**
     * 解码 JT/T 808 报文（以 0x7E 开始和结束）
     */
    private void decodeJTT808(ByteBuf in, List<Object> out) {
        int endIndex = in.indexOf(in.readerIndex() + 1, in.writerIndex(), (byte) 0x7E);
        if (endIndex < 0) return; // 帧未完整

        int frameLength = endIndex - in.readerIndex() + 1;
        if (frameLength < 12) {
            in.skipBytes(frameLength);
            return;
        }

        ByteBuf frame = in.readRetainedSlice(frameLength);
        out.add(new JTT808Packet(frame));
    }

    /**
     * 解码文本数据（JSON、字符串等）
     */
    private void decodeString(ByteBuf in, List<Object> out) {
        int readable = in.readableBytes();
        if (readable == 0) return;

        // 必须是 JSON 格式
        if (in.getByte(in.readerIndex()) != '{') {
            if (readable > 100) {
                String text = in.toString(in.readerIndex(), readable, StandardCharsets.UTF_8);
                in.skipBytes(readable);
                out.add(text);
            }
            return;
        }

        // 查找匹配的 }
        int endIndex = findClosingBrace(in, in.readerIndex(), in.writerIndex());
        if (endIndex >= 0) {
            int length = endIndex - in.readerIndex() + 1;
            if (length <= readable) {
                ByteBuf jsonBuf = in.readRetainedSlice(length);
                String jsonString = jsonBuf.toString(StandardCharsets.UTF_8);
                jsonBuf.release();
                out.add(jsonString);
            }
        }
    }
    /**
     * 查找匹配的 }，支持嵌套，忽略字符串内的 {
     */
    private int findClosingBrace(ByteBuf buf, int start, int end) {
        int stack = 0;
        boolean inString = false;
        char quote = 0;

        for (int i = start; i < end; i++) {
            byte b = buf.getByte(i);
            char c = (char) (b & 0xFF);

            if (inString) {
                if (c == '\\' && i + 1 < end) {
                    i++; // 跳过转义字符
                } else if (c == quote) {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    quote = c;
                } else if (c == '{') {
                    stack++;
                } else if (c == '}') {
                    stack--;
                    if (stack == 0) return i;
                }
            }
        }
        return -1; // 未找到完整 JSON
    }
}
