package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * 2^l选1-COT协议发送方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public abstract class AbstractLcotSender extends AbstractTwoPartyPto implements LcotSender {
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

    protected AbstractLcotSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, LcotConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput(int inputBitLength, byte[] delta, int maxNum) {
        MathPreconditions.checkPositive("inputBitLength", inputBitLength);
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        MathPreconditions.checkEqual("Δ.length", "outputByteLength", delta.length, outputByteLength);
        Preconditions.checkArgument(BytesUtils.isReduceByteArray(delta, outputBitLength));
        this.delta = BytesUtils.clone(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta, outputBitLength);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setInitInput(int inputBitLength, int maxNum) {
        MathPreconditions.checkPositive("inputBitLength", inputBitLength);
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
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}
