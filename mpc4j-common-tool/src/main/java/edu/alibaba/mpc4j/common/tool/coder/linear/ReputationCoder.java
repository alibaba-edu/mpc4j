package edu.alibaba.mpc4j.common.tool.coder.linear;

import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * 重复编码器。
 *
 * @author Weiran Liu
 * @date 2021/12/14
 */
public class ReputationCoder implements LinearCoder {
    /**
     * 码字比特长度
     */
    private final int codewordBitLength;
    /**
     * 码字字节长度
     */
    private final int codewordByteLength;
    /**
     * 输入为1的编码结果
     */
    private final byte[] trueEncodeBytes;
    /**
     * 输入为0的编码结果
     */
    private final byte[] falseEncodeBytes;

    public ReputationCoder(int codewordBitLength) {
        assert codewordBitLength > 0;
        this.codewordBitLength = codewordBitLength;
        this.codewordByteLength = CommonUtils.getByteLength(codewordBitLength);
        // 输入为0x01时的编码结果为全1
        trueEncodeBytes = new byte[codewordByteLength];
        Arrays.fill(trueEncodeBytes, (byte)0xFF);
        BytesUtils.reduceByteArray(trueEncodeBytes, codewordBitLength);
        falseEncodeBytes = new byte[codewordByteLength];
    }

    @Override
    public int getDatawordBitLength() {
        return 1;
    }

    @Override
    public int getDatawordByteLength() {
        return 1;
    }

    @Override
    public int getCodewordBitLength() {
        return codewordBitLength;
    }

    @Override
    public int getCodewordByteLength() {
        return codewordByteLength;
    }

    @Override
    public int getMinimalHammingDistance() {
        return codewordBitLength;
    }

    @Override
    public byte[] encode(byte[] input) {
        assert input.length == 1 && (input[0] == 0x01 || input[0] == 0x00);
        if (input[0] == 0x00) {
            return BytesUtils.clone(falseEncodeBytes);
        } else {
            return BytesUtils.clone(trueEncodeBytes);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < codewordBitLength; i++) {
            builder.append("0 ");
        }
        builder.append('\n');
        for (int i = 0; i < codewordBitLength; i++) {
            builder.append("1 ");
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ReputationCoder)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        ReputationCoder that = (ReputationCoder)obj;
        return new EqualsBuilder().append(this.codewordBitLength, that.codewordBitLength).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.codewordBitLength).toHashCode();
    }
}
