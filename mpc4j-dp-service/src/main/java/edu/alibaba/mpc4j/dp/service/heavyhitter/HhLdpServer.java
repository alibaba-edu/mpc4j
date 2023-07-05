package edu.alibaba.mpc4j.dp.service.heavyhitter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;

import java.util.*;

/**
 * Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public interface HhLdpServer {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    HhLdpFactory.HhLdpType getType();

    /**
     * Inserts an item during the warmup state.
     *
     * @param itemBytes the item.
     * @return true if the randomized item is not ignored and successfully inserted.
     */
    @CanIgnoreReturnValue
    boolean warmupInsert(byte[] itemBytes);

    /**
     * Stops warming up.
     */
    void stopWarmup();

    /**
     * Returns the server context.
     *
     * @return the server context.
     */
    HhLdpServerContext getServerContext();

    /**
     * Inserts a randomized item.
     *
     * @param itemBytes the randomized item.
     * @return true if the randomized item is not ignored and successfully inserted.
     */
    @CanIgnoreReturnValue
    boolean randomizeInsert(byte[] itemBytes);

    /**
     * Responses Heavy Hitters.
     *
     * @return Heavy Hitters.
     */
    Map<String, Double> heavyHitters();

    /**
     * Responses Heavy Hitters with descending order list.
     *
     * @return the heavy hitter map.
     */
    default List<Map.Entry<String, Double>> orderedHeavyHitters() {
        Map<String, Double> heavyHitters = heavyHitters();
        List<Map.Entry<String, Double>> orderedHeavyHitters = new ArrayList<>(heavyHitters.entrySet());
        // descending sort
        orderedHeavyHitters.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(orderedHeavyHitters);

        return orderedHeavyHitters;
    }

    /**
     * Returns the privacy parameter ε / w.
     *
     * @return the privacy parameter ε / w.
     */
    double getWindowEpsilon();

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size d.
     */
    int getD();

    /**
     * Gets the number of Heavy Hitters k.
     *
     * @return the number of Heavy Hitters.
     */
    int getK();

    /**
     * Returns the total insert item num.
     *
     * @return the total insert item num.
     */
    int getNum();
}
