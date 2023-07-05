package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.ArrayList;

/**
 * client-payload circuit PSI client output.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public class CcpsiClientOutput<T> {
    /**
     * the client table
     */
    private final ArrayList<T> table;
    /**
     * the client share bits
     */
    private final SquareZ2Vector z1;

    public CcpsiClientOutput(ArrayList<T> table, SquareZ2Vector z1) {
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        MathPreconditions.checkEqual("z1.bitNum", "β", z1.getNum(), table.size());
        this.z1 = z1;
    }

    public int getBeta() {
        return table.size();
    }

    public ArrayList<T> getTable() {
        return table;
    }

    public SquareZ2Vector getZ1() {
        return z1;
    }
}
