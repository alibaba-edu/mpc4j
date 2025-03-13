package edu.alibaba.mpc4j.work.scape.s3pc.db.tools;

import edu.alibaba.mpc4j.s3pc.abb3.structure.z2.TripletZ2Vector;
import edu.alibaba.mpc4j.s3pc.abb3.structure.zlong.TripletLongVector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utils for input process
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class InputProcessUtils {
    /**
     * Reorganize the input data in order, putting the key join_key, the payload second, and the flag last.
     *
     * @param table    consisting of join_key, payload, and valid_indicator
     * @param keyIndex the indexes of the join_key
     */
    public static TripletZ2Vector[] reshapeInput(TripletZ2Vector[] table, int[] keyIndex) {
        TripletZ2Vector[] newArrays = new TripletZ2Vector[table.length];
        IntStream.range(0, keyIndex.length).forEach(i -> newArrays[i] = table[keyIndex[i]]);
        HashSet<Integer> hashSet = Arrays.stream(keyIndex).boxed().collect(Collectors.toCollection(HashSet::new));
        int index = 0;
        for (int i = keyIndex.length; i < table.length; i++) {
            while (hashSet.contains(index)) {
                index++;
            }
            newArrays[i] = table[index];
            index++;
        }
        return newArrays;
    }

    /**
     * Reorganize the input data in order, putting the key join_key, the payload second, and the flag last.
     *
     * @param table    consisting of join_key, payload, and valid_indicator
     * @param keyIndex the indexes of the join_key
     */
    public static TripletLongVector[] reshapeInput(TripletLongVector[] table, int[] keyIndex) {
        TripletLongVector[] newArrays = new TripletLongVector[table.length];
        IntStream.range(0, keyIndex.length).forEach(i -> newArrays[i] = table[keyIndex[i]]);
        HashSet<Integer> hashSet = Arrays.stream(keyIndex).boxed().collect(Collectors.toCollection(HashSet::new));
        int index = 0;
        for (int i = keyIndex.length; i < table.length; i++) {
            while (hashSet.contains(index)) {
                index++;
            }
            newArrays[i] = table[index];
            index++;
        }
        return newArrays;
    }

    /**
     * Reorganize the input data in order, putting the key join_key, the payload second, the parameters third, and the flag last.
     *
     * @param parameters some parameters should be appended into the positions that:
     *                   (1) after the join_key and payload, (2) but before the valid indicator flag
     * @param table      consisting of join_key, payload, and valid_indicator
     * @param keyIndex   the indexes of the join_key
     * @return [join_key, payload, parameters, valid_indicator]
     */
    public static TripletLongVector[] concat(TripletLongVector[] parameters, TripletLongVector[] table, int[] keyIndex) {
        TripletLongVector[] newArrays = new TripletLongVector[parameters.length + table.length];
        IntStream.range(0, keyIndex.length).forEach(i -> newArrays[i] = table[keyIndex[i]]);
        HashSet<Integer> hashSet = Arrays.stream(keyIndex).boxed().collect(Collectors.toCollection(HashSet::new));
        int index = 0;
        for (int i = keyIndex.length; i < table.length - 1; i++) {
            while (hashSet.contains(index)) {
                index++;
            }
            newArrays[i] = table[index];
            index++;
        }
        System.arraycopy(parameters, 0, newArrays, table.length - 1, parameters.length);
        newArrays[newArrays.length - 1] = table[table.length - 1];
        return newArrays;
    }

    /**
     * append attributes into the table
     *
     * @param original original table, the last column is valid indicator flag
     * @param newAttr  new attributes
     * @return [original_attributes, new_attributes, valid_indicator]
     */
    public static TripletLongVector[] appendAttributes(TripletLongVector[] original, TripletLongVector[] newAttr) {
        TripletLongVector[] newArrays = new TripletLongVector[original.length + newAttr.length];
        System.arraycopy(original, 0, newArrays, 0, original.length - 1);
        System.arraycopy(newAttr, 0, newArrays, original.length - 1, newAttr.length);
        newArrays[newArrays.length - 1] = original[original.length - 1];
        return newArrays;
    }
}
