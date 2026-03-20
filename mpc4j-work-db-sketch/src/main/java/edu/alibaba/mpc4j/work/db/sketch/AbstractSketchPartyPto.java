package edu.alibaba.mpc4j.work.db.sketch;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.AbstractAbbThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;

/**
 * Abstract base class for sketch party protocols in the S³ framework.
 * <p>
 * Provides access to the core MPC computation parties used by all sketch protocols:
 * <ul>
 *   <li>{@link Abb3Party}: the ABB3 (Arithmetic Black-Box for 3PC) party that orchestrates MPC operations.</li>
 *   <li>{@link TripletZ2cParty}: the Z2 (Boolean) circuit party for bit-level operations
 *       (e.g., comparisons, prefix-and for LeadingOnes in HLL, multiplexers).</li>
 *   <li>{@link TripletLongParty}: the Zlong (64-bit integer) arithmetic party for
 *       arithmetic operations (e.g., addition, prefix-sum for CMS).</li>
 * </ul>
 */
public abstract class AbstractSketchPartyPto extends AbstractAbbThreePartyPto implements SketchPartyPto {

    /**
     * the ABB3 party that provides high-level MPC operations including sorting, compaction, and prefix operations
     */
    protected final Abb3Party abb3Party;
    /**
     * the Z2 (Boolean) circuit party for bit-level secure computation (comparisons, AND, XOR, etc.)
     */
    protected final TripletZ2cParty z2cParty;
    /**
     * the Zlong (64-bit integer) arithmetic party for secure integer arithmetic (addition, multiplication, etc.)
     */
    protected final TripletLongParty zl64cParty;

    protected AbstractSketchPartyPto(PtoDesc ptoDesc, Abb3Party abb3Party, MultiPartyPtoConfig config) {
        super(ptoDesc, abb3Party.getRpc(), config);
        this.abb3Party = abb3Party;
        z2cParty = abb3Party.getZ2cParty();
        zl64cParty = abb3Party.getLongParty();
    }

    @Override
    public Abb3Party getAbb3Party() {
        return abb3Party;
    }

    @Override
    public void setParallel(boolean parallel) {
        abb3Party.setParallel(parallel);
        super.setParallel(parallel);
    }

    @Override
    public void setTaskId(int taskId) {
        abb3Party.setTaskId(taskId);
        super.setTaskId(taskId);
    }
}
