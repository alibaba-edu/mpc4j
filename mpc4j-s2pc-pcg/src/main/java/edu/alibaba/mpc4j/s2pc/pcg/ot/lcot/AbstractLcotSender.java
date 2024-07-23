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
    protected int l;
    /**
     * 输入字节长度
     */
    protected int byteL;
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

    protected void setInitInput(int l, byte[] delta) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        linearCoder = LinearCoderFactory.getInstance(l);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        MathPreconditions.checkEqual("Δ.length", "outputByteLength", delta.length, outputByteLength);
        Preconditions.checkArgument(BytesUtils.isReduceByteArray(delta, outputBitLength));
        this.delta = BytesUtils.clone(delta);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta, outputBitLength);
        initState();
    }

    protected void setInitInput(int l) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        linearCoder = LinearCoderFactory.getInstance(l);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        // 生成Δ
        delta = new byte[outputByteLength];
        secureRandom.nextBytes(delta);
        BytesUtils.reduceByteArray(delta, outputBitLength);
        deltaBinary = BinaryUtils.byteArrayToBinary(delta, outputBitLength);
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        byteNum = CommonUtils.getByteLength(num);
        extraInfo++;
    }
}
