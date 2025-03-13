package edu.alibaba.mpc4j.s3pc.abb3.mainpto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3RpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Properties;

/**
 * abstract main two party protocol.
 *
 * @author Feng Han
 * @date 2024/5/3
 */
public abstract class AbstractMainAbb3PartyPto implements MainAbb3PartyPto {
    /**
     * malicious secure or not
     */
    public final static String PARALLEL = "parallel";
    /**
     * malicious secure or not
     */
    public final static String IS_MALICIOUS = "malicious";
    /**
     * verify multiplication with MAC
     */
    public final static String VERIFY_WITH_MAC = "use_mac";
    /**
     * use simulate mtp or not
     */
    public final static String USE_MT_SIM_MODE = "mt_sim_mode";
    /**
     * stop watch
     */
    protected final StopWatch stopWatch;
    /**
     * stop watch
     */
    protected final SecureRandom secureRandom;
    /**
     * is malicious secure or not
     */
    protected final boolean parallel;
    /**
     * is malicious secure or not
     */
    protected final boolean isMalicious;
    /**
     * use mac or not
     */
    protected final boolean useMac;
    /**
     * simulate mt generation or not
     */
    protected final boolean usSimMt;
    /**
     * own RPC
     */
    protected final Rpc ownRpc;
    /**
     * abb3 config
     */
    protected final Abb3RpConfig abb3RpConfig;
    /**
     * append string
     */
    protected final String appendString;
    /**
     * save file path
     */
    protected final String filePathString;

    public AbstractMainAbb3PartyPto(Properties properties, String ownName) {
        stopWatch = new StopWatch();
        secureRandom = new SecureRandom();
        // read append string
        appendString = MainPtoConfigUtils.readAppendString(properties);
        // read save file path
        filePathString = MainPtoConfigUtils.readFileFolderName(properties);
        File inputFolder = new File(filePathString);
        if (!inputFolder.exists()) {
            boolean success = inputFolder.mkdir();
            assert success;
        }
        // read RPC
        ownRpc = RpcPropertiesUtils.readNettyRpcWithOwnName(properties, ownName, "first", "second", "third");
        parallel = PropertiesUtils.readBoolean(properties, PARALLEL, true);
        isMalicious = PropertiesUtils.readBoolean(properties, IS_MALICIOUS);
        useMac = PropertiesUtils.readBoolean(properties, VERIFY_WITH_MAC);
        usSimMt = PropertiesUtils.readBoolean(properties, USE_MT_SIM_MODE);
        abb3RpConfig = (isMalicious && usSimMt)
            ? new Abb3RpConfig.Builder(true, useMac)
            .setTripletProviderConfig(new TripletProviderConfig.Builder(true)
                .setRpZ2MtpConfig(RpMtProviderFactory.createZ2MtpConfigTestMode())
                .setRpZl64MtpConfig(RpMtProviderFactory.createZl64MtpConfigTestMode())
                .build()).build()
            : new Abb3RpConfig.Builder(isMalicious, useMac).build();
    }

    @Override
    public void runNetty() throws IOException, MpcAbortException {
        runParty(ownRpc);
    }
}
