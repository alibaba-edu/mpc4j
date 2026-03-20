package edu.alibaba.mpc4j.work.db.sketch.utils.hll;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.work.db.sketch.utils.LowMcCircuit;
import edu.alibaba.mpc4j.work.db.sketch.utils.Utils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParamUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Implementation of HyperLogLog (HLL) cardinality estimation.
 * Uses leading zero counting on hash values to estimate distinct element count.
 */
public class HLLImpl implements HLL {
    private final int size;
    private final int[] data;
    private final BigInteger[] buffer;
    private final BigInteger indexMask;
    private final BigInteger hashMask;
    private final int hashLength;
    private final int logSketchSize;
    private int bufferSize;
    private final BigInteger[] hashPara;
    private final double constant = 0.79;
    private PlainZ2Vector key;
    private final PlainZ2cParty party;
    private final LowMcCircuit circuit;
    private int hashBitLen;

    /**
     * Constructs an HLL implementation with default hashing
     * @param size sketch size (must be power of 2)
     */
    public HLLImpl(int size) {
        assert (size > 0 && (size & (size - 1)) == 0) : "size must be power of 2";
        this.size = size;
        data = new int[size];
        buffer = new BigInteger[size];
        bufferSize = 0;
        this.hashLength = 20;
        this.logSketchSize = LongUtils.ceilLog2(size);
        this.party = new PlainZ2cParty();
        LowMcParam lowMcParam = LowMcParamUtils.getParam(64, 23131, 40);
        this.circuit = new LowMcCircuit(party, null, lowMcParam);
        this.hashPara = genHashParameter(logSketchSize + hashLength);
        this.indexMask = BigInteger.valueOf((1L << logSketchSize) - 1);
        this.hashMask = BigInteger.valueOf((1L << hashLength) - 1);
    }
    
    /**
     * Constructs an HLL implementation with LowMc circuit hashing
     * @param size sketch size (must be power of 2)
     * @param key hash key for LowMc circuit
     * @param hashBitLength bit length of hash output
     */
    public HLLImpl(int size, PlainZ2Vector key, int hashBitLength) {
        this(size);
        this.setKey(key);
        this.hashBitLen = hashBitLength;
    }
    /**
     * Simple hash function for testing
     * @param element element to hash
     * @return hash value
     */
    private BigInteger hash(BigInteger element) {
        int value_1 = Math.abs(new Random(element.intValue()).nextInt());
        int value_2 = Math.abs(new Random(element.intValue() + 1).nextInt());
        return (BigInteger.valueOf((long) value_1 * value_2));
    }
    
    /**
     * Sets the LowMc circuit key for cryptographic hashing
     * @param key hash key
     */
    private void setKey(PlainZ2Vector key) {
        this.key = key;
        try {
            circuit.init(this.key);
        } catch (MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void input(BigInteger... elements) {
        for (BigInteger element : elements) {
            input(element);
        }
    }
    
    /**
     * Inserts a single element into the buffer
     * @param element element to insert
     */
    @Override
    public void input(BigInteger element) {
        this.buffer[bufferSize] = element;
        bufferSize++;
        if (bufferSize == size) {
            merge();
        }
    }
    
    /**
     * Counts leading zeros in a BigInteger value
     * @param value value to analyze
     * @return number of leading zeros
     */
    private int getLeadingZeros(BigInteger value) {
        int num = value.getLowestSetBit();
        if (num == -1) {
            num = hashBitLen;
        }
        return num;
    }
    
    /**
     * Counts leading zeros for multiple Z2 vectors
     * @param values array of Z2 vectors
     * @return array of leading zero counts
     */
    private int[] getLeadingZeros(PlainZ2Vector[] values) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            values[i].reverseBits();
            result[i] = getLeadingZeros(values[i].getBitVector().getBigInteger());
        }
        return result;
    }
    /**
     * Merges buffered elements into the sketch
     */
    /**
     * Merges buffered elements into the sketch
     */
    private void merge() {
        if (this.key != null) {
            PlainZ2Vector[] plainBuffer;
            BitVector[] bitVectors = new BitVector[bufferSize];
            for (int i = 0; i < bufferSize; i++) {
                bitVectors[i] = BitVectorFactory.create(32, buffer[i]);
            }
            plainBuffer = party.setPublicValues(bitVectors);
            plainBuffer = Utils.matrixTranspose(party, plainBuffer);
            MpcZ2Vector[] hashBuffer;
            try {
                hashBuffer = circuit.enc(plainBuffer);
            } catch (MpcAbortException e) {
                throw new RuntimeException(e);
            }
            if (hashBuffer != null) {
                hashBuffer = Utils.matrixTranspose(party, hashBuffer);
                Arrays.stream(hashBuffer).forEach(ele -> ele.split(ele.bitNum() - logSketchSize - hashBitLen));
                PlainZ2Vector[] hashPart = Arrays.stream((PlainZ2Vector[]) hashBuffer).map(
                        ele -> ele.split(hashBitLen)
                ).toArray(PlainZ2Vector[]::new);
                int[] leadingZeroNum = getLeadingZeros(hashPart);
                MpcZ2Vector[] finalHashBuffer = hashBuffer;
                IntStream.range(0, hashBuffer.length).forEach(i -> {
                    int index = finalHashBuffer[i].getBitVector().getBigInteger().intValueExact();
                    data[index] = Math.max(data[index], leadingZeroNum[i]);
                });
            }
        }
        else {
            for (int i = 0; i < bufferSize; i++) {
                BigInteger element = buffer[i];
                BigInteger hashValue = hash(element);
                int index = hashValue.shiftRight(hashLength).and(indexMask).intValue();
                int zeroNum = getLeadingZeros(hashValue.and(hashMask));
                data[index] = Math.max(data[index], zeroNum);
            }
        }
        bufferSize = 0;
    }

    /**
     * Queries the estimated cardinality
     * Uses HLL formula: E = a * S * 2^(sum/S) where a=0.79, S=size
     * @return estimated number of distinct elements
     */
    @Override
    public double query() {
        if (bufferSize != 0) {
            merge();
        }
        int sum = Arrays.stream(this.data).reduce(0, Integer::sum);
        return constant * size * Math.pow(2, (double) sum / size);
    }

    /**
     * Gets the internal sketch table
     * @return sketch table
     */
    public int[] getTable(){
        return this.data;
    }

    /**
     * Generates hash parameters
     * @param elementBitLen bit length of elements
     * @return hash parameters
     */
    private BigInteger[] genHashParameter(int elementBitLen) {
        MathPreconditions.checkPositiveInRangeClosed("0 < elementBitLen <= 64", elementBitLen, 64);
        return IntStream.range(0, 2).mapToObj(i ->
                BitVectorFactory.createRandom(elementBitLen, new Random()).getBigInteger()).toArray(BigInteger[]::new);
    }
}
