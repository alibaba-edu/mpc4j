package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

/**
 * single Zl circuit sender thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleUnaryZlcSenderThread extends Thread {
    /**
     * sender
     */
    private final ZlcParty sender;
    /**
     * operator
     */
    private final UnaryAcOperator operator;
    /**
     * x vector
     */
    private final ZlVector xVector;
    /**
     * z vector
     */
    private final ZlVector zVector;
    /**
     * num
     */
    private final int num;
    /**
     * z (plain)
     */
    private ZlVector sendPlainVector;
    /**
     * z (secret)
     */
    private ZlVector sendSecretVector;

    SingleUnaryZlcSenderThread(ZlcParty sender, UnaryAcOperator operator, ZlVector xVector) {
        this.sender = sender;
        this.operator = operator;
        this.xVector = xVector;
        num = xVector.getNum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                zVector = xVector.neg();
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    ZlVector getExpectVector() {
        return zVector;
    }

    ZlVector getSendPlainVector() {
        return sendPlainVector;
    }

    ZlVector getSendSecretVector() {
        return sendSecretVector;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            // set inputs
            MpcZlVector xPlainMpcVector = sender.create(xVector);
            MpcZlVector x0SecretMpcVector = sender.shareOwn(xVector);
            MpcZlVector z0PlainMpcVector, z0SecretMpcVector;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NEG:
                    // (plain, plain)
                    z0PlainMpcVector = sender.neg(xPlainMpcVector);
                    sendPlainVector = sender.revealOwn(z0PlainMpcVector);
                    sender.revealOther(z0PlainMpcVector);
                    // (plain, secret)
                    z0SecretMpcVector = sender.neg(x0SecretMpcVector);
                    sendSecretVector = sender.revealOwn(z0SecretMpcVector);
                    sender.revealOther(z0SecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
