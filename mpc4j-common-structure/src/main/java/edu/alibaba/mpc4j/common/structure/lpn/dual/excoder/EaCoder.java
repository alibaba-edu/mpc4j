package edu.alibaba.mpc4j.common.structure.lpn.dual.excoder;

import edu.alibaba.mpc4j.common.structure.lpn.dual.expander.NonSysExpanderCoder;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * abstract EA coder.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class EaCoder implements ExCoder {
    /**
     * non-systematic expander coder
     */
    private final NonSysExpanderCoder expanderCoder;
    /**
     * The message size of the code, i.e., k.
     */
    private final int k;
    /**
     * The codeword size of the code, i.e., n.
     */
    private final int n;

    /**
     * Creates an EA coder.
     *
     * @param k              k.
     * @param n              n.
     * @param expanderWeight expander weight.
     */
    public EaCoder(int k, int n, int expanderWeight) {
        expanderCoder = new NonSysExpanderCoder(k, n, expanderWeight);
        this.k = k;
        this.n = n;
    }

    @Override
    public int getCodeSize() {
        return n;
    }

    @Override
    public int getMessageSize() {
        return k;
    }

    @Override
    public void setParallel(boolean parallel) {
        expanderCoder.setParallel(parallel);
    }

    @Override
    public boolean getParallel() {
        return expanderCoder.getParallel();
    }

    @Override
    public boolean[] dualEncode(boolean[] es) {
        MathPreconditions.checkEqual("n", "inputs.length", n, es.length);
        boolean[] ws = new boolean[n];
        System.arraycopy(es, 0, ws, 0, n);
        // accumulate
        for (int i = 0; i < n - 1; i++) {
            ws[i + 1] ^= ws[i];
        }
        // expand
        return expanderCoder.dualEncode(ws);
    }

    @Override
    public byte[][] dualEncode(byte[][] es) {
        MathPreconditions.checkEqual("n", "inputs.length", n, es.length);
        // here we cannot use System.arraycopy since this would be a soft copy.
        byte[][] ws = BytesUtils.clone(es);
        // accumulate
        for (int i = 0; i < n - 1; i++) {
            BytesUtils.xori(ws[i + 1], ws[i]);
        }
        // expand
        return expanderCoder.dualEncode(ws);
    }
}
