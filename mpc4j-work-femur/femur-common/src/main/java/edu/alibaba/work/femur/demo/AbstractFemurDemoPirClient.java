package edu.alibaba.work.femur.demo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.security.SecureRandom;
import java.util.List;

/**
 * Abstract Femur demo PIR client.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public abstract class AbstractFemurDemoPirClient implements FemurDemoPirClient {
    /**
     * random state
     */
    protected final SecureRandom secureRandom;
    /**
     * config
     */
    protected FemurDemoPirConfig config;
    /**
     * database size
     */
    protected int n;
    /**
     * entry bit length
     */
    protected int l;
    /**
     * entry byte length
     */
    protected int byteL;
    /**
     * init state
     */
    private boolean init;
    /**
     * client ID
     */
    protected String clientId;
    /**
     * (version, PGM-index)
     */
    protected Pair<String, LongApproxPgmIndex> pgmIndexPair;
    /**
     * key representing ⊥
     */
    protected static final long BOT_KEY = Long.MAX_VALUE;

    protected AbstractFemurDemoPirClient(FemurDemoPirConfig config) {
        this.config = config;
        secureRandom = new SecureRandom();
        init = false;
    }

    protected void setRegisterInput(String clientId) {
        Preconditions.checkArgument(!init);
        this.clientId = clientId;
    }

    @Override
    public void setDatabaseParams(List<byte[]> paramsPayload) {
        Preconditions.checkArgument(!init);
        MathPreconditions.checkEqual("n", "2", paramsPayload.size(), 2);
        n = IntUtils.byteArrayToInt(paramsPayload.get(0));
        MathPreconditions.checkPositive("n", n);
        l = IntUtils.byteArrayToInt(paramsPayload.get(1));
        MathPreconditions.checkPositive("l", l);
        MathPreconditions.checkEqual("l % Long.SIZE", "0", l % Long.SIZE, 0);
        byteL = CommonUtils.getByteLength(l);
        init = true;
    }

    @Override
    public void setHint(List<byte[]> hintPayload) {
        Preconditions.checkArgument(init);
        MathPreconditions.checkEqual("hintPayload.size()", "2", hintPayload.size(), 2);
        String version = new String(hintPayload.get(0), CommonConstants.DEFAULT_CHARSET);
        LongApproxPgmIndex pgmIndex = LongApproxPgmIndex.fromByteArray(hintPayload.get(1));
        MathPreconditions.checkEqual("n", "PGM-index.size()", n, pgmIndex.size());
        this.pgmIndexPair = Pair.of(version, pgmIndex);
    }

    protected void checkQueryInput(long key, int t, double epsilon, int pgmIndexLeafEpsilon) {
        Preconditions.checkArgument(
            key != BOT_KEY,
            "k must not equal ⊥ (" + BOT_KEY + ")"
        );
        // we must set PGM-index
        Preconditions.checkNotNull(pgmIndexPair);
        MathPreconditions.checkPositive("epsilon", epsilon);
        // check t
        MathPreconditions.checkGreaterOrEqual("t", t, LongApproxPgmIndex.bound(pgmIndexLeafEpsilon));
        // check register
        Preconditions.checkNotNull(clientId);
    }
}
