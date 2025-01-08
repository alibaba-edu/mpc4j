package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

/**
 * This program determines the kth order statistic (the kth smallest number in a list) in O(n) time in the average
 * case and O(n^2) time in the worst case.  It achieves this through the QuickSelect algorithm.
 *
 * @author John Kurlak <john@kurlak.com>
 * @date 1/17/2013
 */
public class IntQuickSelect {
    /**
     * Determines the k-th order statistic for the given list.
     *
     * @param list The list.
     * @param k    The k value to use.
     * @return The kth order statistic for the list.
     */
    public static int quickSelect(int[] list, int k) {
        return quickSelect(list, 0, list.length - 1, k);
    }

    /**
     * Recursively determines the kth order statistic for the given list.
     *
     * @param list       The list.
     * @param leftIndex  The left index of the current sublist.
     * @param rightIndex The right index of the current sublist.
     * @param k          The k value to use.
     * @return The kth order statistic for the list.
     */
    public static int quickSelect(int[] list, int leftIndex, int rightIndex, int k) {
        // Edge case
        if (k < 1 || k > list.length) {
            throw new ArithmeticException("k < 1 or k > list.length");
        }

        // Base case
        if (leftIndex == rightIndex) {
            return list[leftIndex];
        }

        // Partition the sublist into two halves
        int pivotIndex = randomPartition(list, leftIndex, rightIndex);
        int sizeLeft = pivotIndex - leftIndex + 1;

        // Perform comparisons and recurse in binary search / quicksort fashion
        if (sizeLeft == k) {
            return list[pivotIndex];
        } else if (sizeLeft > k) {
            return quickSelect(list, leftIndex, pivotIndex - 1, k);
        } else {
            return quickSelect(list, pivotIndex + 1, rightIndex, k - sizeLeft);
        }
    }

    /**
     * Randomly partitions a set about a pivot such that the values to the left
     * of the pivot are less than or equal to the pivot and the values to the
     * right of the pivot are greater than the pivot.
     *
     * @param list       The list.
     * @param leftIndex  The left index of the current sublist.
     * @param rightIndex The right index of the current sublist.
     * @return The index of the pivot.
     */
    private static int randomPartition(int[] list, int leftIndex, int rightIndex) {
        int pivotIndex = medianOf3(list, leftIndex, rightIndex);
        int pivotValue = list[pivotIndex];
        int storeIndex = leftIndex;

        swap(list, pivotIndex, rightIndex);

        for (int i = leftIndex; i < rightIndex; i++) {
            if (list[i] <= pivotValue) {
                swap(list, storeIndex, i);
                storeIndex++;
            }
        }

        swap(list, rightIndex, storeIndex);

        return storeIndex;
    }

    /**
     * Computes the median of the first value, middle value, and last value
     * of a list.  Also rearranges the first, middle, and last values of the
     * list to be in sorted order.
     *
     * @param list       The list.
     * @param leftIndex  The left index of the current sublist.
     * @param rightIndex The right index of the current sublist.
     * @return The index of the median value.
     */
    private static int medianOf3(int[] list, int leftIndex, int rightIndex) {
        int centerIndex = (leftIndex + rightIndex) / 2;

        if (list[leftIndex] > list[rightIndex]) {
            swap(list, leftIndex, centerIndex);
        }

        if (list[leftIndex] > list[rightIndex]) {
            swap(list, leftIndex, rightIndex);
        }

        if (list[centerIndex] > list[rightIndex]) {
            swap(list, centerIndex, rightIndex);
        }

        swap(list, centerIndex, rightIndex - 1);

        return rightIndex - 1;
    }

    /**
     * Swaps two elements in a list.
     *
     * @param list   The list.
     * @param index1 The index of the first element to swap.
     * @param index2 The index of the second element to swap.
     */
    private static void swap(int[] list, int index1, int index2) {
        int temp = list[index1];
        list[index1] = list[index2];
        list[index2] = temp;
    }
}
