package edu.alibaba.mpc4j.work.scape.s3pc.opf.agg;

import edu.alibaba.mpc4j.common.circuit.z2.Z2CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.Z2IntegerCircuit;
import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.AbstractThreePartyOpfPto;

/**
 * abstract aggregate function party.
 *
 * @author Feng Han
 * @date 2025/2/27
 */
public abstract class AbstractAggParty extends AbstractThreePartyOpfPto implements AggParty {
    /**
     * adder type
     */
    public final ComparatorType comparatorType;
    /**
     * z2 circuit
     */
    public final Z2IntegerCircuit z2IntegerCircuit;

    protected AbstractAggParty(PtoDesc ptoDesc, Abb3Party abb3Party, AggConfig config) {
        super(ptoDesc, abb3Party, config);
        comparatorType = config.getComparatorTypes();
        z2IntegerCircuit = new Z2IntegerCircuit(abb3Party.getZ2cParty(),
            new Z2CircuitConfig.Builder().setComparatorType(comparatorType).build());
    }

    protected void checkInput(TripletLongVector input, TripletLongVector validFlag){
        MathPreconditions.checkEqual("input.getNum()", "validFlag.getNum()", input.getNum(), validFlag.getNum());
    }

    protected void checkInput(TripletZ2Vector[] input, TripletZ2Vector validFlag){
        MathPreconditions.checkEqual("input[0].getNum()", "validFlag.getNum()", input[0].getNum(), validFlag.getNum());
    }
}
