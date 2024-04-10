package edu.alibaba.mpc4j.common.rpc.pto;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

/**
 * abstract two-party protocol using MemoryRpc. This is used for creating test cases.
 *
 * @author Weiran Liu
 * @date 2023/5/22
 */
public abstract class AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTwoPartyMemoryRpcPto.class);
    /**
     * the random status
     */
    protected static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stop watch
     */
    protected static final StopWatch STOP_WATCH = new StopWatch();
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    public AbstractTwoPartyMemoryRpcPto(String name) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        // We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
        // In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
    }

    @Before
    public void connect() {
        firstRpc.connect();
        secondRpc.connect();
    }

    @After
    public void disconnect() {
        firstRpc.disconnect();
        secondRpc.disconnect();
    }

    protected void printAndResetRpc(long time) {
        long firstPartyByteLength = firstRpc.getSendByteLength();
        long secondPartyByteLength = secondRpc.getSendByteLength();
        firstRpc.reset();
        secondRpc.reset();
        LOGGER.info("{} sends {}B, {} sends {}B, time = {}ms",
            firstRpc.ownParty().getPartyName(), firstPartyByteLength,
            secondRpc.ownParty().getPartyName(), secondPartyByteLength,
            time
        );
    }
}
