package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32WprfMatrixFactory.F32WprfMatrixType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * weak PRF efficiency test. We compare efficiency with AES and LowMC.
 *
 * @author Weiran Liu
 * @date 2024/10/16
 */
public class WprfEfficiencyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WprfEfficiencyTest.class);
    /**
     * time decimal format
     */
    private static final DecimalFormat TIME_DECIMAL_FORMAT = new DecimalFormat("0.0000");
    /**
     * stop watch
     */
    private final StopWatch stopWatch;
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public WprfEfficiencyTest() {
        stopWatch = new StopWatch();
        secureRandom = new SecureRandom();
    }

    @Test
    public void testEfficiency() {
        LOGGER.info("{}\t{}", "                name", "   PRP/PRF(us)");
        int logN = 16;
        int n = 1 << logN;
        // PRP efficiency
        Prp aesPrp = PrpFactory.createInstance(PrpType.JDK_AES);
        aesPrp.setKey(BlockUtils.zeroBlock());
        byte[] aesPrpInput = BlockUtils.zeroBlock();
        // warmup
        IntStream.range(0, n).forEach(index -> aesPrp.prp(aesPrpInput));
        // efficiency
        stopWatch.start();
        IntStream.range(0, n).forEach(index -> aesPrp.prp(aesPrpInput));
        stopWatch.stop();
        double aesPrpTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
        stopWatch.reset();
        LOGGER.info("{}\t{}",
            StringUtils.leftPad(aesPrp.getPrpType().name(), 20),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(aesPrpTime), 10)
        );

        // LowMC PRP efficiency
        Prp lowMcPrp = PrpFactory.createInstance(PrpType.JDK_LONGS_LOW_MC_20);
        lowMcPrp.setKey(BlockUtils.zeroBlock());
        byte[] lowMcPrpInput = BlockUtils.zeroBlock();
        // warmup
        IntStream.range(0, n).forEach(index -> lowMcPrp.prp(lowMcPrpInput));
        // efficiency
        stopWatch.start();
        IntStream.range(0, n).forEach(index -> lowMcPrp.prp(lowMcPrpInput));
        stopWatch.stop();
        double lowMcPrpTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
        stopWatch.reset();
        LOGGER.info("{}\t{}",
            StringUtils.leftPad(lowMcPrp.getPrpType().name(), 20),
            StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(lowMcPrpTime), 10)
        );

        // F32 weak PRF efficiency
        Z3ByteField field = new Z3ByteField();
        byte[] seedA = BlockUtils.randomBlock(secureRandom);
        byte[] seedB = BlockUtils.randomBlock(secureRandom);
        for (F32WprfMatrixType type : F32WprfMatrixType.values()) {
            F32Wprf f32Wprf = new F32Wprf(field, seedA, seedB, type);
            byte[] input = field.createRandoms(F32Wprf.getInputLength(), secureRandom);
            byte[] key = f32Wprf.keyGen(secureRandom);
            f32Wprf.init(key);
            // warmup
            IntStream.range(0, n).forEach(index -> f32Wprf.prf(input));
            // efficiency
            stopWatch.start();
            IntStream.range(0, n).forEach(index -> f32Wprf.prf(input));
            stopWatch.stop();
            double f32WprfTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
            stopWatch.reset();
            LOGGER.info("{}\t{}",
                StringUtils.leftPad(F32Wprf.class.getSimpleName() + " (" + type.name() + ")", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(f32WprfTime), 10)
            );
        }

        // F23 weak PRF efficiency
        for (F23WprfMatrixType type : F23WprfMatrixType.values()) {
            F23Wprf f23Wprf = new F23Wprf(field, seedA, seedB, type);
            byte[] input = BytesUtils.randomByteArray(F23Wprf.getInputByteLength(), secureRandom);
            byte[] key = f23Wprf.keyGen(secureRandom);
            f23Wprf.init(key);
            // warmup
            IntStream.range(0, n).forEach(index -> f23Wprf.prf(input));
            // efficiency
            stopWatch.start();
            IntStream.range(0, n).forEach(index -> f23Wprf.prf(input));
            stopWatch.stop();
            double f32WprfTime = (double) stopWatch.getTime(TimeUnit.MICROSECONDS) / n;
            stopWatch.reset();
            LOGGER.info("{}\t{}",
                StringUtils.leftPad(F23Wprf.class.getSimpleName() + " (" + type.name() + ")", 20),
                StringUtils.leftPad(TIME_DECIMAL_FORMAT.format(f32WprfTime), 10)
            );
        }
    }
}
