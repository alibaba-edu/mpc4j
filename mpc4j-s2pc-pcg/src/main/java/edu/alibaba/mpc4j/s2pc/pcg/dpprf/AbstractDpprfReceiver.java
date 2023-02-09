package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.RdpprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * DPPRF abstract receiver.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public abstract class AbstractDpprfReceiver extends AbstractSecureTwoPartyPto implements DpprfReceiver {
    /**
     * config
     */
    private final DpprfConfig config;
    /**
     * max α upper bound
     */
    protected int maxAlphaBound;
    /**
     * max α bit length
     */
    protected int maxH;
    /**
     * max batch num
     */
    private int maxBatchNum;
    /**
     * α upper bound
     */
    protected int alphaBound;
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
    /**
     * α bit length
     */
    protected int h;
    /**
     * batch num
     */
    protected int batchNum;

    protected AbstractDpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, RdpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public DpprfFactory.DpprfType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBatchNum, int maxAlphaBound) {
        assert maxBatchNum > 0: "maxBatchNum must be greater than 0:" + maxBatchNum;
        this.maxBatchNum = maxBatchNum;
        assert maxAlphaBound > 0 : "maxAlphaBound must be greater than 0: " + maxAlphaBound;
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound, 1);
        initialized = false;
    }

    protected void setPtoInput(int[] alphaArray, int alphaBound) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert alphaBound > 0 && alphaBound <= maxAlphaBound
            : "alphaBound must be in range (0, " + maxAlphaBound + "]: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound, 1);
        batchNum = alphaArray.length;
        assert batchNum > 0 && batchNum <= maxBatchNum : "batch must be in range (0, " + maxBatchNum + "]: " + batchNum;
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> {
                assert alpha >= 0 && alpha < alphaBound : "α must be in range [0, " + alphaBound + "): " + alpha;
            })
            .toArray();
        int offset = Integer.SIZE - h;
        alphaBinaryArray = new boolean[batchNum][h];
        notAlphaBinaryArray = new boolean[batchNum][h];
        IntStream.range(0, batchNum).forEach(index -> {
            int alpha = alphaArray[index];
            // 将α展开成二进制
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, h).forEach(i -> {
                alphaBinaryArray[index][i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notAlphaBinaryArray[index][i] = !alphaBinaryArray[index][i];
            });
        });
        extraInfo++;
    }

    protected void setPtoInput(int[] alphaArray, int alphaBound, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alphaArray, alphaBound);
        assert preReceiverOutput.getNum() >= DpprfFactory.getPrecomputeNum(config, batchNum, alphaBound);
    }
}
