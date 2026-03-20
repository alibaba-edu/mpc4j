package edu.alibaba.mpc4j.work.db.sketch.HLL;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.AbstractSketchPartyPto;

/**
 * Abstract base class for HyperLogLog (HLL) party implementations in the S³ Framework.
 * 
 * This class provides common functionality for all HLL party implementations.
 * It extends AbstractSketchPartyPto to inherit general sketch party capabilities
 * and initializes the HLL protocol with the specified configuration.
 * 
 * Concrete implementations (e.g., HLLz2Party) extend this class to provide
 * protocol-specific implementations of the HLL operations.
 */
public abstract class AbstractHLLParty extends AbstractSketchPartyPto {

    /**
     * Constructs an AbstractHLLParty with the specified protocol description and configuration.
     * 
     * This constructor initializes the HLL protocol by setting up the protocol description,
     * the ABB3 party for secure computation primitives, and the HLL configuration.
     * 
     * @param ptoDesc the protocol description containing metadata about the HLL protocol
     * @param abb3Party the ABB3 party instance providing secure computation primitives
     * @param config the HLL configuration specifying protocol parameters and security model
     */
    public AbstractHLLParty(PtoDesc ptoDesc, Abb3Party abb3Party, HLLConfig config) {
        super(ptoDesc, abb3Party, config);
    }

}
