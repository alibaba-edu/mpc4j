package edu.alibaba.mpc4j.s2pc.pso.oprf.ra17;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.s2pc.pso.oprf.MpOprfSenderOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * RA17-MPOPRF协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/02/06
 */
public class Ra17MpOprfSenderOutput implements MpOprfSenderOutput {
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * 密钥α
     */
    private final BigInteger alpha;
    /**
     * 伪随机函数输出字节长度
     */
    private final int prfByteLength;
    /**
     * 批处理数量
     */
    private final int batchSize;

    public Ra17MpOprfSenderOutput(EnvType envType, BigInteger alpha, int batchSize) {
        assert batchSize > 0;
        this.batchSize = batchSize;
        ecc = EccFactory.createInstance(envType);
        assert alpha.compareTo(BigInteger.ZERO) > 0 && BigIntegerUtils.less(alpha, ecc.getN());
        // 协议内部会修改alpha，因此这里仍然需要拷贝
        this.alpha = alpha.add(BigInteger.ZERO);
        prfByteLength = ecc.encode(ecc.getG(), false).length;
    }

    @Override
    public int getPrfByteLength() {
        return prfByteLength;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public byte[] getPrf(byte[] input) {
        ECPoint output = ecc.hashToCurve(input);
        // 将椭圆曲线点编码为byte[]，压缩编码效率比非压缩编码慢
        return ecc.encode(ecc.multiply(output, alpha), false);
    }
}
