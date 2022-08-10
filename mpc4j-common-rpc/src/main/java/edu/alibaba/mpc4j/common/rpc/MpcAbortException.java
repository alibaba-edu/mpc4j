package edu.alibaba.mpc4j.common.rpc;

/**
 * MPC中Security with Abort安全模型所抛出的异常。
 *
 * @author Weiran Liu
 * @date 2022/01/13
 */
public class MpcAbortException extends Exception {
    private static final long serialVersionUID = 3694698352396653827L;
    /**
     * 异常
     */
    private Throwable cause;

    /**
     * base constructor.
     */
    public MpcAbortException() {
    }

    /**
     * create a MpcAbortException with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public MpcAbortException(String  message) {
        super(message);
    }

    /**
     * Create a MpcAbortException with the given message and underlying cause.
     *
     * @param message message describing exception.
     * @param cause the throwable that was the underlying cause.
     */
    public MpcAbortException(String  message, Throwable cause) {
        super(message);

        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
