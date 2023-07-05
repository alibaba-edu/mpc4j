package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.ArrayList;

/**
 * server-payload circuit PSI server output.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class ScpsiServerOutput<T> {
    /**
     * the server table
     */
    private final ArrayList<T> table;
    /**
     * the server share bits
     */
    private final SquareZ2Vector z0;

    public ScpsiServerOutput(ArrayList<T> table, SquareZ2Vector z0) {
        MathPreconditions.checkPositive("β", table.size());
        this.table = table;
        MathPreconditions.checkEqual("z0.bitNum", "β", z0.getNum(), table.size());
        this.z0 = z0;
    }

    public int getBeta() {
        return table.size();
    }

    public ArrayList<T> getTable() {
        return table;
    }

    public SquareZ2Vector getZ0() {
        return z0;
    }
}
