package edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoder;
import edu.alibaba.mpc4j.common.tool.coder.linear.LinearCoderFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotFactory;

import java.util.Arrays;

/**
 * n选1-COT协议接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/5/25
 */
public abstract class AbstractLhotReceiver extends AbstractSecureTwoPartyPto implements LhotReceiver {
    /**
     * 配置项
     */
    private final LhotConfig config;
    /**
     * 输入比特长度
     */
    protected int inputBitLength;
    /**
     * 输入字节长度
     */
    protected int inputByteLength;
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
    /**
     * 选择值数组
     */
    protected byte[][] choices;

    protected AbstractLhotReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, LhotConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    @Override
    public LotFactory.LotType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int inputBitLength, int maxNum) {
        assert inputBitLength > 0: "InputBitLength must be greater than 0: " + inputBitLength;
        this.inputBitLength = inputBitLength;
        inputByteLength = CommonUtils.getByteLength(inputBitLength);
        linearCoder = LinearCoderFactory.getInstance(inputBitLength);
        outputBitLength = linearCoder.getCodewordBitLength();
        outputByteLength = linearCoder.getCodewordByteLength();
        assert maxNum > 0 : "MaxNum must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(byte[][] choices) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert choices.length > 0 && choices.length <= maxNum : "# of choices must be in range (0, " + maxNum + "]";
        num = choices.length;
        byteNum = CommonUtils.getByteLength(num);
        // 拷贝一份
        this.choices = Arrays.stream(choices)
            .peek(choice -> {
                assert choice.length == inputByteLength && BytesUtils.isReduceByteArray(choice, inputBitLength);
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
