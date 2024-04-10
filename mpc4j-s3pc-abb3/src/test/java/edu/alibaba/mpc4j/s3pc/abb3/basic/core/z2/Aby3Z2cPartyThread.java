package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2;

import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.Aby3Z2cTest.BcOperator;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.replicate.TripletRpZ2Vector;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.Aby3Z2cTest.MUL_REQ_OP;

/**
 * aby3 z2 computation party thread
 *
 * @author Feng Han
 * @date 2024/02/01
 */
public class Aby3Z2cPartyThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aby3Z2cPartyThread.class);
    private final TripletZ2cParty z2cParty;
    private final int[] bitNums;
    private final BcOperator[] ops;
    private final HashMap<BcOperator, BitVector[][]> hashMap;

    Aby3Z2cPartyThread(TripletZ2cParty z2cParty, int[] bitNums, BcOperator[] ops) {
        this.z2cParty = z2cParty;
        this.bitNums = bitNums;
        this.ops = ops;
        hashMap = new HashMap<>();
    }

    public BitVector[][] getInputAndRes(BcOperator op) {
        Assert.assertTrue(hashMap.containsKey(op));
        return hashMap.get(op);
    }

    @Override
    public void run() {
        long mulOpNum = Arrays.stream(ops).filter(MUL_REQ_OP::contains).count();
        long totalNum = mulOpNum * (bitNums[0] + Arrays.stream(bitNums).sum() + (bitNums.length * 8L + 8));
        try {
            z2cParty.getTripletProvider().init(totalNum, 0);
            z2cParty.init();
            for(BcOperator op : ops){
                switch (op){
                    case OR:
                    case AND:
                    case ANDI:
                    case XOR:
                    case XORI:
                        testDyadicBcOp(op);
                        break;
                    case NOT:
                    case NOTI:
                        testUnaryBcOp(op);
                        break;
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

    private void testDyadicBcOp(BcOperator op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        // Test share & share, share & plain
        TripletRpZ2Vector[] leftMultiInput, rightMultiInput;
        TripletRpZ2Vector leftSingleInput, rightSingleInput;
        PlainZ2Vector[] rightPlainMultiInput;
        PlainZ2Vector rightPlainSingleInput;

        List<TripletZ2Vector> shareResult = new LinkedList<>();
        BitVector[][] inputAndRes = new BitVector[3][];
        if (z2cParty.getRpc().ownParty().getPartyId() == 0) {
            SecureRandom secureRandom = new SecureRandom();
            BitVector[] leftMulti = Arrays.stream(bitNums).mapToObj(num ->
                BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
            BitVector[] rightMulti = Arrays.stream(bitNums).mapToObj(num ->
                BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
            BitVector leftSingle = BitVectorFactory.createRandom(bitNums[0], secureRandom);
            BitVector rightSingle = BitVectorFactory.createRandom(bitNums[0], secureRandom);

            TripletRpZ2Vector[] rightMultiRandom = z2cParty.getTripletProvider().getCrProvider().randRpShareZ2Vector(bitNums);
            TripletRpZ2Vector rightSingleRandom = z2cParty.getTripletProvider().getCrProvider().randRpShareZ2Vector(new int[]{bitNums[0]})[0];
            BitVector[] rightMultiRandomPlain = z2cParty.open(rightMultiRandom);
            BitVector rightSingleRandomPlain = z2cParty.open(new TripletZ2Vector[]{rightSingleRandom})[0];
            rightPlainMultiInput = Arrays.stream(rightMultiRandomPlain).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
            rightPlainSingleInput = PlainZ2Vector.create(rightSingleRandomPlain);

            List<BitVector> leftInput = Arrays.stream(leftMulti).collect(Collectors.toCollection(LinkedList::new));
            leftInput.add(leftSingle);
            leftInput.addAll(Arrays.stream(leftMulti).collect(Collectors.toList()));
            leftInput.add(leftSingle);
            inputAndRes[0] = leftInput.toArray(new BitVector[0]);

            List<BitVector> rightInput = Arrays.stream(rightMulti).collect(Collectors.toCollection(LinkedList::new));
            rightInput.add(rightSingle);
            rightInput.addAll(Arrays.stream(rightMultiRandomPlain).collect(Collectors.toList()));
            rightInput.add(rightSingleRandomPlain);
            inputAndRes[1] = rightInput.toArray(new BitVector[0]);

            leftMultiInput = (TripletRpZ2Vector[]) z2cParty.shareOwn(leftMulti);
            rightMultiInput = (TripletRpZ2Vector[]) z2cParty.shareOwn(rightMulti);
            leftSingleInput = (TripletRpZ2Vector) z2cParty.shareOwn(new BitVector[]{leftSingle})[0];
            rightSingleInput = (TripletRpZ2Vector) z2cParty.shareOwn(new BitVector[]{rightSingle})[0];
        }else{
            TripletRpZ2Vector[] rightMultiRandom = z2cParty.getTripletProvider().getCrProvider().randRpShareZ2Vector(bitNums);
            TripletRpZ2Vector rightSingleRandom = z2cParty.getTripletProvider().getCrProvider().randRpShareZ2Vector(new int[]{bitNums[0]})[0];
            BitVector[] rightMultiRandomPlain = z2cParty.open(rightMultiRandom);
            BitVector rightSingleRandomPlain = z2cParty.open(new TripletZ2Vector[]{rightSingleRandom})[0];
            rightPlainMultiInput = Arrays.stream(rightMultiRandomPlain).map(PlainZ2Vector::create).toArray(PlainZ2Vector[]::new);
            rightPlainSingleInput = PlainZ2Vector.create(rightSingleRandomPlain);

            leftMultiInput = (TripletRpZ2Vector[]) z2cParty.shareOther(bitNums, z2cParty.getRpc().getParty(0));
            rightMultiInput = (TripletRpZ2Vector[]) z2cParty.shareOther(bitNums, z2cParty.getRpc().getParty(0));
            leftSingleInput = (TripletRpZ2Vector) z2cParty.shareOther(new int[]{bitNums[0]}, z2cParty.getRpc().getParty(0))[0];
            rightSingleInput = (TripletRpZ2Vector) z2cParty.shareOther(new int[]{bitNums[0]}, z2cParty.getRpc().getParty(0))[0];
        }
        switch (op) {
            case XOR: {
                shareResult.addAll(Arrays.stream(z2cParty.xor(leftMultiInput, rightMultiInput)).collect(Collectors.toList()));
                shareResult.add(z2cParty.xor(leftSingleInput, rightSingleInput));
                shareResult.addAll(Arrays.stream(z2cParty.xor(leftMultiInput, rightPlainMultiInput)).collect(Collectors.toList()));
                shareResult.add(z2cParty.xor(leftSingleInput, rightPlainSingleInput));
                break;
            }
            case XORI: {
                TripletRpZ2Vector[] leftMultiCopy = Arrays.stream(leftMultiInput).map(TripletRpZ2Vector::copy).toArray(TripletRpZ2Vector[]::new);
                TripletRpZ2Vector leftSingleCopy = leftSingleInput.copy();
                z2cParty.xori(leftMultiInput, rightMultiInput);
                z2cParty.xori(leftSingleInput, rightSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiInput).collect(Collectors.toList()));
                shareResult.add(leftSingleInput);
                z2cParty.xori(leftMultiCopy, rightPlainMultiInput);
                z2cParty.xori(leftSingleCopy, rightPlainSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiCopy).collect(Collectors.toList()));
                shareResult.add(leftSingleCopy);
                break;
            }
            case AND: {
                shareResult.addAll(Arrays.stream(z2cParty.and(leftMultiInput, rightMultiInput)).collect(Collectors.toList()));
                shareResult.add(z2cParty.and(leftSingleInput, rightSingleInput));
                shareResult.addAll(Arrays.stream(z2cParty.and(leftMultiInput, rightPlainMultiInput)).collect(Collectors.toList()));
                shareResult.add(z2cParty.and(leftSingleInput, rightPlainSingleInput));
                break;
            }
            case ANDI: {
                TripletRpZ2Vector[] leftMultiCopy = Arrays.stream(leftMultiInput).map(TripletRpZ2Vector::copy).toArray(TripletRpZ2Vector[]::new);
                TripletRpZ2Vector leftSingleCopy = leftSingleInput.copy();
                z2cParty.andi(leftMultiInput, rightMultiInput);
                z2cParty.andi(leftSingleInput, rightSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiInput).collect(Collectors.toList()));
                shareResult.add(leftSingleInput);
                z2cParty.andi(leftMultiCopy, rightPlainMultiInput);
                z2cParty.andi(leftSingleCopy, rightPlainSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiCopy).collect(Collectors.toList()));
                shareResult.add(leftSingleCopy);
                break;
            }
            case OR: {
                shareResult.addAll(Arrays.stream(z2cParty.or(leftMultiInput, rightMultiInput)).collect(Collectors.toList()));
                shareResult.add(z2cParty.or(leftSingleInput, rightSingleInput));
                shareResult.addAll(Arrays.stream(z2cParty.or(leftMultiInput, rightPlainMultiInput)).collect(Collectors.toList()));
                shareResult.add(z2cParty.or(leftSingleInput, rightPlainSingleInput));
                break;
            }
        }
        if(z2cParty.getRpc().ownParty().getPartyId() == 0){
            inputAndRes[2] = z2cParty.revealOwn(shareResult.toArray(new TripletZ2Vector[0]));
            hashMap.put(op, inputAndRes);
        }else{
            z2cParty.revealOther(shareResult.toArray(new TripletZ2Vector[0]), z2cParty.getRpc().getParty(0));
        }
    }

    private void testUnaryBcOp(BcOperator op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        TripletRpZ2Vector[] multiInput;
        TripletRpZ2Vector singleInput;
        BitVector[][] inputAndRes = new BitVector[2][];
        List<TripletZ2Vector> shareResult = new LinkedList<>();
        if (z2cParty.getRpc().ownParty().getPartyId() == 0) {
            SecureRandom secureRandom = new SecureRandom();
            BitVector[] multi = Arrays.stream(bitNums).mapToObj(num ->
                BitVectorFactory.createRandom(num, secureRandom)).toArray(BitVector[]::new);
            BitVector single = BitVectorFactory.createRandom(bitNums[0], secureRandom);

            List<BitVector> leftInput = Arrays.stream(multi).collect(Collectors.toCollection(LinkedList::new));
            leftInput.add(single);
            inputAndRes[0] = leftInput.toArray(new BitVector[0]);

            multiInput = (TripletRpZ2Vector[]) z2cParty.shareOwn(multi);
            singleInput = (TripletRpZ2Vector) z2cParty.shareOwn(new BitVector[]{single})[0];
        }else{
            multiInput = (TripletRpZ2Vector[]) z2cParty.shareOther(bitNums, z2cParty.getRpc().getParty(0));
            singleInput = (TripletRpZ2Vector) z2cParty.shareOther(new int[]{bitNums[0]}, z2cParty.getRpc().getParty(0))[0];
        }
        switch (op) {
            case NOT: {
                shareResult.addAll(Arrays.stream(z2cParty.not(multiInput)).map(each -> (TripletRpZ2Vector) each).collect(Collectors.toList()));
                shareResult.add((TripletRpZ2Vector) z2cParty.not(singleInput));
                break;
            }
            case NOTI: {
                z2cParty.noti(multiInput);
                z2cParty.noti(singleInput);
                shareResult.addAll(Arrays.stream(multiInput).collect(Collectors.toList()));
                shareResult.add(singleInput);
                break;
            }
        }
        if(z2cParty.getRpc().ownParty().getPartyId() == 0){
            inputAndRes[1] = z2cParty.revealOwn(shareResult.toArray(new TripletZ2Vector[0]));
            hashMap.put(op, inputAndRes);
        }else{
            z2cParty.revealOther(shareResult.toArray(new TripletZ2Vector[0]), z2cParty.getRpc().getParty(0));
        }
    }
}
