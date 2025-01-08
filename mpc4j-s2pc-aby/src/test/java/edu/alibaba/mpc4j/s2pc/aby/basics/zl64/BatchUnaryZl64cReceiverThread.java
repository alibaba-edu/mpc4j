package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;

/**
 * batch Zl circuit receiver thread for unary operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class BatchUnaryZl64cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Zl64cParty receiver;
    /**
     * Zl
     */
    private final Zl64 zl64;
    /**
     * operator
     */
    private final UnaryAcOperator operator;
    /**
     * x vectors
     */
    private final Zl64Vector[] xVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * z (plain)
     */
    private Zl64Vector[] recvPlainVectors;
    /**
     * z (secret)
     */
    private Zl64Vector[] recvSecretVectors;

    BatchUnaryZl64cReceiverThread(Zl64cParty receiver, Zl64 zl64, UnaryAcOperator operator, Zl64Vector[] xVectors) {
        this.receiver = receiver;
        this.zl64 = zl64;
        this.operator = operator;
        this.xVectors = xVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(Zl64Vector::getNum).sum();
    }

    Zl64Vector[] getRecvPlainVectors() {
        return recvPlainVectors;
    }

    Zl64Vector[] getRecvSecretVectors() {
        return recvSecretVectors;
    }

    @Override
    public void run() {
        try {
            receiver.init(zl64.getL(), totalNum);
            // set inputs
            MpcZl64Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(receiver::create)
                .toArray(MpcZl64Vector[]::new);
            int[] nums = Arrays.stream(xVectors).mapToInt(Zl64Vector::getNum).toArray();
            MpcZl64Vector[] x1SecretMpcVectors = receiver.shareOther(zl64, nums);
            MpcZl64Vector[] z1PlainMpcVectors, z1SecretMpcVectors;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NEG:
                    // (plain, plain)
                    z1PlainMpcVectors = receiver.neg(xPlainMpcVectors);
                    receiver.revealOther(z1PlainMpcVectors);
                    recvSecretVectors = receiver.revealOwn(z1PlainMpcVectors);
                    // (plain, secret)
                    z1SecretMpcVectors = receiver.neg(x1SecretMpcVectors);
                    receiver.revealOther(z1SecretMpcVectors);
                    recvPlainVectors = receiver.revealOwn(z1SecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
