package edu.alibaba.mpc4j.common.rpc.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * thread-safe data packet buffer. The design follows the Producer-Consumer pattern. See:
 * <p>
 * 《Java多线程设计模式》，第五章：Producer-Consumer，我来做，你来用。
 * </p>
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public class DataPacketBuffer {
    /**
     * default buffer size
     */
    private static final int DEFAULT_BUFFER_SIZE = 1 << 10;
    /**
     * buffer
     */
    private final Map<DataPacketHeader, List<byte[]>> dataPacketBuffer;

    public DataPacketBuffer() {
        dataPacketBuffer = new ConcurrentHashMap<>(DEFAULT_BUFFER_SIZE);
    }

    /**
     * Puts a data packet into the buffer.
     *
     * @param dataPacket the data packet.
     */
    public synchronized void put(DataPacket dataPacket) {
        assert (dataPacket != null);
        dataPacketBuffer.put(dataPacket.getHeader(), dataPacket.getPayload());
        notifyAll();
    }

    /**
     * Takes a data packet that matches the header.
     *
     * @param header the header.
     * @return a data packet.
     * @throws InterruptedException interrupted exception.
     */
    public synchronized DataPacket take(DataPacketHeader header) throws InterruptedException {
        assert (header != null);
        // if there is no target data packet in the buffer, waiting until new data packet is added.
        while (!dataPacketBuffer.containsKey(header)) {
            wait();
        }
        return DataPacket.fromByteArrayList(header, dataPacketBuffer.remove(header));
    }

    /**
     * Takes a data packet that matches the receiver ID.
     *
     * @param receiverId the receiver ID.
     * @return a data packet.
     * @throws InterruptedException interrupted exception.
     */
    public synchronized DataPacket take(int receiverId) throws InterruptedException {
        DataPacketHeader targetHeader = null;
        while (targetHeader == null) {
            // we first try to find a candidate header
            for (DataPacketHeader dataPacketHeader : dataPacketBuffer.keySet()) {
                if (dataPacketHeader.getReceiverId() == receiverId) {
                    targetHeader = dataPacketHeader;
                }
            }
            if (targetHeader == null) {
                // if we cannot find any candidate, wait for new data packets.
                wait();
            }
        }
        return DataPacket.fromByteArrayList(targetHeader, dataPacketBuffer.remove(targetHeader));
    }

    /**
     * Takes a data packet that matches the receiver ID and the protocol ID.
     *
     * @param receiverId the receiver ID.
     * @param ptoId      the protocol ID.
     * @return a data packet.
     * @throws InterruptedException interrupted exception.
     */
    public synchronized DataPacket take(int receiverId, int ptoId) throws InterruptedException {
        DataPacketHeader targetHeader = null;
        while (targetHeader == null) {
            // we first try to find a candidate header
            for (DataPacketHeader dataPacketHeader : dataPacketBuffer.keySet()) {
                if (dataPacketHeader.getReceiverId() == receiverId && dataPacketHeader.getPtoId() == ptoId) {
                    targetHeader = dataPacketHeader;
                }
            }
            if (targetHeader == null) {
                // if we cannot find any candidate, wait for new data packets.
                wait();
            }
        }
        return DataPacket.fromByteArrayList(targetHeader, dataPacketBuffer.remove(targetHeader));
    }
}
