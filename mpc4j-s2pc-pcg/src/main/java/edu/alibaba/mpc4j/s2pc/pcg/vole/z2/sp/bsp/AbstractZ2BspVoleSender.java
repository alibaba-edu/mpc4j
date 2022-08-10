package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2-BSP-VOLE发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public abstract class AbstractZ2BspVoleSender extends AbstractSecureTwoPartyPto implements Z2BspVoleSender {
    /**
     * 配置项
     */
    private final Z2BspVoleConfig config;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 最大数量对数
     */
    protected int maxH;
    /**
     * 最大批处理数量
     */
    private int maxBatch;
    /**
     * 单点索引值数组
     */
    protected int[] alphaArray;
    /**
     * α比特值数组
     */
    protected boolean[][] alphaBinaryArray;
    /**
     * 非α比特值数组
     */
    protected boolean[][] notAlphaBinaryArray;
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
     * 批处理数量
     */
    protected int batch;

    protected AbstractZ2BspVoleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, Z2BspVoleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public Z2BspVoleFactory.Z2BspVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxBatch, int maxNum) {
        assert maxNum > 0 : "maxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        maxH = LongUtils.ceilLog2(maxNum);
        assert maxBatch > 0 : "maxBatch must be greater than 0:" + maxBatch;
        this.maxBatch = maxBatch;
        initialized = false;
    }

    protected void setPtoInput(int[] alphaArray, int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        numOffset = byteNum * Byte.SIZE - num;
        h = LongUtils.ceilLog2(num);
        batch = alphaArray.length;
        assert batch > 0 && batch <= maxBatch : "batch must be in range (0, " + maxBatch + "]: " + batch;
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> {
                assert alpha >= 0 && alpha < num : "α must be in range [0, " + num + "): " + alpha;
            })
            .toArray();
        int offset = Integer.SIZE - h;
        alphaBinaryArray = new boolean[alphaArray.length][h];
        notAlphaBinaryArray = new boolean[alphaArray.length][h];
        IntStream.range(0, alphaArray.length).forEach(index -> {
            int alpha = alphaArray[index];
            // 将α展开成二进制
            byte[] alphaBytes = IntUtils.intToByteArray(alpha);
            IntStream.range(0, h).forEach(i -> {
                alphaBinaryArray[index][i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
                notAlphaBinaryArray[index][i] = !alphaBinaryArray[index][i];
            });
        });
        // 一次并行处理m个数据
        extraInfo += batch;
    }

    protected void setPtoInput(int[] alphaArray, int num, Z2VoleSenderOutput preSenderOutput) {
        setPtoInput(alphaArray, num);
        assert preSenderOutput.getNum() >= Z2BspVoleFactory.getPrecomputeNum(config, alphaArray.length, num);
    }
}
