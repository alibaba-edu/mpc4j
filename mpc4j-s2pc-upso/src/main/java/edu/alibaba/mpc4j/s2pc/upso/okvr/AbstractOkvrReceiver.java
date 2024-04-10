package edu.alibaba.mpc4j.s2pc.upso.okvr;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * abstract OKVR receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public abstract class AbstractOkvrReceiver extends AbstractTwoPartyPto implements OkvrReceiver {
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
     * batch size
     */
    protected int retrievalSize;
    /**
     * retrieval key array
     */
    protected ByteBuffer[] keyArray;


    protected AbstractOkvrReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, OkvrConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int num, int l, int retrievalSize) {
        // check num
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        // check l
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // check retrieval size
        MathPreconditions.checkPositive("retrieval size", retrievalSize);
        this.retrievalSize = retrievalSize;
        initState();
    }

    protected void setPtoInput(Set<ByteBuffer> keys) {
        checkInitialized();
        // check retrieval size
        MathPreconditions.checkEqual("keys.size()", "retrieval_size", keys.size(), retrievalSize);
        // we do not even require that input array are distinct.
        keyArray = keys.toArray(new ByteBuffer[0]);
        extraInfo++;
    }
}
