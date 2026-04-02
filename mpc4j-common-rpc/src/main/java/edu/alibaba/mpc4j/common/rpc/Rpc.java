package edu.alibaba.mpc4j.common.rpc;

import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;

import java.util.Set;

/**
 * 协议通信接口。
 * <p>
 * RPC生命周期：
 * <ol>
 *   <li>构造Rpc实例</li>
 *   <li>connect(): 主动上线，初始化通信资源，与其他参与方建立连接</li>
 *   <li>send()/receive(): 执行协议通信</li>
 *   <li>disconnect(): 主动下线，通知其他参与方，释放通信资源</li>
 * </ol>
 * </p>
 * <p>
 * 注意：connect()和disconnect()可以循环调用，实现多次上线/下线。
 * </p>
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
     * 主动上线。
     * <p>
     * mpc4j 的通信模型要求：
     * <ul>
     *   <li>在协议执行之前预先确定参与方数量，运行期间不可变更。</li>
     *   <li>每个参与方显式调用 connect() 上线，参与方可以按任意顺序独立调用 connect()，无需等待其他参与方先启动。</li>
     *   <li>只有当所有参与方都完成 connect() 后，才可以开始通信。</li>
     *   <li>若已处于已连接状态，重复调用 connect() 将被忽略（仅打印警告日志）。</li>
     *   <li>调用 disconnect() 后可再次调用 connect() 实现重连，但所有参与方需都 disconnect() 之后，才能启动新一轮 connect()。</li>
     * </ul>
     * </p>
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
     * @param ptoId protocol ID.
     * @return one received data packet.
     */
    DataPacket receiveAny(int ptoId);

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
     * 主动下线。
     * <p>
     * mpc4j 的通信模型要求：
     * <ul>
     *   <li>每个参与方可以按任意顺序独立调用 disconnect()，底层握手协议会等待配对方响应。</li>
     *   <li>只有当所有参与方都完成 disconnect() 后，才可以发起新一轮 connect() 重连。
     *       若部分参与方已 disconnect() 但在其他参与方尚未 disconnect() 时就执行 connect()，重连将导致握手超时。</li>
     *   <li>若已处于已断开状态，重复调用 disconnect() 将被忽略（仅打印警告日志）。</li>
     * </ul>
     * </p>
     */
    void disconnect();
}
