package edu.alibaba.mpc4j.common.rpc.utils;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据包。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public final class DataPacket {
    /**
     * 数据包头
     */
    private DataPacketHeader header;
    /**
     * 数据包负载
     */
    private List<byte[]> payload;

    /**
     * 构建数据包。
     *
     * @param header  数据包头。
     * @param payload 数据包负载。
     * @return 数据包。
     */
    public static DataPacket fromByteArrayList(DataPacketHeader header, List<byte[]> payload) {
        DataPacket dataPacket = new DataPacket();
        dataPacket.header = header;
        dataPacket.payload = payload;

        return dataPacket;
    }

    /**
     * 私有构造函数
     */
    private DataPacket() {
        // empty
    }

    /**
     * 返回数据包头。
     *
     * @return 数据包头。
     */
    public DataPacketHeader getHeader() {
        return header;
    }

    /**
     * 返回数据包负载。
     *
     * @return 数据包负载。
     */
    public List<byte[]> getPayload() {
        return payload;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(header)
            .append(payload.stream().map(ByteBuffer::wrap).collect(Collectors.toList()))
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DataPacket)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        DataPacket that = (DataPacket)obj;
        return new EqualsBuilder()
            .append(this.header, that.header)
            .append(
                this.payload.stream().map(ByteBuffer::wrap).collect(Collectors.toList()),
                that.payload.stream().map(ByteBuffer::wrap).collect(Collectors.toList())
            )
            .isEquals();
    }
}
