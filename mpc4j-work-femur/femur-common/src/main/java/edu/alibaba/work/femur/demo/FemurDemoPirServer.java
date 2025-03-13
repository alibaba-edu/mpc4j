package edu.alibaba.work.femur.demo;

import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Femur demo PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public interface FemurDemoPirServer {
    /**
     * Initializes Femur PIR.
     *
     * @param n database size.
     * @param l entry bit length.
     */
    void init(int n, int l);

    /**
     * Sets database, each database generates a new version of PGM-index.
     *
     * @param keyValueDatabase key-value database.
     */
    void setDatabase(TLongObjectMap<byte[]> keyValueDatabase);

    /**
     * Gets hint.
     *
     * @return hint payload.
     */
    Pair<FemurStatus, List<byte[]>> getHint();

    /**
     * Updates value for the given key.
     *
     * @param key   key.
     * @param value value.
     * @return true if successfully update the value; false otherwise.
     */
    boolean updateValue(long key, byte[] value);

    /**
     * Registers a client.
     *
     * @param registerPayload register payload.
     */
    Pair<FemurStatus, List<byte[]>> register(List<byte[]> registerPayload);

    /**
     * Server response the query.
     *
     * @param queryPayload query payload.
     * @return (code, response payload).
     */
    Pair<FemurStatus, List<byte[]>> response(List<byte[]> queryPayload);

    /**
     * Server close.
     */
    void reset();
}
