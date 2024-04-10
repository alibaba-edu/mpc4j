package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;

/**
 * batch Boolean circuit receiver thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchUnaryZ2cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Z2cParty receiver;
    /**
     * operator
     */
    private final UnaryBcOperator operator;
    /**
     * x vectors
     */
    private final BitVector[] xVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * z (plain)
     */
    private BitVector[] recvPlainVectors;
    /**
     * z (secret)
     */
    private BitVector[] recvSecretVectors;

    BatchUnaryZ2cReceiverThread(Z2cParty receiver, UnaryBcOperator operator, BitVector[] xVectors) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVectors = xVectors;
        totalBitNum = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).sum();
    }

    BitVector[] getRecvPlainVectors() {
        return recvPlainVectors;
    }

    BitVector[] getRecvSecretVectors() {
        return recvSecretVectors;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalBitNum);
            // set inputs
            MpcZ2Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(each -> receiver.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            int[] bitNums = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).toArray();
            MpcZ2Vector[] x1SecretMpcVectors = receiver.shareOther(bitNums);
            MpcZ2Vector[] z1PlainMpcVectors, z1SecretMpcVectors;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NOT:
                    // (plain, plain)
                    z1PlainMpcVectors = receiver.not(xPlainMpcVectors);
                    receiver.revealOther(z1PlainMpcVectors);
                    recvSecretVectors = receiver.revealOwn(z1PlainMpcVectors);
                    // (plain, secret)
                    z1SecretMpcVectors = receiver.not(x1SecretMpcVectors);
                    receiver.revealOther(z1SecretMpcVectors);
                    recvPlainVectors = receiver.revealOwn(z1SecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
