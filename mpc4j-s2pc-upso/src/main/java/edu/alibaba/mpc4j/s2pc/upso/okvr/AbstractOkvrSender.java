package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * abstract OKVR sender.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public abstract class AbstractOkvrSender extends AbstractTwoPartyPto implements OkvrSender {
    /**
     * number of points
     */
    protected int num;
    /**
     * value bit length
     */
    protected int l;
    /**
     * value byte length
     */
    protected int byteL;
    /**
     * retrieval size
     */
    protected int retrievalSize;
    /**
     * key-value map
     */
    protected Map<ByteBuffer, byte[]> keyValueMap;

    protected AbstractOkvrSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, OkvrConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(Map<ByteBuffer, byte[]> keyValueMap, int l, int retrievalSize) {
        // check num
        num = keyValueMap.size();
        MathPreconditions.checkPositive("num", keyValueMap.size());
        // check l
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // check retrieval size
        MathPreconditions.checkGreater("retrieval size", retrievalSize, 1);
        this.retrievalSize = retrievalSize;
        // check values
        keyValueMap.forEach((key, value) -> {
            assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        });
        this.keyValueMap = keyValueMap;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
