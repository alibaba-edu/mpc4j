package edu.alibaba.work.femur.demo.naive;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.ApacheGeometricSampler;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.work.femur.demo.AbstractFemurDemoPirClient;
import edu.alibaba.work.femur.demo.FemurStatus;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Naive Femur demo PIR client.
 *
 * @author Weiran Liu
 * @date 2024/9/19
 */
public class NaiveFemurDemoRedisPirClient extends AbstractFemurDemoPirClient {
    /**
     * whether to use differential privacy
     */
    private final boolean dp;
    /**
     * queried key
     */
    private long key;
    /**
     * response size
     */
    private int responseSize;
    /**
     * epsilon range used to build this index
     */
    private final int pgmIndexLeafEpsilon;

    public NaiveFemurDemoRedisPirClient(NaiveFemurDemoRedisPirConfig config) {
        super(config);
        dp = config.getDp();
        key = BOT_KEY;
        responseSize = -1;
        pgmIndexLeafEpsilon = config.getPgmIndexLeafEpsilon();
    }

    @Override
    public List<byte[]> register(String clientId) {
        setRegisterInput(clientId);
        // client directly sends the client ID to server.
        return Collections.singletonList(clientId.getBytes(CommonConstants.DEFAULT_CHARSET));
    }

    @Override
    public List<byte[]> query(long key, int t, double epsilon) {
        checkQueryInput(key, t, epsilon, pgmIndexLeafEpsilon);

        this.key = key;
        LongApproxPgmIndex pgmIndex = pgmIndexPair.getValue();
        int[] index = pgmIndex.approximateIndexRangeOf(key);
        int range = t;
        int leftBound = (index[0] >= 0)
            // we have a candidate left_range, add negative noise
            ? index[0] - secureRandom.nextInt(
            LongApproxPgmIndex.leftBound(pgmIndexLeafEpsilon),
            t - LongApproxPgmIndex.rightBound(pgmIndexLeafEpsilon))
            // the queried key must not in the PGM-index, generate a random left range
            : secureRandom.nextInt(n);
        if (dp) {
            double b = 2.0 * t / epsilon;
            ApacheGeometricSampler sampler = new ApacheGeometricSampler(0, b);
            int leftNoise = Math.abs(sampler.sample());
            if (leftNoise >= n) {
                leftNoise = n - 1;
            }
            int rightNoise = Math.abs(sampler.sample());
            if (rightNoise >= n) {
                rightNoise = n - 1;
            }
            leftBound = leftBound - leftNoise;
            if (leftNoise + rightNoise + range > n) {
                range = n;
            } else {
                range = range + leftNoise + rightNoise;
            }
        }
        responseSize = range;
        List<byte[]> queryPayload = new ArrayList<>();
        // (ClientID, version, leftBound, Range)
        queryPayload.add(clientId.getBytes(CommonConstants.DEFAULT_CHARSET));
        queryPayload.add(pgmIndexPair.getKey().getBytes(CommonConstants.DEFAULT_CHARSET));
        queryPayload.add(IntUtils.intToByteArray(leftBound));
        queryPayload.add(IntUtils.intToByteArray(range));

        return queryPayload;
    }

    @Override
    public Pair<FemurStatus, byte[]> retrieve(Pair<FemurStatus, List<byte[]>> response) {
        FemurStatus femurStatus = response.getKey();
        switch (femurStatus) {
            case HINT_V_MISMATCH, CLIENT_NOT_REGS -> {
                return Pair.of(femurStatus, null);
            }
            case SERVER_SUCC_RES -> {
                assert key != BOT_KEY;
                assert responseSize >= 0;
                List<byte[]> responsePayload = response.getValue();
                int[] index = pgmIndexPair.getValue().approximateIndexRangeOf(key);
                if (index[0] >= 0) {
                    assert responsePayload.size() == responseSize;
                    byte[] keyBytes = LongUtils.longToByteArray(key);
                    for (int i = 0; i < responseSize; i++) {
                        byte[] keyEntry = responsePayload.get(i);
                        MathPreconditions.checkEqual("respond length", "expect length", keyEntry.length , Long.BYTES + byteL);
                        if (Arrays.equals(keyBytes, 0, Long.BYTES, keyEntry, 0, Long.BYTES)) {
                            byte[] entry = new byte[byteL];
                            System.arraycopy(keyEntry, Long.BYTES, entry, 0, byteL);
                            key = BOT_KEY;
                            responseSize = -1;
                            return Pair.of(femurStatus, entry);
                        }
                    }
                }
                key = BOT_KEY;
                responseSize = -1;
                return Pair.of(FemurStatus.SERVER_SUCC_RES, null);
            }
            default -> throw new IllegalStateException("Invalid state " + femurStatus);
        }
    }
}