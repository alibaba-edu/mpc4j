package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-SSP-VOLE协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public interface Z2SspVoleReceiver extends TwoPartyPto, SecurePto {
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
     * @param delta  关联值Δ。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(boolean delta, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2SspVoleReceiverOutput receive(int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 接收方输出。
     * @param preReceiverOutput 预计算接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2SspVoleReceiverOutput receive(int num, Z2VoleReceiverOutput preReceiverOutput) throws MpcAbortException;
}
