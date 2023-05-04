package edu.alibaba.mpc4j.s2pc.opf.oprf.kkrt16;

import edu.alibaba.mpc4j.common.tool.coder.random.RandomCoder;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfSenderOutput;

import java.util.Arrays;

/**
 * KKRT16-OPRF协议发送方输出。
 *
 * @author Weiran Liu
 * @date 2022/02/07
 */
public class Kkrt16OprfSenderOutput implements OprfSenderOutput {
    /**
     * 编码器
     */
    private final RandomCoder randomCoder;
    /**
     * 全局密钥
     */
    private final byte[] delta;
    /**
     * 关联密钥
     */
    private final byte[][] qs;

    public Kkrt16OprfSenderOutput(RandomCoder randomCoder, byte[] delta, byte[][] qs) {
        this.randomCoder = randomCoder;
        assert delta.length == randomCoder.getCodewordByteLength();
        this.delta = BytesUtils.clone(delta);
        assert qs.length > 0;
        this.qs = Arrays.stream(qs)
            .peek(q -> {
                assert q.length == randomCoder.getCodewordByteLength();
            })
            .map(BytesUtils::clone)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] getPrf(int index, byte[] input) {
        byte[] prf = randomCoder.encode(input);
        BytesUtils.andi(prf, delta);
        BytesUtils.xori(prf, qs[index]);

        return prf;
    }

    @Override
    public int getPrfByteLength() {
        return randomCoder.getCodewordByteLength();
    }

    @Override
    public int getBatchSize() {
        return qs.length;
    }
}
