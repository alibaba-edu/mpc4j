package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;

/**
 * OPRP发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface OprpSender extends TwoPartyPto {
    /**
     * 返回PRP类型。
     *
     * @return PRP类型。
     */
    PrpType getPrpType();

    /**
     * 返回协议是否为逆映射。
     *
     * @return 协议是否为逆映射。
     */
    boolean isInvPrp();

    /**
     * 初始化协议。
     *
     * @param maxBatchSize 最大批处理数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param key       密钥。
     * @param batchSize 批处理数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    OprpSenderOutput oprp(byte[] key, int batchSize) throws MpcAbortException;
}
