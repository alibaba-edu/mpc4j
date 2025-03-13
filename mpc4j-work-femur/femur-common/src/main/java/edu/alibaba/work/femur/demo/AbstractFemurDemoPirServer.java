package edu.alibaba.work.femur.demo;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.structure.pgm.LongApproxPgmIndex;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.tuple.Pair;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * abstract Femur demo PIR server.
 *
 * @author Liqiang Peng
 * @date 2024/9/10
 */
public abstract class AbstractFemurDemoPirServer implements FemurDemoPirServer {
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
    protected boolean init;
    /**
     * PGM-index pair
     */
    protected Pair<String, LongApproxPgmIndex> pgmIndexPair;
    /**
     * key representing ⊥
     */
    protected static final long BOT_KEY = Long.MAX_VALUE;

    protected AbstractFemurDemoPirServer(FemurDemoPirConfig config) {
        this.config = config;
        secureRandom = new SecureRandom();
        init = false;
    }

    protected void setInitInput(int n, int l) {
        Preconditions.checkArgument(!init);
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        MathPreconditions.checkPositive("l", l);
        MathPreconditions.checkEqual("l", "0", l % Long.SIZE, 0);
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        init = true;
    }

    protected void innerReset() {
        n = 0;
        l = 0;
        byteL = 0;
        pgmIndexPair = null;
        init = false;
    }

    protected void checkSetDatabaseInput(TLongObjectMap<byte[]> database) {
        Preconditions.checkArgument(init);
        MathPreconditions.checkEqual("n", "database.size", n, database.size());
        long[] keyArray = database.keys();
        IntStream.range(0, n).forEach(i -> {
            Preconditions.checkArgument(
                keyArray[i] != BOT_KEY,
                "k_" + i + " must not equal ⊥ (" + BOT_KEY + ")"
            );
            byte[] entry = database.get(keyArray[i]);
            Preconditions.checkArgument(
                entry.length == byteL,
                "v_" + i + " must have length " + byteL
            );
        });
    }

    @Override
    public Pair<FemurStatus, List<byte[]>> getHint() {
        if (!init) {
            return Pair.of(FemurStatus.SERVER_NOT_INIT, new LinkedList<>());
        }
        if (pgmIndexPair == null) {
            return Pair.of(FemurStatus.SERVER_NOT_KVDB, new LinkedList<>());
        }
        List<byte[]> hintPayload = new ArrayList<>();
        hintPayload.add(pgmIndexPair.getKey().getBytes(CommonConstants.DEFAULT_CHARSET));
        hintPayload.add(pgmIndexPair.getValue().toByteArray());
        return Pair.of(FemurStatus.SERVER_SUCC_RES, hintPayload);
    }
}
