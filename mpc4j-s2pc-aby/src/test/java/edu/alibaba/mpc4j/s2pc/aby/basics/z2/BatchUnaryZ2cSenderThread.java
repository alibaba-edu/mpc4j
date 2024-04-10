package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;

/**
 * batch Boolean circuit sender thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchUnaryZ2cSenderThread extends Thread {
    /**
     * sender
     */
    private final Z2cParty sender;
    /**
     * operator
     */
    private final UnaryBcOperator operator;
    /**
     * x vectors
     */
    private final BitVector[] xVectors;
    /**
     * z vectors
     */
    private final BitVector[] zVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * zs (plain)
     */
    private BitVector[] sendPlainVectors;
    /**
     * zs (secret)
     */
    private BitVector[] sendSecretVectors;

    BatchUnaryZ2cSenderThread(Z2cParty sender, UnaryBcOperator operator, BitVector[] xVectors) {
        this.sender = sender;
        this.operator = operator;
        this.xVectors = xVectors;
        totalBitNum = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).sum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NOT:
                zVectors = Arrays.stream(xVectors).map(BitVector::not).toArray(BitVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryBcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    BitVector[] getExpectVectors() {
        return zVectors;
    }

    BitVector[] getSendPlainVectors() {
        return sendPlainVectors;
    }

    BitVector[] getSendSecretVectors() {
        return sendSecretVectors;
    }

    @Override
    public void run() {
        try {
            sender.init(totalBitNum);
            // set inputs
            MpcZ2Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(each -> sender.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] x0SecretMpcVectors = sender.shareOwn(xVectors);
            MpcZ2Vector[] z0PlainMpcVectors, z0SecretMpcVectors;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NOT:
                    // (plain, plain)
                    z0PlainMpcVectors = sender.not(xPlainMpcVectors);
                    sendSecretVectors = sender.revealOwn(z0PlainMpcVectors);
                    sender.revealOther(z0PlainMpcVectors);
                    // (plain, secret)
                    z0SecretMpcVectors = sender.not(x0SecretMpcVectors);
                    sendPlainVectors = sender.revealOwn(z0SecretMpcVectors);
                    sender.revealOther(z0SecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
