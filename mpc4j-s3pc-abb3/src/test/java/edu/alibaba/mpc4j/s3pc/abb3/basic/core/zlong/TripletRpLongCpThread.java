package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong;

import edu.alibaba.mpc4j.common.circuit.zlong.MpcLongVector;
import edu.alibaba.mpc4j.common.circuit.zlong.PlainLongVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletRpLongCpTest.AcOperator;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletRpLongCpTest.MUL_REQ_OP;

/**
 * aby3 zlong computation party thread
 *
 * @author Feng Han
 * @date 2024/02/01
 */
public class TripletRpLongCpThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripletRpLongCpThread.class);
    private final TripletLongParty z64cParty;
    private final int[] dataNums;
    private final AcOperator[] ops;
    private final HashMap<AcOperator, LongVector[][]> hashMap;

    TripletRpLongCpThread(TripletLongParty z64cParty, int[] dataNums, AcOperator[] ops) {
        this.z64cParty = z64cParty;
        this.dataNums = dataNums;
        this.ops = ops;
        hashMap = new HashMap<>();
    }

    public LongVector[][] getInputAndRes(AcOperator op) {
        Assert.assertTrue(hashMap.containsKey(op));
        return hashMap.get(op);
    }

    @Override
    public void run() {
        long mulOpNum = Arrays.stream(ops).filter(MUL_REQ_OP::contains).count();
        long totalNum = mulOpNum * (dataNums[0] + Arrays.stream(dataNums).sum());
        try {
            z64cParty.getTripletProvider().init(0, totalNum);
            z64cParty.init();
            for(AcOperator op : ops){
                switch (op){
                    case ADD:
                    case ADDI:
                    case SUB:
                    case SUBI:
                    case MUL:
                    case MULI:
                        testDyadicAcOp(op);
                        break;
                    case NEG:
                    case NEGI:
                        testUnaryAcOp(op);
                        break;
                }
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }

    private void testDyadicAcOp(AcOperator op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        // Test share & share, share & plain
        TripletLongVector[] leftMultiInput, rightMultiInput;
        TripletLongVector leftSingleInput, rightSingleInput;
        PlainLongVector[] rightPlainMultiInput;
        PlainLongVector rightPlainSingleInput;

        List<MpcLongVector> shareResult = new LinkedList<>();
        LongVector[][] inputAndRes = new LongVector[3][];
        if (z64cParty.getRpc().ownParty().getPartyId() == 0) {
            SecureRandom secureRandom = new SecureRandom();
            LongVector[] leftMulti = Arrays.stream(dataNums).mapToObj(num ->
                LongVector.createRandom(num, secureRandom)).toArray(LongVector[]::new);
            LongVector[] rightMulti = Arrays.stream(dataNums).mapToObj(num ->
                LongVector.createRandom(num, secureRandom)).toArray(LongVector[]::new);
            LongVector leftSingle = LongVector.createRandom(dataNums[0], secureRandom);
            LongVector rightSingle = LongVector.createRandom(dataNums[0], secureRandom);

            TripletLongVector[] rightMultiRandom = z64cParty.getTripletProvider().getCrProvider().randRpShareZl64Vector(dataNums);
            TripletLongVector rightSingleRandom = z64cParty.getTripletProvider().getCrProvider().randRpShareZl64Vector(new int[]{dataNums[0]})[0];
            LongVector[] rightMultiRandomPlain = z64cParty.open(rightMultiRandom);
            LongVector rightSingleRandomPlain = z64cParty.open(new TripletLongVector[]{rightSingleRandom})[0];
            rightPlainMultiInput = Arrays.stream(rightMultiRandomPlain).map(PlainLongVector::create).toArray(PlainLongVector[]::new);
            rightPlainSingleInput = PlainLongVector.create(rightSingleRandomPlain);

            List<LongVector> leftInput = Arrays.stream(leftMulti).collect(Collectors.toCollection(LinkedList::new));
            leftInput.add(leftSingle);
            leftInput.addAll(Arrays.stream(leftMulti).collect(Collectors.toList()));
            leftInput.add(leftSingle);
            inputAndRes[0] = leftInput.toArray(new LongVector[0]);

            List<LongVector> rightInput = Arrays.stream(rightMulti).collect(Collectors.toCollection(LinkedList::new));
            rightInput.add(rightSingle);
            rightInput.addAll(Arrays.stream(rightMultiRandomPlain).collect(Collectors.toList()));
            rightInput.add(rightSingleRandomPlain);
            inputAndRes[1] = rightInput.toArray(new LongVector[0]);

            leftMultiInput = (TripletLongVector[]) z64cParty.shareOwn(leftMulti);
            rightMultiInput = (TripletLongVector[]) z64cParty.shareOwn(rightMulti);
            leftSingleInput = (TripletLongVector) z64cParty.shareOwn(new LongVector[]{leftSingle})[0];
            rightSingleInput = (TripletLongVector) z64cParty.shareOwn(new LongVector[]{rightSingle})[0];
        }else{
            TripletLongVector[] rightMultiRandom = z64cParty.getTripletProvider().getCrProvider().randRpShareZl64Vector(dataNums);
            TripletLongVector rightSingleRandom = z64cParty.getTripletProvider().getCrProvider().randRpShareZl64Vector(new int[]{dataNums[0]})[0];
            LongVector[] rightMultiRandomPlain = z64cParty.open(rightMultiRandom);
            LongVector rightSingleRandomPlain = z64cParty.open(new TripletLongVector[]{rightSingleRandom})[0];
            rightPlainMultiInput = Arrays.stream(rightMultiRandomPlain).map(PlainLongVector::create).toArray(PlainLongVector[]::new);
            rightPlainSingleInput = PlainLongVector.create(rightSingleRandomPlain);

            leftMultiInput = (TripletLongVector[]) z64cParty.shareOther(dataNums, z64cParty.getRpc().getParty(0));
            rightMultiInput = (TripletLongVector[]) z64cParty.shareOther(dataNums, z64cParty.getRpc().getParty(0));
            leftSingleInput = (TripletLongVector) z64cParty.shareOther(new int[]{dataNums[0]}, z64cParty.getRpc().getParty(0))[0];
            rightSingleInput = (TripletLongVector) z64cParty.shareOther(new int[]{dataNums[0]}, z64cParty.getRpc().getParty(0))[0];
        }
        switch (op) {
            case ADD: {
                shareResult.addAll(Arrays.stream(z64cParty.add(leftMultiInput, rightMultiInput)).collect(Collectors.toList()));
                shareResult.add(z64cParty.add(leftSingleInput, rightSingleInput));
                shareResult.addAll(Arrays.stream(z64cParty.add(leftMultiInput, rightPlainMultiInput)).collect(Collectors.toList()));
                shareResult.add(z64cParty.add(leftSingleInput, rightPlainSingleInput));
                break;
            }
            case ADDI: {
                TripletLongVector[] leftMultiCopy = Arrays.stream(leftMultiInput).map(TripletLongVector::copy).toArray(TripletLongVector[]::new);
                TripletLongVector leftSingleCopy = (TripletLongVector) leftSingleInput.copy();
                z64cParty.addi(leftMultiInput, rightMultiInput);
                z64cParty.addi(leftSingleInput, rightSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiInput).collect(Collectors.toList()));
                shareResult.add(leftSingleInput);
                z64cParty.addi(leftMultiCopy, rightPlainMultiInput);
                z64cParty.addi(leftSingleCopy, rightPlainSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiCopy).collect(Collectors.toList()));
                shareResult.add(leftSingleCopy);
                break;
            }
            case SUB: {
                shareResult.addAll(Arrays.stream(z64cParty.sub(leftMultiInput, rightMultiInput)).collect(Collectors.toList()));
                shareResult.add(z64cParty.sub(leftSingleInput, rightSingleInput));
                shareResult.addAll(Arrays.stream(z64cParty.sub(leftMultiInput, rightPlainMultiInput)).collect(Collectors.toList()));
                shareResult.add(z64cParty.sub(leftSingleInput, rightPlainSingleInput));
                break;
            }
            case SUBI: {
                TripletLongVector[] leftMultiCopy = Arrays.stream(leftMultiInput).map(TripletLongVector::copy).toArray(TripletLongVector[]::new);
                TripletLongVector leftSingleCopy = (TripletLongVector) leftSingleInput.copy();
                z64cParty.subi(leftMultiInput, rightMultiInput);
                z64cParty.subi(leftSingleInput, rightSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiInput).collect(Collectors.toList()));
                shareResult.add(leftSingleInput);
                z64cParty.subi(leftMultiCopy, rightPlainMultiInput);
                z64cParty.subi(leftSingleCopy, rightPlainSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiCopy).collect(Collectors.toList()));
                shareResult.add(leftSingleCopy);
                break;
            }
            case MUL: {
                shareResult.addAll(Arrays.stream(z64cParty.mul(leftMultiInput, rightMultiInput)).collect(Collectors.toList()));
                shareResult.add(z64cParty.mul(leftSingleInput, rightSingleInput));
                shareResult.addAll(Arrays.stream(z64cParty.mul(leftMultiInput, rightPlainMultiInput)).collect(Collectors.toList()));
                shareResult.add(z64cParty.mul(leftSingleInput, rightPlainSingleInput));
                break;
            }
            case MULI:{
                z64cParty.muli(leftMultiInput, rightPlainMultiInput);
                z64cParty.muli(leftSingleInput, rightPlainSingleInput);
                shareResult.addAll(Arrays.stream(leftMultiInput).collect(Collectors.toList()));
                shareResult.add(leftSingleInput);
                break;
            }
        }
        if(z64cParty.getRpc().ownParty().getPartyId() == 0){
//            inputAndRes[2] = z64cParty.open(shareResult.toArray(new MpcLongVector[0]));
            inputAndRes[2] = z64cParty.revealOwn(shareResult.toArray(new MpcLongVector[0]));
            if(op.equals(AcOperator.MULI)){
                IntStream.range(0, 2).forEach(i -> inputAndRes[i] = Arrays.copyOfRange(inputAndRes[i], inputAndRes[i].length / 2, inputAndRes[2].length));
            }
            hashMap.put(op, inputAndRes);
        }else{
//            inputAndRes[2] = z64cParty.open(shareResult.toArray(new MpcLongVector[0]));
            z64cParty.revealOther(z64cParty.getRpc().getParty(0), shareResult.toArray(new MpcLongVector[0]));
        }
    }

    private void testUnaryAcOp(AcOperator op) throws MpcAbortException {
        LOGGER.info("testing {}", op.toString());
        TripletLongVector[] multiInput;
        TripletLongVector singleInput;
        LongVector[][] inputAndRes = new LongVector[2][];
        List<MpcLongVector> shareResult = new LinkedList<>();
        if (z64cParty.getRpc().ownParty().getPartyId() == 0) {
            SecureRandom secureRandom = new SecureRandom();
            LongVector[] multi = Arrays.stream(dataNums).mapToObj(num ->
                LongVector.createRandom(num, secureRandom)).toArray(LongVector[]::new);
            LongVector single = LongVector.createRandom(dataNums[0], secureRandom);

            List<LongVector> leftInput = Arrays.stream(multi).collect(Collectors.toList());
            leftInput.add(single);
            inputAndRes[0] = leftInput.toArray(new LongVector[0]);

            multiInput = (TripletLongVector[]) z64cParty.shareOwn(multi);
            singleInput = (TripletLongVector) z64cParty.shareOwn(new LongVector[]{single})[0];
        }else{
            multiInput = (TripletLongVector[]) z64cParty.shareOther(dataNums, z64cParty.getRpc().getParty(0));
            singleInput = (TripletLongVector) z64cParty.shareOther(new int[]{dataNums[0]}, z64cParty.getRpc().getParty(0))[0];
        }
        switch (op) {
            case NEG: {
                shareResult.addAll(Arrays.stream(z64cParty.neg(multiInput)).map(each -> (TripletLongVector) each).collect(Collectors.toList()));
                shareResult.add(z64cParty.neg(singleInput));
                break;
            }
            case NEGI: {
                z64cParty.negi(multiInput);
                z64cParty.negi(singleInput);
                shareResult.addAll(Arrays.stream(multiInput).collect(Collectors.toList()));
                shareResult.add(singleInput);
                break;
            }
        }
        if(z64cParty.getRpc().ownParty().getPartyId() == 0){
            inputAndRes[1] = z64cParty.revealOwn(shareResult.toArray(new MpcLongVector[0]));
            hashMap.put(op, inputAndRes);
        }else{
            z64cParty.revealOther(z64cParty.getRpc().getParty(0), shareResult.toArray(new MpcLongVector[0]));
        }
    }
}
