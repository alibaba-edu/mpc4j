package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * OPRP协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public abstract class AbstractOprpSender extends AbstractTwoPartyPto implements OprpSender {
    /**
     * 最大批处理数量
     */
    protected int maxBatchSize;
    /**
     * 取整最大批处理数量
     */
    protected int maxRoundBatchSize;
    /**
     * 批处理数量
     */
    protected int batchSize;
    /**
     * 批处理字节数量
     */
    protected int batchByteSize;
    /**
     * 取整批处理数量
     */
    protected int roundBatchSize;
    /**
     * 密钥
     */
    protected byte[] key;

    protected AbstractOprpSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, OprpConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int maxBatchSize) {
        MathPreconditions.checkPositive("maxBatchSize", maxBatchSize);
        this.maxBatchSize = maxBatchSize;
        maxRoundBatchSize = CommonUtils.getByteLength(this.maxBatchSize) * Byte.SIZE;
        initState();
    }

    protected void setPtoInput(byte[] key, int batchSize) throws MpcAbortException {
        checkInitialized();
        MathPreconditions.checkEqual("key.length", "λ(B)", key.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.key = key;
        MathPreconditions.checkPositiveInRangeClosed("batchSize", batchSize, maxBatchSize);
        this.batchSize = batchSize;
        batchByteSize = CommonUtils.getByteLength(batchSize);
        roundBatchSize = batchByteSize * Byte.SIZE;
        extraInfo++;
    }
}
