package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

/**
 * Z2-SSP-VOLE发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public interface Z2SspVoleSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Z2SspVoleFactory.Z2SspVoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alpha α。
     * @param num   数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2SspVoleSenderOutput send(int alpha, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alpha α。
     * @param num   数量。
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2SspVoleSenderOutput send(int alpha, int num, Z2VoleSenderOutput preSenderOutput) throws MpcAbortException;
}
