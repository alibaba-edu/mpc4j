package edu.alibaba.mpc4j.s2pc.aby.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;

import java.util.Arrays;

/**
 * BC协议参与方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface BcParty extends TwoPartyPto, SecurePto {

    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    BcFactory.BcType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxRoundNum 最大单轮数量。
     * @param updateNum   更新数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxRoundNum, int updateNum) throws MpcAbortException;

    /**
     * 执行AND运算，得到zi，满足z0 ⊕ z1 = z = x & y = (x0 ⊕ x1) & (y0 ⊕ y1)。
     *
     * @param xi xi。
     * @param yi yi。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BcSquareVector and(BcSquareVector xi, BcSquareVector yi) throws MpcAbortException;

    /**
     * 执行XOR运算，得到zi，满足z0 ⊕ z1 = z = x ^ y = (x0 ⊕ x1) ^ (y0 ⊕ y1)。
     *
     * @param xi xi。
     * @param yi yi。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BcSquareVector xor(BcSquareVector xi, BcSquareVector yi) throws MpcAbortException;

    /**
     * 执行NOT运算，得到zi，满足z0 ⊕ z1 = z = !x = (x0 ⊕ x1)。
     *
     * @param xi xi。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    BcSquareVector not(BcSquareVector xi) throws MpcAbortException;

    /**
     * 执行OR运算，得到zi，满足z0 ⊕ z1 = z = x | y = (x0 ⊕ x1) | (y0 ⊕ y1)。
     *
     * @param xi xi。
     * @param yi yi。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    default BcSquareVector or(BcSquareVector xi, BcSquareVector yi) throws MpcAbortException {
        return xor(xor(xi, yi), and(xi, yi));
    }

    /**
     * 执行MUX运算，得到zi，满足：
     * - 如果c0 ⊕ c1 = c == 0，则z0 ⊕ z1 = z = x。
     * - 如果c0 ⊕ c1 = c == 1，则z0 ⊕ z1 = z = y。
     *
     * @param xi xi。
     * @param yi yi。
     * @param ci ci。
     * @return zi。
     * @throws MpcAbortException 如果协议异常中止。
     */
    default BcSquareVector mux(BcSquareVector xi, BcSquareVector yi, BcSquareVector ci) throws MpcAbortException {
        assert xi.bitLength() == yi.bitLength();
        assert ci.bitLength() == 1;
        byte[] choiceBytes = new byte[xi.byteLength()];
        byte[] ciBytes = ci.getBytes();
        Arrays.fill(choiceBytes, BinaryUtils.getBoolean(ciBytes, Byte.SIZE - 1) ? (byte)0xFF : (byte)0x00);
        BcSquareVector choice = BcSquareVector.create(choiceBytes, xi.bitLength(), ci.isPublic());
        BcSquareVector t = xor(xi, yi);
        t = and(t, choice);
        return xor(t, xi);
    }

    /**
     * 返回AND门数量。
     *
     * @param reset 是否重置。
     * @return AND门数量。
     */
    long andGateNum(boolean reset);

    /**
     * 返回XOR门数量。
     *
     * @param reset 是否重置。
     * @return XOR门数量。
     */
    long xorGateNum(boolean reset);
}
