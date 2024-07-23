package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

/**
 * abstract standard keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/7/19
 */
public abstract class AbstractStdKwPirServer<T> extends AbstractTwoPartyPto implements StdKwPirServer<T> {
    /**
     * database size
     */
    protected int n;
    /**
     * value bit length
     */
    protected int l;
    /**
     * value byte length
     */
    protected int byteL;
    /**
     * ByteBuffer for ⊥
     */
    protected ByteBuffer botByteBuffer;
    /**
     * max batch num
     */
    private int maxBatchNum;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractStdKwPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, StdKwPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(Map<T, byte[]> keyValueMap, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositive("n", keyValueMap.size());
        n = keyValueMap.size();
        byte[] bot = new byte[byteL];
        Arrays.fill(bot, (byte) 0xFF);
        BytesUtils.reduceByteArray(bot, this.l);
        botByteBuffer = ByteBuffer.wrap(bot);
        keyValueMap.forEach((keyword, value) -> {
            ByteBuffer keywordByteBuffer = ByteBuffer.wrap(ObjectUtils.objectToByteArray(keyword));
            Preconditions.checkArgument(!keywordByteBuffer.equals(botByteBuffer), "k_i must not equal ⊥");
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(value, byteL, this.l));
        });
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int batchNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", batchNum, maxBatchNum);
        this.batchNum = batchNum;
    }
}
