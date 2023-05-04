package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.nio.ByteBuffer;

/**
 * server-payload circuit PSI server output.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class ScpsiServerOutput {
    /**
     * the server table
     */
    private final ByteBuffer[] table;
    /**
     * the server share bits
     */
    private final SquareZ2Vector z0;

    public ScpsiServerOutput(ByteBuffer[] table, SquareZ2Vector z0) {
        MathPreconditions.checkPositive("β", table.length);
        this.table = table;
        MathPreconditions.checkEqual("z0.bitNum", "β", z0.getNum(), table.length);
        this.z0 = z0;
    }

    public int getBeta() {
        return table.length;
    }

    public ByteBuffer[] getTable() {
        return table;
    }

    public SquareZ2Vector getZ0() {
        return z0;
    }
}
