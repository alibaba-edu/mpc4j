package edu.alibaba.mpc4j.common.rpc.utils;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * data packet.
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public final class DataPacket {
    /**
     * header
     */
    private DataPacketHeader header;
    /**
     * payload type
     */
    private PayloadType payloadType;
    /**
     * payload
     */
    private List<byte[]> payload;
    /**
     * equal length
     */
    private int equalLength;

    /**
     * Creates a data packet.
     *
     * @param header  header.
     * @param payload payload.
     * @return a data packet.
     */
    public static DataPacket fromByteArrayList(DataPacketHeader header, List<byte[]> payload) {
        DataPacket dataPacket = new DataPacket();
        dataPacket.header = header;
        dataPacket.payload = payload;
        dataPacket.equalLength = -1;

        // empty payload
        if (payload.size() == 0) {
            dataPacket.payloadType = PayloadType.EMPTY;
            return dataPacket;
        }

        // singleton payload
        if (payload.size() == 1) {
            dataPacket.payloadType = PayloadType.SINGLETON;
            return dataPacket;
        }

        // try equal-size payload
        boolean equalSize = true;
        int length = payload.get(0).length;
        for (byte[] data : payload) {
            if (data.length != length) {
                equalSize = false;
                break;
            }
        }
        if (equalSize) {
            dataPacket.payloadType = PayloadType.EQUAL_SIZE;
            dataPacket.equalLength = length;
            return dataPacket;
        }

        // normal payload
        dataPacket.payloadType = PayloadType.NORMAL;
        return dataPacket;
    }

    /**
     * Creates a data packet without checking correctness.
     *
     * @param header      header.
     * @param payloadType type.
     * @param payload     payload.
     * @return a data packet.
     */
    public static DataPacket fromUncheck(DataPacketHeader header, PayloadType payloadType, List<byte[]> payload) {
        DataPacket dataPacket = new DataPacket();
        dataPacket.header = header;
        dataPacket.payloadType = payloadType;
        dataPacket.payload = payload;

        return dataPacket;
    }

    /**
     * private constructor.
     */
    private DataPacket() {
        // empty
    }

    /**
     * Gets header.
     *
     * @return header.
     */
    public DataPacketHeader getHeader() {
        return header;
    }

    /**
     * Gets payload type.
     *
     * @return payload type.
     */
    public PayloadType getPayloadType() {
        return payloadType;
    }

    /**
     * Gets payload.
     *
     * @return payload.
     */
    public List<byte[]> getPayload() {
        return payload;
    }

    /**
     * Gets equal length. If the payload is not equal-length, return -1.
     *
     * @return equal length.
     */
    public int getEqualLength() {
        return equalLength;
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
        DataPacket that = (DataPacket) obj;
        return new EqualsBuilder()
            .append(this.header, that.header)
            .append(
                this.payload.stream().map(ByteBuffer::wrap).collect(Collectors.toList()),
                that.payload.stream().map(ByteBuffer::wrap).collect(Collectors.toList())
            )
            .isEquals();
    }
}
