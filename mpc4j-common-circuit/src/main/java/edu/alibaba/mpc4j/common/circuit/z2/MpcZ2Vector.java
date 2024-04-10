package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.MpcVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * MPC Bit Vector.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public interface MpcZ2Vector extends MpcVector {

    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    BitVector getBitVector();

    /**
     * Get the inner bit vector.
     *
     * @return the inner bit vector.
     */
    BitVector[] getBitVectors();

    /**
     * Sets the inner bit vector.
     * <p></p>
     * There are some specific bit vectors that need to be set via several bit vectors. So, we set the input to an array.
     *
     * @param data the given bit vector(s).
     */
    void setBitVectors(BitVector... data);

    /**
     * Gets the number of bit shares.
     *
     * @return the number of bit shares.
     */
    default int bitNum() {
        return getNum();
    }

    /**
     * Gets the num in bytes.
     *
     * @return the num in bytes.
     */
    int byteNum();

    /**
     * reverse the bits
     */
    void reverseBits();

    /**
     * split inputs with padding zeros into multiple vectors.
     *
     * @param bitNums bit num for each vector.
     * @return split vectors.
     */
    MpcZ2Vector[] splitWithPadding(int[] bitNums);

    /**
     * extend the bits of specific positions with fixed skip length from the end to the front.
     * if destBitLen % skipLen > 0, then there are 0s in the first group.
     * For example, given data = abc, skipLen = 2 and destBitLen = 5
     * the return vectors are [abcbc]
     * given data = abcde, skipLen = 4 and totalBitNum = 13
     * the return vectors are [a000a,bcdebcde]
     *
     * @param destBitLen the bit length of target value.
     * @param skipLen the skip length in extending.
     * @return extended vectors.
     */
    MpcZ2Vector extendBitsWithSkip(int destBitLen, int skipLen);

    /**
     * get the bits of specific positions with fixed skip length from the end to the front.
     * For example, given data = abcdefg, skipLen = 2 and totalBitNum = 3
     * the return vectors are [ade, cfg]
     * given data = a,bcdefghi, skipLen = 1 and totalBitNum = 4
     * the return vectors are [bdfh, cegi]
     *
     * @param totalBitNum how many bits need to take out
     * @param skipLen the fixed skip length
     * @return resulting vectors.
     */
    MpcZ2Vector[] getBitsWithSkip(int totalBitNum, int skipLen);

    /**
     * 基于一定间隔，得到部分bit的数据
     * @param startPos 从哪一个位置开始取
     * @param num 取多少个bit
     * @param skipLen 取位的间隔是多少个bit.
     *
     * @return resulting vector.
     */
    MpcZ2Vector getPointsWithFixedSpace(int startPos, int num, int skipLen);

    /**
     * 基于一定间隔，设置部分bit的数据
     * @param source 从哪一个wire取数据
     * @param startPos 从哪一个位置开始置位
     * @param num 设置多少个bit
     * @param skipLen 置位的间隔是多少个bit
     */
    default void setPointsWithFixedSpace(MpcZ2Vector source, int startPos, int num, int skipLen){
        assert isPlain() == source.isPlain();
        getBitVector().setBitsByInterval(source.getBitVector(), startPos, num, skipLen);
    }
}
