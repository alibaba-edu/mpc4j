package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.nio.ByteBuffer;

/**
 * client-payload circuit PSI client output.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public class CcpsiClientOutput {
    /**
     * the client table
     */
    private final ByteBuffer[] table;
    /**
     * the client share bits
     */
    private final SquareZ2Vector z1;

    public CcpsiClientOutput(ByteBuffer[] table, SquareZ2Vector z1) {
        MathPreconditions.checkPositive("β", table.length);
        this.table = table;
        MathPreconditions.checkEqual("z1.bitNum", "β", z1.getNum(), table.length);
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
