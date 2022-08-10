package edu.alibaba.mpc4j.common.tool.crypto.prf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory.PrfType;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * 使用Bouncy Castle的ECB-SM4实现的PRF。方案构造来自于论文：
 * Chase M, Miao P. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020.
 * 第4.2节：Instantiation of Cryptographic Primitives。
 *
 * @author Weiran Liu
 * @date 2021/12/13
 */
class BcSm4CbcPrf implements Prf {
    /**
     * 伪随机数生成器
     */
    private final Prg prg;
    /**
     * 伪随机置换
     */
    private final Prp prp;
    /**
     * 输出字节长度
     */
    private final int outputByteLength;
    /**
     * 密钥
     */
    private byte[] key;

    BcSm4CbcPrf(int outputByteLength) {
        this.outputByteLength = outputByteLength;
        prg = PrgFactory.createInstance(PrgFactory.PrgType.BC_SM4_ECB, outputByteLength);
        prp = PrpFactory.createInstance(PrpFactory.PrpType.BC_SM4);
    }

    @Override
    public int getOutputByteLength() {
        return prg.getOutputByteLength();
    }

    @Override
    public void setKey(byte[] key) {
        prp.setKey(key);
        // 虽然PRP已经拷贝了密钥，但这里仍然需要再拷贝一次
        this.key = BytesUtils.clone(key);
    }

    @Override
    public byte[] getKey() {
        return key;
    }

    @Override
    public byte[] getBytes(byte[] message) {
        assert message.length > 0;
        byte[] mac = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        // 计算MAC值
        if (message.length == CommonConstants.BLOCK_BYTE_LENGTH) {
            mac = prp.prp(message);
        } else if (message.length < CommonConstants.BLOCK_BYTE_LENGTH) {
            mac = prp.prp(Arrays.copyOf(message, CommonConstants.BLOCK_BYTE_LENGTH));
        } else {
            int blockNum = CommonUtils.getUnitNum(message.length, CommonConstants.BLOCK_BYTE_LENGTH);
            byte[] paddingMessage = new byte[blockNum * CommonConstants.BLOCK_BYTE_LENGTH];
            System.arraycopy(message, 0, paddingMessage, paddingMessage.length - message.length, message.length);
            byte[] x = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int blockIndex = 0; blockIndex < blockNum; blockIndex++) {
                System.arraycopy(paddingMessage, blockIndex * CommonConstants.BLOCK_BYTE_LENGTH,
                    x, 0, CommonConstants.BLOCK_BYTE_LENGTH);
                BytesUtils.xori(mac, x);
                mac = prp.prp(mac);
            }
        }
        // 输出扩展结果
        if (outputByteLength == CommonConstants.BLOCK_BYTE_LENGTH) {
            return mac;
        } else if (outputByteLength < CommonConstants.BLOCK_BYTE_LENGTH) {
            return Arrays.copyOf(mac, outputByteLength);
        } else {
            return prg.extendToBytes(mac);
        }
    }

    @Override
    public PrfType getPrfType() {
        return PrfType.BC_SM4_CBC;
    }
}
