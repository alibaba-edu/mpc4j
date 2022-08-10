package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * SSP-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public abstract class AbstractSspCotReceiver extends AbstractSecureTwoPartyPto implements SspCotReceiver {
    /**
     * 配置项
     */
    private final SspCotConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大数量对数
     */
    protected int maxH;
    /**
     * α
     */
    protected int alpha;
    /**
     * α比特值
     */
    protected boolean[] alphaBinary;
    /**
     * 非α比特值
     */
    protected boolean[] notAlphaBinary;
    /**
     * 数量
     */
    protected int num;
    /**
     * 数量对数
     */
    protected int h;

    protected AbstractSspCotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SspCotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public SspCotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        maxH = LongUtils.ceilLog2(maxNum);
        initialized = false;
    }

    protected void setPtoInput(int alpha, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        h = LongUtils.ceilLog2(num);
        assert alpha >= 0 && alpha < num : "α must be in range [0, " + num + "): " + alpha;
        this.alpha = alpha;
        // 将α展开成二进制
        alphaBinary = new boolean[h];
        notAlphaBinary = new boolean[h];
        byte[] alphaBytes = IntUtils.intToByteArray(alpha);
        int offset = Integer.SIZE - h;
        IntStream.range(0, h).forEach(i -> {
            alphaBinary[i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
            notAlphaBinary[i] = !alphaBinary[i];
        });
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alpha, num);
        assert preReceiverOutput.getNum() >= SspCotFactory.getPrecomputeNum(config, num);
    }
}
