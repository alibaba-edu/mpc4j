package edu.alibaba.mpc4j.s2pc.pso.pid;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.pid.PidFactory.PidType;

import java.util.Set;

/**
 * PID协议接口。
 *
 * @author Weiran Liu
 * @date 2022/02/08
 */
public interface PidParty<T> extends TwoPartyPto, SecurePto {

    @Override
    PidType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxOwnSetSize   最大自己元素数量。
     * @param maxOtherSetSize 最大对方元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxOwnSetSize, int maxOtherSetSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param ownElementSet 自己元素集合。
     * @param otherSetSize  对方元素数量。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    PidPartyOutput<T> pid(Set<T> ownElementSet, int otherSetSize) throws MpcAbortException;
}
