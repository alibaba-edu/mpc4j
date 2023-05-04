package edu.alibaba.mpc4j.common.rpc;

import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;

import java.util.Set;

/**
 * 协议通信接口。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public interface Rpc {
    /**
     * 返回自己的参与方信息。
     *
     * @return 自己的参与方信息。
     */
    Party ownParty();

    /**
     * 返回包括自己的所有参与方集合。
     *
     * @return 参与方集合。
     */
    Set<Party> getPartySet();

    /**
     * 返回参与方。
     *
     * @param partyId 参与方ID。
     * @return 参与方。
     */
    Party getParty(int partyId);

    /**
     * 开启连接。
     */
    void connect();

    /**
     * 发送数据。
     *
     * @param dataPacket 数据包。
     */
    void send(DataPacket dataPacket);

    /**
     * 接收数据。
     *
     * @param header 数据包头。
     * @return 接收到的数据包。
     */
    DataPacket receive(DataPacketHeader header);

    /**
     * Receives any data packet. It blocks and wait until there is at least one received data packet. If there are many
     * received data packet, it returns any valid data packet.
     *
     * @return one received data packet.
     */
    DataPacket receiveAny();

    /**
     * 返回已发送的数据负载字节长度。
     *
     * @return 已发送的数据负载字节长度。
     */
    long getPayloadByteLength();

    /**
     * 返回已发送的总字节长度。注意：总字节长度可能胡小于负载字节长度。如果发送数据有规律，总数据长度可能会更小。
     *
     * @return 已发送的总字节长度。
     */
    long getSendByteLength();

    /**
     * 返回已发送的数据包数量。
     *
     * @return 已发送的数据包数量。
     */
    long getSendDataPacketNum();

    /**
     * 与其他参与方网络同步。
     */
    void synchronize();

    /**
     * 重置统计信息。
     */
    void reset();

    /**
     * 关闭连接。
     */
    void disconnect();
}
