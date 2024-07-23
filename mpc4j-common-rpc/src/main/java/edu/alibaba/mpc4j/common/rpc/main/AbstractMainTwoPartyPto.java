package edu.alibaba.mpc4j.common.rpc.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * abstract main two party protocol.
 *
 * @author Weiran Liu
 * @date 2024/5/3
 */
public abstract class AbstractMainTwoPartyPto implements MainTwoPartyPto {
    /**
     * stop watch
     */
    protected final StopWatch stopWatch;
    /**
     * own RPC
     */
    protected final Rpc ownRpc;
    /**
     * append string
     */
    protected final String appendString;
    /**
     * save file path
     */
    protected final String filePathString;


    public AbstractMainTwoPartyPto(Properties properties, String ownName) {
        stopWatch = new StopWatch();
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
        ownRpc = RpcPropertiesUtils.readNettyRpcWithOwnName(properties, ownName, "server", "client");
    }

    @Override
    public void runNetty() throws IOException, MpcAbortException {
        if (ownRpc.ownParty().getPartyId() == 0) {
            runParty1(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runParty2(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }
}
