package edu.alibaba.mpc4j.common.tool.crypto.prg;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory.PrgType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 使用Bouncy Castle的SM4/ECB模式实现的伪随机数生成器。
 *
 * @author Weiran Liu
 * @date 2021/12/08
 */
public class BcSm4EcbPrg implements Prg {
    /**
     * 输出字节长度
     */
    private final int outputByteLength;
    /**
     * 底层所用的伪随机置换
     */
    private final Prp[] prps;
    /**
     * 是否需要截断处理
     */
    private final boolean needTruncate;

    BcSm4EcbPrg(int outputByteLength) {
        this.outputByteLength = outputByteLength;
        // 所需要的伪随机置换数量
        int prpNum = CommonUtils.getUnitNum(outputByteLength, CommonConstants.BLOCK_BYTE_LENGTH);
        // 是否需要截断输出长度
        needTruncate = (outputByteLength % CommonConstants.BLOCK_BYTE_LENGTH != 0);
        prps = IntStream.range(0, prpNum)
            .mapToObj(prpIndex -> {
                Prp prp = PrpFactory.createInstance(PrpType.BC_SM4);
                // 将种子密钥设置为PRP的索引值
                byte[] key = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                    .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, prpIndex)
                    .array();
                prp.setKey(key);
                return prp;
            })
            .toArray(Prp[]::new);
    }

    @Override
    public int getOutputByteLength() {
        return outputByteLength;
    }

    @Override
    public byte[] extendToBytes(byte[] seed) {
        assert seed.length == CommonConstants.BLOCK_BYTE_LENGTH;
        if (outputByteLength == CommonConstants.BLOCK_BYTE_LENGTH) {
            // 如果只输出一个分组，可以直接返回PRP结果
            // 经过测试，下述代码执行性能竟然比直接return BytesUtils.xor(prps[0].prp(seed), seed)更快
            byte[] prpOutput = prps[0].prp(seed);
            BytesUtils.xori(prpOutput, seed);
            return prpOutput;
        } else if (outputByteLength < CommonConstants.BLOCK_BYTE_LENGTH) {
            // 如果输出小于一个分组，则可以不用ByteBuffer
            byte[] prpOutput = prps[0].prp(seed);
            return Arrays.copyOf(prpOutput, outputByteLength);
        } else {
            ByteBuffer outputByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * prps.length);
            for (Prp prp : prps) {
                // PRF_seed(k) \xor k
                byte[] prpOutput = prp.prp(seed);
                BytesUtils.xori(prpOutput, seed);
                outputByteBuffer.put(prpOutput);
            }
            byte[] output = outputByteBuffer.array();
            return needTruncate ? Arrays.copyOf(output, outputByteLength) : output;
        }
    }

    @Override
    public PrgType getPrgType() {
        return PrgType.BC_SM4_ECB;
    }
}
