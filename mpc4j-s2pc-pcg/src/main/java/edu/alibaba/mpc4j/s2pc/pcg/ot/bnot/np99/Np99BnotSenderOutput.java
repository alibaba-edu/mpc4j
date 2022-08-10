package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.np99;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.nio.ByteBuffer;


/**
 * NP99-基础n选1-OT协议发送方输出。
 *
 * @author Hanwen Feng
 * @date 2022/07/20
 */
public class Np99BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * n的比特长度
     */
    private final int nBitLength;
    /**
     * 基础OT发送方输出
     */
    private final BaseOtSenderOutput baseOtSenderOutput;
    /**
     * KDF
     */
    private final Kdf kdf;

    public Np99BnotSenderOutput(EnvType envType, int n, int num, final BaseOtSenderOutput baseOtSenderOutput) {
        init(n, num);
        nBitLength = LongUtils.ceilLog2(n);
        assert nBitLength * num == baseOtSenderOutput.getNum();
        this.baseOtSenderOutput = baseOtSenderOutput;
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public byte[] getRb(int index, int choice) {
        assertValidInput(index, choice);
        boolean[] binaryChoice = BinaryUtils.byteArrayToBinary(IntUtils.intToByteArray(choice), nBitLength);
        ByteBuffer rbBuffer = ByteBuffer.allocate(nBitLength * CommonConstants.BLOCK_BYTE_LENGTH);
        for (int i = 0; i < nBitLength; i++) {
            rbBuffer = binaryChoice[i] ? rbBuffer.put(baseOtSenderOutput.getR1(index * nBitLength + i)) :
                    rbBuffer.put(baseOtSenderOutput.getR0(index * nBitLength + i));
        }
        return kdf.deriveKey(rbBuffer.array());
    }
}
