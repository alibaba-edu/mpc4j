package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;

/**
 * Abstract Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public abstract class AbstractBcParty extends AbstractSecureTwoPartyPto implements BcParty {
    /**
     * protocol configuration
     */
    private final BcConfig config;
    /**
     * maximum number of bits in round.
     */
    protected int maxRoundBitNum;
    /**
     * total number of bits for updates.
     */
    protected long maxUpdateBitNum;
    /**
     * current number of bits.
     */
    protected int bitNum;
    /**
     * the number of input bits
     */
    protected long inputBitNum;
    /**
     * the number of AND gates.
     */
    protected long andGateNum;
    /**
     * the number of XOR gates.
     */
    protected long xorGateNum;
    /**
     * the number of output bits
     */
    protected long outputBitNum;

    public AbstractBcParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, BcConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        andGateNum = 0;
        xorGateNum = 0;
    }

    @Override
    public BcFactory.BcType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxRoundBitNum, int updateBitNum) {
        assert maxRoundBitNum > 0 && maxRoundBitNum <= config.maxBaseNum()
            : "maxRoundBitNum must be in range (0, " + config.maxBaseNum() + "]";
        this.maxRoundBitNum = maxRoundBitNum;
        assert updateBitNum >= maxRoundBitNum : "updateBitNum must be greater or equal to maxRoundBitNum";
        this.maxUpdateBitNum = updateBitNum;
        initialized = false;
    }

    protected void setShareOwnInput(BitVector bitVector) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert bitVector.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + bitVector.bitNum();
        bitNum = bitVector.bitNum();
        inputBitNum += bitNum;
    }

    protected void setShareOtherInput(int bitNum) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert bitNum <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + bitNum;
        this.bitNum = bitNum;
        inputBitNum += bitNum;
    }

    protected void setAndInput(SquareSbitVector xi, SquareSbitVector yi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() == yi.bitNum()
            : "two BitVector must have the same number of bits (" + xi.bitNum() + " : " + yi.bitNum() + ")";
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of AND gates is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    protected void setXorInput(SquareSbitVector xi, SquareSbitVector yi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() == yi.bitNum()
            : "two BitVector must have the same number of bits (" + xi.bitNum() + " : " + yi.bitNum() + ")";
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of XOR gates is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    protected void setRevealOwnInput(SquareSbitVector xi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of output bits is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    protected void setRevealOtherInput(SquareSbitVector xi) {
        if (!initialized) {
            throw new IllegalStateException("Need init...");
        }
        assert xi.bitNum() <= maxRoundBitNum
            : "the number of bits must be less than or equal to " + maxRoundBitNum + ": " + xi.bitNum();
        // the number of output bits is added during the protocol execution.
        bitNum = xi.bitNum();
    }

    @Override
    public long inputBitNum(boolean reset) {
        long result = inputBitNum;
        inputBitNum = reset ? 0L : inputBitNum;
        return result;
    }

    @Override
    public long andGateNum(boolean reset) {
        long result = andGateNum;
        andGateNum = reset ? 0L : andGateNum;
        return result;
    }

    @Override
    public long xorGateNum(boolean reset) {
        long result = xorGateNum;
        xorGateNum = reset ? 0L : xorGateNum;
        return result;
    }
    @Override
    public long outputBitNum(boolean reset) {
        long result = outputBitNum;
        outputBitNum = reset ? 0L : outputBitNum;
        return result;
    }

    @Override
    public SquareSbitVector[] shareOwn(BitVector[] xArray) {
        if (xArray.length == 0) {
            return new SquareSbitVector[0];
        }
        // merge
        BitVector mergeX = mergeBitVectors(xArray);
        // share
        SquareSbitVector mergeShareXi = shareOwn(mergeX);
        // split
        int[] lengths = Arrays.stream(xArray).mapToInt(BitVector::bitNum).toArray();
        return splitSbitVector(mergeShareXi, lengths);
    }

    @Override
    public SquareSbitVector[] shareOther(int[] bitNums) throws MpcAbortException {
        if (bitNums.length == 0) {
            return new SquareSbitVector[0];
        }
        // share
        int bitNum = Arrays.stream(bitNums).sum();
        SquareSbitVector mergeShareXi = shareOther(bitNum);
        // split
        return splitSbitVector(mergeShareXi, bitNums);
    }

    @Override
    public SquareSbitVector[] and(SquareSbitVector[] xiArray, SquareSbitVector[] yiArray) throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareSbitVector[0];
        }
        // merge xi and yi
        SquareSbitVector mergeXiArray = mergeSbitVectors(xiArray);
        SquareSbitVector mergeYiArray = mergeSbitVectors(yiArray);
        // and operation
        SquareSbitVector mergeZiArray = and(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(SquareSbitVector::bitNum).toArray();
        return splitSbitVector(mergeZiArray, lengths);
    }

    @Override
    public SquareSbitVector[] xor(SquareSbitVector[] xiArray, SquareSbitVector[] yiArray) throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareSbitVector[0];
        }
        // merge xi and yi
        SquareSbitVector mergeXiArray = mergeSbitVectors(xiArray);
        SquareSbitVector mergeYiArray = mergeSbitVectors(yiArray);
        // xor operation
        SquareSbitVector mergeZiArray = xor(mergeXiArray, mergeYiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(SquareSbitVector::bitNum).toArray();
        return splitSbitVector(mergeZiArray, lengths);
    }

    @Override
    public BitVector[] revealOwn(SquareSbitVector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new BitVector[0];
        }
        // merge
        SquareSbitVector mergeXiArray = mergeSbitVectors(xiArray);
        // reveal
        BitVector mergeX = revealOwn(mergeXiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(SbitVector::bitNum).toArray();
        return splitBitVector(mergeX, lengths);
    }

    @Override
    public void revealOther(SquareSbitVector[] xiArray) {
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareSbitVector mergeXiArray = mergeSbitVectors(xiArray);
        // reveal
        revealOther(mergeXiArray);
    }

    private BitVector mergeBitVectors(BitVector[] bitVectors) {
        assert bitVectors.length > 0 : "merged vector length must be greater than 0";
        BitVector mergeBitVector = BitVectorFactory.createEmpty();
        for (BitVector bitVector : bitVectors) {
            assert bitVector.bitNum() > 0 : "the number of bits must be greater than 0";
            mergeBitVector.merge(bitVector);
        }
        return mergeBitVector;
    }

    private SquareSbitVector mergeSbitVectors(SquareSbitVector[] sbitVectors) {
        assert sbitVectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = sbitVectors[0].isPlain();
        SquareSbitVector mergeSbitVector = SquareSbitVector.createEmpty(plain);
        // we must merge the bit vector in the reverse order
        for (SquareSbitVector sbitVector : sbitVectors) {
            assert sbitVector.bitNum() > 0 : "the number of bits must be greater than 0";
            mergeSbitVector.merge(sbitVector);
        }
        return mergeSbitVector;
    }

    private BitVector[] splitBitVector(BitVector mergeBitVector, int[] lengths) {
        BitVector[] bitVectors = new BitVector[lengths.length];
        for (int index = 0; index < lengths.length; index++) {
            bitVectors[index] = mergeBitVector.split(lengths[index]);
        }
        assert mergeBitVector.bitNum() == 0 : "merged vector must remain 0 bits: " + mergeBitVector.bitNum();
        return bitVectors;
    }

    private SquareSbitVector[] splitSbitVector(SquareSbitVector mergeSbitVector, int[] lengths) {
        SquareSbitVector[] sbitVectors = new SquareSbitVector[lengths.length];
        for (int index = 0; index < lengths.length; index++) {
            sbitVectors[index] = mergeSbitVector.split(lengths[index]);
        }
        assert mergeSbitVector.bitNum() == 0 : "merged vector must remain 0 bits: " + mergeSbitVector.bitNum();
        return sbitVectors;
    }
}
