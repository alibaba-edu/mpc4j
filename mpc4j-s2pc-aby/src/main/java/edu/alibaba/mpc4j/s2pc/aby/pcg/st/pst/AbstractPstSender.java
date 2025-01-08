package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.stream.IntStream;

/**
 * Abstract partial ST sender
 *
 * @author Feng Han
 * @date 2024/8/5
 */
public abstract class AbstractPstSender extends AbstractTwoPartyPto implements PstSender {
    /**
     * config
     */
    protected final PstConfig config;
    /**
     * batch num
     */
    protected int batchNum;
    /**
     * each num
     */
    protected int eachNum;
    /**
     * permutation π array
     */
    protected int[][] piArray;
    /**
     * element byte length
     */
    protected int byteLength;
    /**
     * n * log(n)
     */
    protected int cotNum;

    protected AbstractPstSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, PstConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int[][] piArray, int byteLength) {
        checkInitialized();
        batchNum = piArray.length;
        MathPreconditions.checkPositive("batchNum", batchNum);
        eachNum = piArray[0].length;
        Preconditions.checkArgument(IntMath.isPowerOfTwo(eachNum), "eachNum must be a power of 2: %s", eachNum);
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(piArray[batchIndex]));
            MathPreconditions.checkEqual("n", batchIndex + "-th π.length", eachNum, piArray[batchIndex].length);
        });
        this.piArray = piArray;
        MathPreconditions.checkPositive("byte_length", byteLength);
        this.byteLength = byteLength;
        cotNum = PstFactory.getPrecomputeNum(config, batchNum, eachNum);
        extraInfo++;
    }

    protected void setPtoInput(int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput) {
        setPtoInput(piArray, byteLength);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), PstFactory.getPrecomputeNum(config, batchNum, eachNum)
            );
        }
    }
}
