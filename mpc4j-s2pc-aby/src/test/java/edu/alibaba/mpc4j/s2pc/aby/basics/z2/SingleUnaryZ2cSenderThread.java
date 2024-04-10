package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * single Boolean circuit sender thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleUnaryZ2cSenderThread extends Thread {
    /**
     * sender
     */
    private final Z2cParty sender;
    /**
     * operator
     */
    private final UnaryBcOperator operator;
    /**
     * x vector
     */
    private final BitVector xVector;
    /**
     * z vector
     */
    private final BitVector zVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * z (plain)
     */
    private BitVector sendPlainVector;
    /**
     * z (secret)
     */
    private BitVector sendSecretVector;

    SingleUnaryZ2cSenderThread(Z2cParty sender, UnaryBcOperator operator, BitVector xVector) {
        this.sender = sender;
        this.operator = operator;
        this.xVector = xVector;
        bitNum = xVector.bitNum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NOT:
                zVector = xVector.not();
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryBcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    BitVector getExpectVector() {
        return zVector;
    }

    BitVector getSendPlainVector() {
        return sendPlainVector;
    }

    BitVector getSendSecretVector() {
        return sendSecretVector;
    }

    @Override
    public void run() {
        try {
            sender.init(bitNum);
            // set inputs
            MpcZ2Vector xPlainMpcVector = sender.create(true, xVector);
            MpcZ2Vector x0SecretMpcVector = sender.shareOwn(xVector);
            MpcZ2Vector z0PlainMpcVector, z0SecretMpcVector;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NOT:
                    // (plain, plain)
                    z0PlainMpcVector = sender.not(xPlainMpcVector);
                    sendPlainVector = sender.revealOwn(z0PlainMpcVector);
                    sender.revealOther(z0PlainMpcVector);
                    // (plain, secret)
                    z0SecretMpcVector = sender.not(x0SecretMpcVector);
                    sendSecretVector = sender.revealOwn(z0SecretMpcVector);
                    sender.revealOther(z0SecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
