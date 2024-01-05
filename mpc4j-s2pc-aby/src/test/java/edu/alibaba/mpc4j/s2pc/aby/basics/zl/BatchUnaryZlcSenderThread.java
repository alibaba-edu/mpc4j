package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;

/**
 * batch Zl circuit sender thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class BatchUnaryZlcSenderThread extends Thread {
    /**
     * sender
     */
    private final ZlcParty sender;
    /**
     * operator
     */
    private final UnaryAcOperator operator;
    /**
     * x vectors
     */
    private final ZlVector[] xVectors;
    /**
     * z vectors
     */
    private final ZlVector[] zVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * zs (plain)
     */
    private ZlVector[] sendPlainVectors;
    /**
     * zs (secret)
     */
    private ZlVector[] sendSecretVectors;

    BatchUnaryZlcSenderThread(ZlcParty sender, UnaryAcOperator operator, ZlVector[] xVectors) {
        this.sender = sender;
        this.operator = operator;
        this.xVectors = xVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(ZlVector::getNum).sum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                zVectors = Arrays.stream(xVectors).map(ZlVector::neg).toArray(ZlVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    ZlVector[] getExpectVectors() {
        return zVectors;
    }

    ZlVector[] getSendPlainVectors() {
        return sendPlainVectors;
    }

    ZlVector[] getSendSecretVectors() {
        return sendSecretVectors;
    }

    @Override
    public void run() {
        try {
            sender.init(totalNum);
            // set inputs
            MpcZlVector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(sender::create)
                .toArray(MpcZlVector[]::new);
            MpcZlVector[] x0SecretMpcVectors = sender.shareOwn(xVectors);
            MpcZlVector[] z0PlainMpcVectors, z0SecretMpcVectors;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NEG:
                    // (plain, plain)
                    z0PlainMpcVectors = sender.neg(xPlainMpcVectors);
                    sendSecretVectors = sender.revealOwn(z0PlainMpcVectors);
                    sender.revealOther(z0PlainMpcVectors);
                    // (plain, secret)
                    z0SecretMpcVectors = sender.neg(x0SecretMpcVectors);
                    sendPlainVectors = sender.revealOwn(z0SecretMpcVectors);
                    sender.revealOther(z0SecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
