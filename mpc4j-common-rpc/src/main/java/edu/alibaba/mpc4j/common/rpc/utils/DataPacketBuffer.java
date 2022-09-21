package edu.alibaba.mpc4j.common.rpc.utils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全的数据包缓存区，采用多线程Producer-Consumer设计模式。参见《Java多线程设计模式》，第五章：Producer-Consumer，我来做，你来用。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public class DataPacketBuffer {
    /**
     * 默认的缓存区大小
     */
    private static final int DEFAULT_BUFFER_SIZE = 1 << 10;
    /**
     * 数据包缓存区
     */
    private final ConcurrentHashMap<DataPacketHeader, List<byte[]>> dataPacketBuffer;

    public DataPacketBuffer() {
        dataPacketBuffer = new ConcurrentHashMap<>(DEFAULT_BUFFER_SIZE);
    }

    /**
     * 向缓存区放置数据包。
     *
     * @param dataPacket 数据包。
     */
    public synchronized void put(DataPacket dataPacket) {
        assert (dataPacket != null);
        dataPacketBuffer.put(dataPacket.getHeader(), dataPacket.getPayload());
        notifyAll();
    }

    /**
     * 从缓存读取数据包。
     *
     * @param header 数据包投。
     * @return 数据包。
     * @throws InterruptedException 如果出现多线程异常。
     */
    public synchronized DataPacket take(DataPacketHeader header) throws InterruptedException {
        assert (header != null);
        // 如果缓存区没有数据包，或者缓存区没有指定要读取的数据包，则等待新数据包的读取
        while (!dataPacketBuffer.containsKey(header)) {
            wait();
        }
        return DataPacket.fromByteArrayList(header, dataPacketBuffer.remove(header));
    }
}
