package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;


public abstract class AbstractHLLParty extends AbstractSketchPartyPto {

    public AbstractHLLParty(PtoDesc ptoDesc, Abb3Party abb3Party, HLLConfig config){
        super(ptoDesc, abb3Party, config);
    }

}
