package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

import java.util.stream.IntStream;

/**
 * Z2-SSP-VOLE协议发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/13
 */
public abstract class AbstractZ2SspVoleSender extends AbstractSecureTwoPartyPto implements Z2SspVoleSender {
    /**
     * 配置项
     */
    private final Z2SspVoleConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大数量对数
     */
    protected int maxH;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;
    /**
     * 偏移量
     */
    protected int numOffset;
    /**
     * 数量对数
     */
    protected int h;
    /**
     * α
     */
    protected int alpha;
    /**
     * 单点索引比特值
     */
    protected boolean[] alphaBinary;
    /**
     * 反单点索引比特值
     */
    protected boolean[] notAlphaBinary;

    protected AbstractZ2SspVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Z2SspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Z2SspVoleFactory.Z2SspVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxNum) {
        assert maxNum > 0: "maxNum must be greater than 0: " + maxNum;
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
        byteNum = CommonUtils.getByteLength(num);
        numOffset = byteNum * Byte.SIZE - num;
        h = LongUtils.ceilLog2(num);
        assert alpha >= 0 && alpha < num : "α must be in range [0, " + num + "): " + alpha;
        this.alpha = alpha;
        // 将alpha展开成二进制
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

    protected void setPtoInput(int alpha, int num, Z2VoleSenderOutput preSenderOutput) {
        setPtoInput(alpha, num);
        assert preSenderOutput.getNum() >= Z2SspVoleFactory.getPrecomputeNum(config, num);
    }
}
