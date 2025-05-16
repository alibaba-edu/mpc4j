package edu.alibaba.mpc4j.s2pc.upso.upsu;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PkeUpsuConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * UPSU test.
 *
 * @author Liqiang Peng
 * @date 2024/3/12
 */
@RunWith(Parameterized.class)
public class UpsuTest extends AbstractTwoPartyMemoryRpcPto {

    /**
     * sender element size
     */
    private static final int SENDER_ELEMENT_SIZE = 1 << 6;
    /**
     * receiver element size
     */
    private static final int RECEIVER_ELEMENT_SIZE = 1 << 12;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = 8;
    /**
     * UPSU config
     */
    private final UpsuConfig config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

//        // TCL23 + DDH PM-PEQT
//        configurations.add(new Object[]{
//            UpsuFactory.UpsuType.TCL23.name() + " Ecc DDH",
//            new Tcl23UpsuConfig.Builder()
//                .setPmPeqtConfig(new Tcl23EccDdhPmPeqtConfig.Builder().build())
//                .build()
//        });
//        // TCL23 + DDH PM-PEQT
//        configurations.add(new Object[]{
//            UpsuFactory.UpsuType.TCL23.name() + " Byte Ecc DDH",
//            new Tcl23UpsuConfig.Builder()
//                .setPmPeqtConfig(new Tcl23ByteEccDdhPmPeqtConfig.Builder().build())
//                .build()
//        });
//        // TCL23 + Permute + Share and OPRF PM-PEQT
//        configurations.add(new Object[]{
//            UpsuFactory.UpsuType.TCL23.name() + " Permute + Share and OPRF",
//            new Tcl23UpsuConfig.Builder()
//                .setPmPeqtConfig(new Tcl23PsOprfPmPeqtConfig.Builder().build())
//                .build()
//        });
        // ZLP24 + PKE + Vectorized PIR
        configurations.add(new Object[]{
            UpsuFactory.UpsuType.ZLP24_PKE.name() + " Vectorized PIR",
            new Zlp24PkeUpsuConfig.Builder()
                .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
                .build()
        });
//        configurations.add(new Object[]{
//            UpsuFactory.UpsuType.ZLP24_PKE.name() + " 2 Hash Vectorized PIR",
//            new Zlp24PkeUpsuConfig.Builder()
//                .setEccDokvsType(EccDokvsFactory.EccDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
//                .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
//                .build()
//        });
//        // ZLP24 + PEQT + Vectorized PIR
//        configurations.add(new Object[]{
//            UpsuFactory.UpsuType.ZLP24_PEQT.name() + " Vectorized PIR",
//            new Zlp24PeqtUpsuConfig.Builder()
//                .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
//                .build()
//        });
//        configurations.add(new Object[]{
//            UpsuFactory.UpsuType.ZLP24_PEQT.name() + " 2 Hash Vectorized PIR",
//            new Zlp24PeqtUpsuConfig.Builder()
//                .setGf2eDokvsType(Gf2eDokvsFactory.Gf2eDokvsType.H2_SPARSE_CLUSTER_BLAZE_GCT)
//                .setStdIdxPirConfig(new VectorizedStdIdxPirConfig.Builder().build())
//                .build()
//        });

        return configurations;
    }

    public UpsuTest(String name, UpsuConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testDefaultParallel() {
        testUpsu(SENDER_ELEMENT_SIZE, RECEIVER_ELEMENT_SIZE, true);
    }

    @Test
    public void testDefault() {
        testUpsu(SENDER_ELEMENT_SIZE, RECEIVER_ELEMENT_SIZE, false);
    }

    public void testUpsu(int senderElementSize, int receiverElementSize, boolean parallel) {
        List<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(senderElementSize, receiverElementSize, ELEMENT_BYTE_LENGTH);
        Set<ByteBuffer> senderElementSet = sets.get(0);
        Set<ByteBuffer> receiverElementSet = sets.get(1);
        // create instances
        UpsuSender sender = UpsuFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        UpsuReceiver receiver = UpsuFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        int randomTaskId = Math.abs(new SecureRandom().nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        // set parallel
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        try {
            UpsuSenderThread senderThread = new UpsuSenderThread(
                sender, receiverElementSize, senderElementSet, ELEMENT_BYTE_LENGTH
            );
            UpsuReceiverThread receiverThread = new UpsuReceiverThread(
                receiver, senderElementSize, receiverElementSet, ELEMENT_BYTE_LENGTH
            );
            STOP_WATCH.start();
            // start
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            // verify
            UpsuReceiverOutput receiverOutput = receiverThread.getReceiverOutput();
            assertOutput(senderElementSet, receiverElementSet, receiverOutput);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(Set<ByteBuffer> senderElementSet, Set<ByteBuffer> receiverElementSet,
                              UpsuReceiverOutput receiverOutput) {
        Set<ByteBuffer> expectUnion = new HashSet<>(receiverElementSet);
        expectUnion.addAll(senderElementSet);
        Set<ByteBuffer> expectIntersection = new HashSet<>(receiverElementSet);
        expectIntersection.retainAll(senderElementSet);
        int expectPsica = expectIntersection.size();
        Set<ByteBuffer> actualUnion = receiverOutput.getUnion();
        Assert.assertTrue(actualUnion.containsAll(expectUnion));
        Assert.assertTrue(expectUnion.containsAll(actualUnion));
        int actualPsica = receiverOutput.getPsica();
        Assert.assertEquals(expectPsica, actualPsica);
    }
}
