package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * abstract unbalanced OPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
public abstract class AbstractUbopprfReceiver extends AbstractTwoPartyPto implements UbopprfReceiver {
    /**
     * batch size
     */
    protected int batchSize;
    /**
     * the input / output bit length
     */
    protected int l;
    /**
     * the input / output byte length
     */
    protected int byteL;
    /**
     * the batched input array.
     */
    protected byte[][] inputArray;
    /**
     * the number of target programmed points
     */
    protected int pointNum;

    protected AbstractUbopprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, UbopprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int l, int batchSize, int pointNum) {
        // check l
        MathPreconditions.checkGreaterOrEqual("l", l, CommonConstants.STATS_BIT_LENGTH);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // check batch size
        MathPreconditions.checkPositive("batch size", batchSize);
        this.batchSize = batchSize;
        // check point num
        MathPreconditions.checkPositive("point num", pointNum);
        this.pointNum = pointNum;
        initState();
    }

    protected void setPtoInput(byte[][] inputArray) {
        checkInitialized();
        // check batch size
        MathPreconditions.checkEqual("inputArray.length", "batchSize", inputArray.length, batchSize);
        // we do not even require that input array are distinct.
        this.inputArray = inputArray;
        extraInfo++;
    }
}
