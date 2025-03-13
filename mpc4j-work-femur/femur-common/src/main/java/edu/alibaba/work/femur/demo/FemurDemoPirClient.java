package edu.alibaba.work.femur.demo;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * Femur demo PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public interface FemurDemoPirClient {
    /**
     * Registers in Femur PIR.
     *
     * @param clientId client ID.
     * @return register payload.
     */
    List<byte[]> register(String clientId);

    /**
     * Sets database parameters.
     *
     * @param paramsPayload parameter payload.
     */
    void setDatabaseParams(List<byte[]> paramsPayload);

    /**
     * Sets hint.
     *
     * @param hintPayload hint payload.
     */
    void setHint(List<byte[]> hintPayload);

    /**
     * Client generate a query.
     *
     * @param key     key.
     * @param t       t.
     * @param epsilon epsilon.
     * @return query payload.
     */
    List<byte[]> query(long key, int t, double epsilon);

    /**
     * Client retrieves value.
     *
     * @param response response.
     * @return (status, retrieved value).
     */
    Pair<FemurStatus, byte[]> retrieve(Pair<FemurStatus, List<byte[]>> response);
}
