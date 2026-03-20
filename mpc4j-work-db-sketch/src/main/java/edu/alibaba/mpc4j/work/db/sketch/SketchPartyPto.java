package edu.alibaba.mpc4j.work.db.sketch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.ThreePartyPto;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;

/**
 * This module contains the implementations for the sketch-DB protocols
 * Sketch-based Secure Query Processing for Streaming Data (SIGMOD 2026)
 *
 * Interface for a sketch party protocol in the S³ framework.
 * <p>
 * In the outsourced 3PC (three-party computation) model, three non-colluding servers
 * jointly maintain sketch data structures in secret-shared form. Each server runs
 * an instance of {@link SketchPartyPto} to participate in the Merge and Query protocols.
 * <p>
 * This interface extends {@link ThreePartyPto} and provides access to the underlying
 * {@link Abb3Party} which handles the low-level MPC operations (Z2 Boolean circuits,
 * arithmetic circuits, sorting, compaction, etc.).
 */
public interface SketchPartyPto extends ThreePartyPto {
    /**
     * initialize the party
     */
    void init() throws MpcAbortException;

    /**
     * get the abb3 party
     */
    Abb3Party getAbb3Party();
}
