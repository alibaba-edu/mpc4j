package edu.alibaba.mpc4j.s2pc.pjc.pid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PID协议接口。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public interface PidParty<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxOwnElementSetSize   最大自己元素数量。
     * @param maxOtherElementSetSize 最大对方元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxOwnElementSetSize, int maxOtherElementSetSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param ownElementSet       自己元素集合。
     * @param otherElementSetSize 对方元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PidPartyOutput<T> pid(Set<T> ownElementSet, int otherElementSetSize) throws MpcAbortException;
}
