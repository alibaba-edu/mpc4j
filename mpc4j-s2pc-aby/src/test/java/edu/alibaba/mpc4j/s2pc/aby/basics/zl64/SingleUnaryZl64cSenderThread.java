package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * single Zl circuit sender thread for unary operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class SingleUnaryZl64cSenderThread extends Thread {
    /**
     * sender
     */
    private final Zl64cParty sender;
    /**
     * Zl
     */
    private final Zl64 zl64;
    /**
     * operator
     */
    private final UnaryAcOperator operator;
    /**
     * x vector
     */
    private final Zl64Vector xVector;
    /**
     * z vector
     */
    private final Zl64Vector zVector;
    /**
     * num
     */
    private final int num;
    /**
     * z (plain)
     */
    private Zl64Vector sendPlainVector;
    /**
     * z (secret)
     */
    private Zl64Vector sendSecretVector;

    SingleUnaryZl64cSenderThread(Zl64cParty sender, Zl64 zl64, UnaryAcOperator operator, Zl64Vector xVector) {
        this.sender = sender;
        this.zl64 = zl64;
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

    Zl64Vector getExpectVector() {
        return zVector;
    }

    Zl64Vector getSendPlainVector() {
        return sendPlainVector;
    }

    Zl64Vector getSendSecretVector() {
        return sendSecretVector;
    }

    @Override
    public void run() {
        try {
            sender.init(zl64.getL(), num);
            // set inputs
            MpcZl64Vector xPlainMpcVector = sender.create(xVector);
            MpcZl64Vector x0SecretMpcVector = sender.shareOwn(xVector);
            MpcZl64Vector z0PlainMpcVector, z0SecretMpcVector;
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
