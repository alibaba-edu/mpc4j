package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.bp;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;

/**
 * abstract BP-CDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractBpCdpprfReceiver extends AbstractTwoPartyPto implements BpCdpprfReceiver {
    /**
     * config
     */
    private final BpCdpprfConfig config;
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

    protected AbstractBpCdpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BpCdpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int[] alphaArray, int eachNum) {
        checkInitialized();
        Preconditions.checkArgument(IntMath.isPowerOfTwo(eachNum));
        this.eachNum = eachNum;
        batchNum = alphaArray.length;
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("α", alpha, eachNum))
            .toArray();
        cotNum = BpCdpprfFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, eachNum);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), BpCdpprfFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
