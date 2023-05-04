package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.nio.ByteBuffer;

/**
 * Unbalanced Circuit PSI client output.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class UcpsiClientOutput {
    /**
     * the table
     */
    private final ByteBuffer[] table;
    /**
     * z1
     */
    private final SquareZ2Vector z1;

    public UcpsiClientOutput(ByteBuffer[] table, SquareZ2Vector z1) {
        MathPreconditions.checkPositive("β", table.length);
        this.table = table;
        MathPreconditions.checkEqual("share bit length", "β", z1.getNum(), table.length);
        this.z1 = z1;
    }

    public int getBeta() {
        return table.length;
    }

    public ByteBuffer[] getTable() {
        return table;
    }

    public SquareZ2Vector getZ1() {
        return z1;
    }
}
