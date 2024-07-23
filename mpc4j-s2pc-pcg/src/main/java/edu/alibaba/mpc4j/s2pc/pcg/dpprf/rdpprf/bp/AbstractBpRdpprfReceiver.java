package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.bp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract batch-point RDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractBpRdpprfReceiver extends AbstractTwoPartyPto implements BpRdpprfReceiver {
    /**
     * config
     */
    protected final BpRdpprfConfig config;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * n
     */
    protected int eachNum;
    /**
     * log(n)
     */
    protected int eachLogNum;
    /**
     * α array
     */
    protected int[] alphaArray;
    /**
     * α binary arrays
     */
    protected boolean[][] alphaBinaryArray;
    /**
     * negative α binary arrays
     */
    protected boolean[][] notAlphaBinaryArray;

    protected AbstractBpRdpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, BpRdpprfConfig config) {
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
        eachLogNum = LongUtils.ceilLog2(eachNum, 1);
        batchNum = alphaArray.length;
        MathPreconditions.checkPositive("batchNum", batchNum);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> MathPreconditions.checkNonNegativeInRange("alpha", alpha, eachNum))
            .toArray();
        int offset = Integer.SIZE - eachLogNum;
        alphaBinaryArray = new boolean[batchNum][eachLogNum];
        notAlphaBinaryArray = new boolean[batchNum][eachLogNum];
        IntStream.range(0, batchNum).forEach(index -> {
            int alpha = alphaArray[index];
            // 将α展开成二进制
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, eachLogNum).forEach(i -> {
                alphaBinaryArray[index][i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notAlphaBinaryArray[index][i] = !alphaBinaryArray[index][i];
            });
        });
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int eachNum, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, eachNum);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), BpRdpprfFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
