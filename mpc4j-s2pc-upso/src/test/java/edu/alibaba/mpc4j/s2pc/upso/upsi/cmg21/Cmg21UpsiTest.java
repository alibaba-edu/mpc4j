package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiFactory;
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
 * CMG21 UPSI test.
 *
 * @author Liqiang Peng
 * @date 2022/5/26
 */
@RunWith(Parameterized.class)
public class Cmg21UpsiTest extends AbstractTwoPartyMemoryRpcPto {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CMG21
        configurations.add(new Object[]{
            UpsiFactory.UpsiType.CMG21.name(), new Cmg21UpsiConfig.Builder().build()
        });

        return configurations;
    }

    /**
     * CMG21 UPSI config
     */
    private final Cmg21UpsiConfig config;

    public Cmg21UpsiTest(String name, Cmg21UpsiConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2K1() {
        testUpsi(Cmg21UpsiParams.SERVER_2K_CLIENT_MAX_1, false);
    }

    @Test
    public void test100K1() {
        testUpsi(Cmg21UpsiParams.SERVER_100K_CLIENT_MAX_1, false);
    }

    @Test
    public void test1M1024Cmp() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP, false);
    }

    @Test
    public void test1M1024CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_CMP, true);
    }

    @Test
    public void test1M1024Com() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM, false);
    }

    @Test
    public void test1M1024ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM, true);
    }

    @Test
    public void test1M11041Parallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_11041, true);
    }

    @Test
    public void test1M2048CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_CMP, true);
    }

    @Test
    public void test1M2048ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_COM, true);
    }

    @Test
    public void test1M256Parallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256, true);
    }

    @Test
    public void test1M4096CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_CMP, true);
    }

    @Test
    public void test1M4096ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_COM, true);
    }

    @Test
    public void test1M512CmpParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_CMP, true);
    }

    @Test
    public void test1M512ComParallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_COM, true);
    }

    @Test
    public void test1M5535Parallel() {
        testUpsi(Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535, true);
    }

    public void testUpsi(Cmg21UpsiParams upsiParams, boolean parallel) {
        int serverSize = upsiParams.expectServerSize();
        int clientSize = upsiParams.maxClientElementSize();
        List<Set<String>> sets = PsoUtils.generateStringSets("ID", serverSize, clientSize);
        Set<String> serverElementSet = sets.get(0);
        Set<String> clientElementSet = sets.get(1);
        // create instances
        Cmg21UpsiServer<String> server = new Cmg21UpsiServer<>(firstRpc, secondRpc.ownParty(), config);
        Cmg21UpsiClient<String> client = new Cmg21UpsiClient<>(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        server.setParallel(parallel);
        client.setParallel(parallel);
        try {
            Cmg21UpsiServerThread<String> serverThread = new Cmg21UpsiServerThread<>(
                server, upsiParams, serverElementSet, clientElementSet.size()
            );
            Cmg21UpsiClientThread<String> clientThread = new Cmg21UpsiClientThread<>(client, upsiParams, clientElementSet);
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