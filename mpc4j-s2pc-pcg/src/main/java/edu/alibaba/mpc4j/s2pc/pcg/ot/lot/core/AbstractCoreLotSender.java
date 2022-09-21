package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 核2^l选1-OT协议发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public abstract class AbstractCoreLotSender extends AbstractSecureTwoPartyPto implements CoreLotSender {
    /**
     * 配置项
     */
    private final CoreLotConfig config;
    /**
     * 输入比特长度
     */
    protected int inputBitLength;
    /**
     * 输入字节长度
     */
    protected int inputByteLength;
    /**
     * 输出比特长度
     */
    protected int outputBitLength;
    /**
     * 输出字节长度
     */
    protected int outputByteLength;
    /**
     * 线性编码器
     */
    protected LinearCoder linearCoder;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 关联值Δ的比特值
     */
    protected boolean[] deltaBinary;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;

    protected AbstractCoreLotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, CoreLotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public CoreLotFactory.CoreLotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int inputBitLength, byte[] delta, int maxNum) {
        assert inputBitLength > 0: "input bit length must be greater than 0: " + inputBitLength;
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        assert delta.length == outputByteLength && BytesUtils.isReduceByteArray(delta, outputBitLength);
        this.delta = BytesUtils.clone(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta, outputBitLength);
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setInitInput(int inputBitLength, int maxNum) {
        assert inputBitLength > 0: "input bit length must be greater than 0: " + inputBitLength;
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        // 生成Δ
        delta = new byte[outputByteLength];
        secureRandom.nextBytes(delta);
        BytesUtils.reduceByteArray(delta, outputBitLength);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta, outputBitLength);
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}
