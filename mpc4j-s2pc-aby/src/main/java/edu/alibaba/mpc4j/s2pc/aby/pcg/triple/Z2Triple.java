package edu.alibaba.mpc4j.s2pc.aby.pcg.triple;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Z2 triple.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public class Z2Triple implements MergedPcgPartyOutput {
    /**
     * 'a'
     */
    private BitVector a;
    /**
     * 'b'
     */
    private BitVector b;
    /**
     * 'c'
     */
    private BitVector c;

    /**
     * create a triple where each element is represented by bytes.
     *
     * @param num num.
     * @param a   'a' represented by bytes.
     * @param b   'b' represented by bytes.
     * @param c   'c' represented by bytes.
     * @return a triple.
     */
    public static Z2Triple create(int num, byte[] a, byte[] b, byte[] c) {
        if (num == 0) {
            return createEmpty();
        } else {
            Z2Triple triple = new Z2Triple();
            triple.a = BitVectorFactory.create(num, a);
            triple.b = BitVectorFactory.create(num, b);
            triple.c = BitVectorFactory.create(num, c);
            return triple;
        }
    }

    /**
     * Creates a triple.
     *
     * @param envType           environment.
     * @param cotSenderOutput   COT sender output.
     * @param cotReceiverOutput COT receiver output.
     * @return a triple.
     */
    public static Z2Triple create(EnvType envType, CotSenderOutput cotSenderOutput, CotReceiverOutput cotReceiverOutput) {
        MathPreconditions.checkEqual(
            "sender.length", "receiver.length", cotSenderOutput.getNum(), cotReceiverOutput.getNum()
        );
        int num = cotSenderOutput.getNum();
        if (num == 0) {
            return createEmpty();
        } else {
            RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
            byte[][] r0Array = rotSenderOutput.getR0Array();
            byte[][] r1Array = rotSenderOutput.getR1Array();
            byte[] c0 = BytesUtils.extractLsb(r0Array);
            byte[] b0 = BytesUtils.extractLsb(r0Array);
            byte[] x1 = BytesUtils.extractLsb(r1Array);
            BytesUtils.xori(b0, x1);
            RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
            byte[] a0 = BinaryUtils.binaryToRoundByteArray(cotReceiverOutput.getChoices());
            byte[][] rbArray = rotReceiverOutput.getRbArray();
            // R sets u = xa
            byte[] cb = BytesUtils.extractLsb(rbArray);
            // Finally, each Pi sets ci = (ai ⊙ bi) ⊕ ui ⊕ vi. This is the ui ⊕ vi part.
            BytesUtils.xori(c0, cb);
            byte[] temp = BytesUtils.and(a0, b0);
            BytesUtils.xori(c0, temp);

            return create(num, a0, b0, c0);
        }
    }

    /**
     * create an empty triple.
     *
     * @return an empty triple.
     */
    public static Z2Triple createEmpty() {
        Z2Triple triple = new Z2Triple();
        triple.a = BitVectorFactory.createEmpty();
        triple.b = BitVectorFactory.createEmpty();
        triple.c = BitVectorFactory.createEmpty();

        return triple;
    }

    /**
     * create a random triple.
     *
     * @param num          num.
     * @param secureRandom random state.
     * @return a random triple.
     */
    public static Z2Triple createRandom(int num, SecureRandom secureRandom) {
        if (num == 0) {
            return createEmpty();
        } else {
            Z2Triple triple = new Z2Triple();
            triple.a = BitVectorFactory.createRandom(num, secureRandom);
            triple.b = BitVectorFactory.createRandom(num, secureRandom);
            triple.c = BitVectorFactory.createRandom(num, secureRandom);

            return triple;
        }
    }

    /**
     * Creates a random triple.
     *
     * @param that     given triple.
     * @param secureRandom random state.
     * @return a random triple.
     */
    public static Z2Triple createRandom(Z2Triple that, SecureRandom secureRandom) {
        int num = that.getNum();
        if (num == 0) {
            return createEmpty();
        } else {
            Z2Triple triple = new Z2Triple();
            triple.a = BitVectorFactory.createRandom(that.getNum(), secureRandom);
            triple.b = BitVectorFactory.createRandom(that.getNum(), secureRandom);
            // compute c1 = (a0 + a1) * (b0 + b1) - c0
            BitVector a = that.a.xor(triple.a);
            BitVector b = that.b.xor(triple.b);
            triple.c = a.and(b);
            triple.c.xori(that.c);
            return triple;
        }
    }

    /**
     * create a triple where each element is represented by BitVector.
     *
     * @param a   a represented by BitVector.
     * @param b   b represented by BitVector.
     * @param c   c represented by BitVector.
     * @return a triple.
     */
    private static Z2Triple create(BitVector a, BitVector b, BitVector c) {
        assert a.bitNum() == b.bitNum() && a.bitNum() == c.bitNum();
        Z2Triple triple = new Z2Triple();
        triple.a = a;
        triple.b = b;
        triple.c = c;

        return triple;
    }

    /**
     * private constructor.
     */
    private Z2Triple() {
        // empty
    }

    @Override
    public int getNum() {
        return a.bitNum();
    }

    @Override
    public Z2Triple copy() {
        Z2Triple copy = new Z2Triple();
        copy.a = a.copy();
        copy.b = b.copy();
        copy.c = c.copy();
        return copy;
    }

    @Override
    public Z2Triple split(int splitNum) {
        MathPreconditions.checkPositiveInRangeClosed("split_num", splitNum, getNum());
        BitVector splitA = a.split(splitNum);
        BitVector spiltB = b.split(splitNum);
        BitVector splitC = c.split(splitNum);

        return create(splitA, spiltB, splitC);
    }

    @Override
    public void reduce(int reduceNum) {
        MathPreconditions.checkPositiveInRangeClosed("reduce_num", reduceNum, getNum());
        a.reduce(reduceNum);
        b.reduce(reduceNum);
        c.reduce(reduceNum);
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Z2Triple that = (Z2Triple) other;
        a.merge(that.a);
        b.merge(that.b);
        c.merge(that.c);
    }

    /**
     * Get the triple byte num.
     *
     * @return the triple byte num.
     */
    public int getByteNum() {
        return a.byteNum();
    }

    /**
     * Gets 'a'.
     *
     * @return 'a'.
     */
    public byte[] getA() {
        return a.getBytes();
    }

    /**
     * Gets 'a'.
     *
     * @return 'a'.
     */
    public BitVector getVectorA() {
        return a;
    }

    /**
     * Gets 'a' represented by String.
     *
     * @return 'a' represented by String.
     */
    public String getStringA() {
        return a.toString();
    }

    /**
     * Gets 'b'.
     *
     * @return 'b'.
     */
    public byte[] getB() {
        return b.getBytes();
    }

    /**
     * Gets 'b'.
     *
     * @return 'b'.
     */
    public BitVector getVectorB() {
        return b;
    }

    /**
     * Get 'b' represented by String.
     *
     * @return 'b' represented by String.
     */
    public String getStringB() {
        return b.toString();
    }

    /**
     * Get 'c'.
     *
     * @return 'c'.
     */
    public byte[] getC() {
        return c.getBytes();
    }

    /**
     * Get 'c'.
     *
     * @return 'c'.
     */
    public BitVector getVectorC() {
        return c;
    }

    /**
     * Get 'c' represented by String.
     *
     * @return 'c' represented by String.
     */
    public String getStringC() {
        return c.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(a)
            .append(b)
            .append(c)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Z2Triple that) {
            return new EqualsBuilder()
                .append(this.a, that.a)
                .append(this.b, that.b)
                .append(this.c, that.c)
                .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + a.toString() + ", " + b.toString() + ", " + c.toString() + "] (n = " + getNum() + ")";
    }
}
