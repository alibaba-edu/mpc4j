package edu.alibaba.work.femur;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.map.TLongObjectMap;

import java.util.stream.IntStream;

/**
 * Abstract PGM-index range keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public abstract class AbstractFemurRpcPirServer extends AbstractTwoPartyPto implements FemurRpcPirServer {
    /**
     * database size
     */
    protected int n;
    /**
     * entry bit length
     */
    protected int l;
    /**
     * long for ⊥
     */
    protected long botLong = Long.MAX_VALUE;
    /**
     * entry byte length
     */
    protected int byteL;
    /**
     * max batch num
     */
    protected int maxBatchNum;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractFemurRpcPirServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, FemurRpcPirConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(TLongObjectMap<byte[]> keyValueMap, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("l", l);
        MathPreconditions.checkEqual("l", "0", l % Long.SIZE, 0);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositive("n", keyValueMap.size());
        n = keyValueMap.size();
        long[] keyArray = keyValueMap.keys();
        IntStream.range(0, n).forEach(i -> {
            Preconditions.checkArgument(keyArray[i] != botLong, "k_i must not equal ⊥");
            byte[] entry = keyValueMap.get(keyArray[i]);
            Preconditions.checkArgument(entry.length == byteL);
        });
        MathPreconditions.checkPositive("max batch num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int batchNum) {
        MathPreconditions.checkPositiveInRangeClosed("batch num", batchNum, maxBatchNum);
        this.batchNum = batchNum;
        checkInitialized();
    }
}
