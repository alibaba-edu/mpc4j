package edu.alibaba.mpc4j.s2pc.opf.oprf;

/**
 * MPOPRF接收方输出。
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
public class MpOprfReceiverOutput extends OprfReceiverOutput {

    public MpOprfReceiverOutput(int prfByteLength, byte[][] inputs, byte[][] prfs) {
        super(prfByteLength, inputs, prfs);
    }
}
