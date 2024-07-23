package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * index PIR client where the scheme supports batch query using probabilistic batch code (PBC) technique.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public interface PbcableStdIdxPirClient extends IdxPirClient {
    /**
     * Generates key pair.
     *
     * @return key pair.
     */
    Pair<List<byte[]>, List<byte[]>> keyGen();

    /**
     * Client initializes the protocol.
     *
     * @param clientKeys  client keys.
     * @param n           database size.
     * @param l           element bit length.
     * @param maxBatchNum max batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(List<byte[]> clientKeys, int n, int l, int maxBatchNum) throws MpcAbortException;

    /**
     * Generates and sends the query.
     *
     * @param x queried input.
     */
    void query(int x);

    /**
     * Recovers the entry.
     *
     * @param x queried input.
     * @return entry.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[] recover(int x) throws MpcAbortException;

    /**
     * Recovers a dummy entry.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    void dummyRecover() throws MpcAbortException;
}
