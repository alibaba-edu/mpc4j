package edu.alibaba.mpc4j.s2pc.pso.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * OPRP协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public abstract class AbstractOprpSender extends AbstractSecureTwoPartyPto implements OprpSender {
    /**
     * 配置项
     */
    private final OprpConfig config;
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
        this.config = config;
    }

    @Override
    public OprpFactory.OprpType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int matchBatchSize) {
        assert matchBatchSize > 0;
        this.maxBatchSize = matchBatchSize;
        maxRoundBatchSize = CommonUtils.getByteLength(maxBatchSize) * Byte.SIZE;
        initialized = false;
    }

    protected void setPtoInput(byte[] key, int batchSize) throws MpcAbortException {
        assert key.length == CommonConstants.BLOCK_BYTE_LENGTH;
        assert batchSize > 0 && batchSize <= maxBatchSize;
        this.key = key;
        this.batchSize = batchSize;
        batchByteSize = CommonUtils.getByteLength(batchSize);
        roundBatchSize = batchByteSize * Byte.SIZE;
        extraInfo++;
    }
}
