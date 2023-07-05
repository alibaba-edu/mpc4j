package edu.alibaba.mpc4j.s2pc.upso.upsi;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * UPSI test.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
@RunWith(Parameterized.class)
public class UpsiTest extends AbstractTwoPartyPtoTest {
    /**
     * server element size
     */
    private static final int SERVER_ELEMENT_SIZE = 1 << 18;
    /**
     * client element size
     */
    private static final int CLIENT_ELEMENT_SIZE = 1 << 12;
    /**
     * max client element size
     */
    private static final int MAX_CLIENT_ELEMENT_SIZE = 5535;
    /**
     * UPSI config
     */
    private final UpsiConfig config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            UpsiFactory.UpsiType.CMG21.name(), new Cmg21UpsiConfig.Builder().build()
        });

        return configurations;
    }

    public UpsiTest(String name, UpsiConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testCmg21Parallel() {
        testUpsi(SERVER_ELEMENT_SIZE, CLIENT_ELEMENT_SIZE, true);
    }

    @Test
    public void testCmg21() {
        testUpsi(SERVER_ELEMENT_SIZE, CLIENT_ELEMENT_SIZE, false);
    }

    public void testUpsi(int serverSize, int clientSize, boolean parallel) {
        assert clientSize <= MAX_CLIENT_ELEMENT_SIZE;
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        // create instances
        UpsiServer<String> server = UpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        UpsiClient<String> client = UpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        // set parallel
        server.setParallel(parallel);
        client.setParallel(parallel);
        try {
            UpsiServerThread<String> serverThread = new UpsiServerThread<>(
                server, MAX_CLIENT_ELEMENT_SIZE, serverElementSet, clientElementSet.size()
            );
            UpsiClientThread<String> clientThread = new UpsiClientThread<>(
                client, MAX_CLIENT_ELEMENT_SIZE, clientElementSet
            );
            STOP_WATCH.start();
            // start
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            Set<String> psiResult = clientThread.getIntersectionSet();
            sets.get(0).retainAll(sets.get(1));
            Assert.assertTrue(sets.get(0).containsAll(psiResult));
            Assert.assertTrue(psiResult.containsAll(sets.get(0)));
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
