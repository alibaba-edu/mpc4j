package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.MergedPcgPartyOutput;

import java.security.SecureRandom;

/**
 * Z2 triple.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/02/07
 */
public class Z2Triple implements MergedPcgPartyOutput {
    /**
     * triple num
     */
    private int num;
    /**
     * triple byte num
     */
    private int byteNum;
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
     * create a triple, each element is represented by bytes.
     *
     * @param num triple num.
     * @param a   'a' represented by bytes.
     * @param b   'b' represented by bytes.
     * @param c   'c' represented by bytes.
     * @return the created triple.
     */
    public static Z2Triple create(int num, byte[] a, byte[] b, byte[] c) {
        assert num > 0 : "num must be greater than 0: " + num;
        int byteNum = CommonUtils.getByteLength(num);
        Z2Triple triple = new Z2Triple();
        triple.num = num;
        triple.byteNum = byteNum;
        triple.a = BitVectorFactory.create(num, a);
        triple.b = BitVectorFactory.create(num, b);
        triple.c = BitVectorFactory.create(num, c);
        return triple;
    }

    /**
     * create a random triple.
     *
     * @param num triple num.
     * @return the created triple.
     */
    public static Z2Triple createRandom(int num, SecureRandom secureRandom) {
        assert num > 0 : "num must be greater than 0: " + num;
        int byteNum = CommonUtils.getByteLength(num);
        Z2Triple triple = new Z2Triple();
        triple.num = num;
        triple.byteNum = byteNum;
        triple.a = BitVectorFactory.createRandom(num, secureRandom);
        triple.b = BitVectorFactory.createRandom(num, secureRandom);
        triple.c = BitVectorFactory.createRandom(num, secureRandom);
        return triple;
    }

    /**
     * Create a triple with all elements are 1.
     *
     * @param num triple num.
     * @return the created triple.
     */
    static Z2Triple createOnes(int num) {
        assert num > 0 : "num must be greater than 0: " + num;
        int byteNum = CommonUtils.getByteLength(num);
        Z2Triple triple = new Z2Triple();
        triple.num = num;
        triple.byteNum = byteNum;
        triple.a = BitVectorFactory.createOnes(num);
        triple.b = BitVectorFactory.createOnes(num);
        triple.c = BitVectorFactory.createOnes(num);
        return triple;
    }

    /**
     * create an empty triple.
     *
     * @return the created triple.
     */
    public static Z2Triple createEmpty() {
        Z2Triple triple = new Z2Triple();
        triple.num = 0;
        triple.byteNum = 0;
        triple.a = BitVectorFactory.createEmpty();
        triple.b = BitVectorFactory.createEmpty();
        triple.c = BitVectorFactory.createEmpty();

        return triple;
    }

    /**
     * create a triple, each element is represented by BitVector.
     *
     * @param num triple num.
     * @param a   a represented by BitVector.
     * @param b   b represented by BitVector.
     * @param c   c represented by BitVector.
     * @return the created triple.
     */
    private static Z2Triple create(int num, BitVector a, BitVector b, BitVector c) {
        assert num > 0 : "num must be greater than 0: " + num;
        Z2Triple triple = new Z2Triple();
        triple.num = num;
        triple.byteNum = CommonUtils.getByteLength(num);
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
        return num;
    }

    @Override
    public Z2Triple split(int splitNum) {
        assert splitNum > 0 && splitNum <= num : "splitNum must be in range (0, " + num + "]: " + splitNum;
        BitVector splitA = a.split(splitNum);
        BitVector spiltB = b.split(splitNum);
        BitVector splitC = c.split(splitNum);
        num = num - splitNum;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);

        return Z2Triple.create(splitNum, splitA, spiltB, splitC);
    }

    @Override
    public void reduce(int reduceNum) {
        assert reduceNum > 0 && reduceNum <= num : "reduceNum must be in range (0, " + num + "]: " + reduceNum;
        if (reduceNum < num) {
            // if the reduced num is less than num, do split. If not, keep the current state.
            a.reduce(reduceNum);
            b.reduce(reduceNum);
            c.reduce(reduceNum);
            // update num
            num = reduceNum;
            byteNum = CommonUtils.getByteLength(reduceNum);
        }
    }

    @Override
    public void merge(MergedPcgPartyOutput other) {
        Z2Triple that = (Z2Triple) other;
        a.merge(that.a);
        b.merge(that.b);
        c.merge(that.c);
        // update num
        num += that.num;
        byteNum = num == 0 ? 0 : CommonUtils.getByteLength(num);
    }

    /**
     * Get the triple byte num.
     *
     * @return the triple byte num.
     */
    public int getByteNum() {
        return byteNum;
    }

    /**
     * Get 'a'.
     *
     * @return 'a'.
     */
    public byte[] getA() {
        return a.getBytes();
    }

    /**
     * Get 'a' represented by String.
     *
     * @return 'a' represented by String.
     */
    public String getStringA() {
        return a.toString();
    }

    /**
     * Get 'b'.
     *
     * @return 'b'.
     */
    public byte[] getB() {
        return b.getBytes();
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
     * Get 'c' represented by String.
     *
     * @return 'c' represented by String.
     */
    public String getStringC() {
        return c.toString();
    }

    @Override
    public String toString() {
        return "[" + a.toString() + ", " + b.toString() + ", " + c.toString() + "] (n = " + num + ")";
    }
}
