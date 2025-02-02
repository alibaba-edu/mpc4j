package edu.alibaba.mpc4j.s2pc.pcg.ot.lcot;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * 2^l选1-COT协议接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public abstract class AbstractLcotReceiver extends AbstractTwoPartyPto implements LcotReceiver {
    /**
     * 输入比特长度
     */
    protected int l;
    /**
     * 输入字节长度
     */
    protected int byteL;
    /**
     * 输出随机量比特长度
     */
    protected int outputBitLength;
    /**
     * 输出随机量字节长度
     */
    protected int outputByteLength;
    /**
     * 线性编码器
     */
    protected LinearCoder linearCoder;
    /**
     * 数量
     */
    protected int num;
    /**
     * 字节数量
     */
    protected int byteNum;
    /**
     * 选择值数组
     */
    protected byte[][] choices;

    protected AbstractLcotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, LcotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int l) {
        MathPreconditions.checkPositive("l", l);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        linearCoder = LinearCoderFactory.getInstance(l);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        initState();
    }

    protected void setPtoInput(byte[][] choices) {
        checkInitialized();
        MathPreconditions.checkPositive("num", choices.length);
        num = choices.length;
        byteNum = CommonUtils.getByteLength(num);
        // 拷贝一份
        this.choices = Arrays.stream(choices)
            .peek(choice -> {
                MathPreconditions.checkEqual("choice.length", "inputByteLength", choice.length, byteL);
                Preconditions.checkArgument(BytesUtils.isReduceByteArray(choice, l));
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
