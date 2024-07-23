package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.math.BigInteger;
import java.util.stream.IntStream;

/**
 * Abstract Zl Truncation Party.
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public abstract class AbstractZlTruncParty extends AbstractTwoPartyPto implements ZlTruncParty {
    /**
     * max num
     */
    protected int maxNum;
    /**
     * max l
     */
    protected int maxL;
    /**
     * num
     */
    protected int num;
    /**
     * Zl instance
     */
    protected Zl zl;
    /**
     * l.
     */
    protected int l;
    /**
     * l in bytes
     */
    protected int byteL;
    /**
     * zl range bound
     */
    protected BigInteger n;

    public AbstractZlTruncParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlTruncConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
    }

    protected void setInitInput(int maxL, int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        MathPreconditions.checkPositive("maxL", maxL);
        this.maxNum = maxNum;
        this.maxL = maxL;
        initState();
    }

    protected void setPtoInput(SquareZlVector xi, int s) {
        checkInitialized();
        MathPreconditions.checkPositive("shift bit", s);
        MathPreconditions.checkPositiveInRangeClosed("num", xi.getNum(), maxNum);
        MathPreconditions.checkPositiveInRangeClosed("l", xi.getZl().getL(), maxL);
        zl = xi.getZl();
        n = zl.getRangeBound();
        l = zl.getL();
        byteL = zl.getByteL();
        num = xi.getNum();
    }

    protected BitVector[] getIi(SquareZlVector xi) {
        BigInteger lowerBound = n.divide(BigInteger.valueOf(3));
        BigInteger upperBound = n.shiftLeft(1).divide(BigInteger.valueOf(3)).add(BigInteger.ONE);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        int[][] i1 = intStream.mapToObj(index -> {
            BigInteger x = xi.getZlVector().getElement(index);
            if (x.compareTo(lowerBound) <= 0) {
                return new int[]{1, 0, 0};
            } else if (x.compareTo(upperBound) > 0) {
                return new int[]{0, 0, 1};
            } else {
                return new int[]{0, 1, 0};
            }
        }).toArray(int[][]::new);
        BitVector c = BitVectorFactory.createZeros(num);
        BitVector d = BitVectorFactory.createZeros(num);
        BitVector e = BitVectorFactory.createZeros(num);
        for (int index = 0; index < num; index++) {
            if (i1[index][0] == 1) {
                c.set(index, true);
            }
            if (i1[index][1] == 1) {
                d.set(index, true);
            }
            if (i1[index][2] == 1) {
                e.set(index, true);
            }
        }
        return new BitVector[]{c, d, e};
    }

    protected ZlVector iDiv(BigInteger[] input, int d) {
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] element = intStream.mapToObj(index -> input[index].shiftRight(d)).toArray(BigInteger[]::new);
        return ZlVector.create(zl, element);
    }
}
