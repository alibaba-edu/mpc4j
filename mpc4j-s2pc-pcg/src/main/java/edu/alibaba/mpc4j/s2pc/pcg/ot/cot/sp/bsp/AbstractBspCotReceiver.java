package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp;

import java.util.Arrays;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract BSP-COT receiver.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public abstract class AbstractBspCotReceiver extends AbstractTwoPartyPto implements BspCotReceiver {
    /**
     * config
     */
    private final BspCotConfig config;
    /**
     * α array
     */
    protected int[] alphaArray;
    /**
     * each num
     */
    protected int eachNum;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * number of COTs
     */
    protected int cotNum;

    protected AbstractBspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int[] alphaArray, int eachNum) {
        checkInitialized();
        MathPreconditions.checkPositive("eachNum", eachNum);
        this.eachNum = eachNum;
        batchNum = alphaArray.length;
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, eachNum))
            .toArray();
        cotNum = BspCotFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, eachNum);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual("preCotNum", preReceiverOutput.getNum(), cotNum);
        }
    }
}
