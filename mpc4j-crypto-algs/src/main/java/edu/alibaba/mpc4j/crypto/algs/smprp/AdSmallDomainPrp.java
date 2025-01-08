package edu.alibaba.mpc4j.crypto.algs.smprp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.crypto.algs.smprp.SmallDomainPrpFactory.SmallDomainPrpType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * small-domain PRP used in Incremental Offline/Online PIR (USENIX Security 2022). The implementation comes from
 * adprp.cpp<a href="https://github.com/eniac/incpir/blob/main/inc-pir/src/adprp.cpp">adprp.cpp</a>.
 *
 * @author Weiran Liu
 * @date 2024/8/22
 */
public class AdSmallDomainPrp implements SmallDomainPrp {
    /**
     * standard block length
     */
    private static final int STD_BLOCK_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * number of rounds, see <a href="https://github.com/eniac/incpir/blob/main/inc-pir/src/adprp.hpp">adprp.hpp</a>.
     */
    private static final int ROUNDS = 7;
    /**
     * PRP
     */
    private final Prp prp;
    /**
     * init
     */
    private boolean init;
    /**
     * range
     */
    private int range;
    /**
     * left length in bits
     */
    private int leftBitLength;
    /**
     * right length in bits
     */
    private int rightBitLength;

    public AdSmallDomainPrp(EnvType envType) {
        prp = PrpFactory.createInstance(envType);
        init = false;
    }

    @Override
    public SmallDomainPrpType getType() {
        return SmallDomainPrpType.AD_PRP;
    }

    @Override
    public void init(int range, byte[] key) {
        MathPreconditions.checkPositive("range", range);
        this.range = range;
        // the smallest n such that 2^n > range
        int rangeBitLength = LongUtils.ceilLog2(range) + 1;
        leftBitLength = rangeBitLength / 2;
        rightBitLength = rangeBitLength - leftBitLength;
        prp.setKey(key);
        init = true;
    }

    /**
     * Do PRP for one round.
     *
     * @param block           block, must be at most 16 bits.
     * @param tweak           tweak, must be at most 16 bits.
     * @param outputBitLength output length in bit, must be at most 16.
     * @return result.
     */
    private int round(int block, int tweak, int outputBitLength) {
        // output length must be in range [0, 16].
        assert outputBitLength >= 0 && outputBitLength <= 16;
        // block and tweak must be at most 16 bits
        assert (block & ((1 << Short.SIZE) - 1)) == block;
        assert (tweak & ((1 << Short.SIZE) - 1)) == tweak;
        block = block ^ tweak;

        byte[] plaintext = new byte[STD_BLOCK_LENGTH];
        plaintext[STD_BLOCK_LENGTH - 2] = (byte) ((block >> 8) & 0xFF);
        plaintext[STD_BLOCK_LENGTH - 1] = (byte) (block & 0xFF);
        byte[] ciphertext = prp.prp(plaintext);
        int result = ciphertext[STD_BLOCK_LENGTH - 2];
        result = result << Byte.SIZE;
        result = result | ciphertext[STD_BLOCK_LENGTH - 1];
        // only maintain necessary bits
        result = result & ((1 << outputBitLength) - 1);

        return result;
    }

    /**
     * Feistel PRP.
     *
     * @param block input of PRP, of length at most 32 bits.
     * @return permuted block.
     */
    private int feistelPrp(int block) {
        // split left and right
        int left = (block >> rightBitLength) & ((1 << leftBitLength) - 1);
        int right = ((1 << rightBitLength) - 1) & block;

        int left1, right1;
        int permBlock = 0;

        for (int i = 0; i < ROUNDS; i++) {
            left1 = right;
            right1 = left ^ round(right, i + 1, leftBitLength);

            // concat left and right and re-assign left and right
            if (i == ROUNDS - 1) {
                permBlock = (left1 << leftBitLength) | right1;
            } else {
                permBlock = (left1 << leftBitLength) | right1;
                left = permBlock >> rightBitLength & ((1 << leftBitLength) - 1);
                right = permBlock & ((1 << rightBitLength) - 1);
            }
        }
        return permBlock;
    }

    private int feistelInvPrp(int permBlock) {
        int right = permBlock & ((1 << leftBitLength) - 1);
        int left = (permBlock >> leftBitLength) & ((1 << rightBitLength) - 1);

        int left1, right1;
        int block = 0;

        for (int i = 0; i < ROUNDS; i++) {
            right1 = left;
            left1 = right ^ round(left, ROUNDS - i, leftBitLength);

            if (i == ROUNDS - 1) {
                block = (left1 << rightBitLength) | right1;
            } else {
                block = (left1 << rightBitLength) | right1;
                left = (block >> leftBitLength) & ((1 << rightBitLength) - 1);
                right = block & ((1 << leftBitLength) - 1);
            }
        }
        return block;
    }

    @Override
    public int prp(int plaintext) {
        Preconditions.checkArgument(init);
        MathPreconditions.checkInRange("plaintext", plaintext, 0, range);
        // compute the smallest n s.t. 2^n > range
        int tmp = feistelPrp(plaintext);
        while (tmp >= range) {
            tmp = feistelPrp(tmp);
        }
        return tmp;
    }

    @Override
    public int invPrp(int ciphertext) {
        Preconditions.checkArgument(init);
        MathPreconditions.checkInRange("ciphertext", ciphertext, 0, range);
        int tmp = feistelInvPrp(ciphertext);
        while (tmp >= range) {
            tmp = feistelInvPrp(tmp);
        }
        return tmp;
    }
}
