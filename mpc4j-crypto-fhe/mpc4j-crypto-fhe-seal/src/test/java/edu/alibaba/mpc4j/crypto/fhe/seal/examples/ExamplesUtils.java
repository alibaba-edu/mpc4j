package edu.alibaba.mpc4j.crypto.fhe.seal.examples;

import edu.alibaba.mpc4j.crypto.fhe.seal.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.seal.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.seal.zq.UintCore;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;

/**
 * Examples Utils.
 *
 * @author Liqiang Peng
 * @date 2023/12/22
 */
public class ExamplesUtils {

    /**
     * Helper function: Returns the base-2 logarithm of a value.
     *
     * @param x the value.
     * @return the base-2 logarithm.
     */
    static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    /**
     * Helper function: Prints the name of the example in a fancy banner.
     *
     * @param title the title.
     */
    static void printExampleBanner(String title) {
        if (!StringUtils.isEmpty(title)) {
            int titleLength = title.length();
            int bannerLength = titleLength + 2 * 10;
            String bannerTop = "+" + StringUtils.repeat('-', bannerLength - 2) + "+";
            String bannerMiddle = "|" + StringUtils.repeat(' ', 9) + title + StringUtils.repeat(' ', 9) + "|";

            System.out.print("\n" + bannerTop + "\n" + bannerMiddle + "\n" + bannerTop + "\n");
        }
    }

    /**
     * Helper function: Prints the parameters in a SEALContext.
     *
     * @param context the SEALContext.
     */
    static void printParameters(SealContext context) {
        SealContext.ContextData contextData = context.keyContextData();

        /*
        Which scheme are we using?
        */
        String schemeName = switch (contextData.parms().scheme()) {
            case BFV -> "BFV";
            case CKKS -> "CKKS";
            case BGV -> "BGV";
            default -> throw new IllegalArgumentException("unsupported scheme");
        };
        System.out.print("/\n");
        System.out.print("| Encryption parameters :\n");
        System.out.print("|   scheme: " + schemeName + "\n");
        System.out.print("|   poly_modulus_degree: " + contextData.parms().polyModulusDegree() + "\n");

        /*
         * Print the size of the true (product) coefficient modulus.
         */
        System.out.print("|   coeff_modulus size: ");
        System.out.print(contextData.totalCoeffModulusBitCount() + " (");
        Modulus[] coeffModulus = contextData.parms().coeffModulus();
        int coeffModulusSize = coeffModulus.length;
        for (int i = 0; i < coeffModulusSize - 1; i++) {
            System.out.print(coeffModulus[i].bitCount() + " + ");
        }
        System.out.print(coeffModulus[coeffModulusSize - 1].bitCount());
        System.out.print(") bits\n");

        /*
         * For the BFV scheme print the plain_modulus parameter.
         */
        if (contextData.parms().scheme().equals(SchemeType.BFV)) {
            System.out.print("|   plain_modulus: " + contextData.parms().plainModulus().value() + "\n");
        }

        System.out.print("\\\n");
    }

    /**
     * Helper function: Prints a matrix of values.
     *
     * @param matrix  the matrix.
     * @param rowSize the row size.
     */
    static void printMatrix(long[] matrix, int rowSize) {
        /*
         * We're not going to print every column of the matrix (there are 2048). Instead,
         * print this many slots from beginning and end of the matrix.
         */
        int printSize = 5;

        System.out.print("\n");
        System.out.print("    [");
        for (int i = 0; i < printSize; i++) {
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ",");
        }
        System.out.print(StringUtils.leftPad(" ...,", 3));
        for (int i = rowSize - printSize; i < rowSize; i++) {
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ((i != rowSize - 1) ? "," : " ]\n"));
        }
        System.out.print("    [");
        for (int i = rowSize; i < rowSize + printSize; i++) {
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ",");
        }
        System.out.print(StringUtils.rightPad(" ...,", 3));
        for (int i = 2 * rowSize - printSize; i < 2 * rowSize; i++) {
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ((i != 2 * rowSize - 1) ? "," : " ]\n"));
        }
        System.out.print("\n");
    }

    /**
     * Helper function: Print line number. Although we always call this function as
     * <code>printLine(Thread.currentThread().getStackTrace()[1].getLineNumber());</code>,
     * we need to pass the line number as a parameter because the line number is dynamically obtained.
     *
     * @param lineNumber the line number.
     */
    static void printLine(int lineNumber) {
        System.out.print("Line " + StringUtils.leftPad(String.valueOf(lineNumber), 3) + " --> ");
    }

    /**
     * Prints a vector of floating-point values.
     *
     * @param vector vector.
     */
    static void printVector(double[] vector) {
        printVector(vector, 4, 3);
    }

    /**
     * Prints a vector of floating-point values.
     *
     * @param vector    vector.
     * @param printSize number of values to print.
     * @param prec      precision.
     */
    static void printVector(double[] vector, int printSize, int prec) {
        TDoubleArrayList vec = new TDoubleArrayList(vector);
        /*
        Save the formatting information for std::cout, which is not needed for Java.
        */
        int slot_count = vec.size();

        DecimalFormat format = new DecimalFormat("#." + StringUtils.repeat("0", prec));
        System.out.println();
        if (slot_count <= 2 * printSize) {
            System.out.print("    [");
            for (int i = 0; i < slot_count; i++) {
                System.out.print(" " + format.format(vec.get(i)) + ((i != slot_count - 1) ? "," : " ]\n"));
            }
        } else {
            vec.ensureCapacity(Math.max(vec.size(), 2 * printSize));
            System.out.print("    [");
            for (int i = 0; i < printSize; i++) {
                System.out.print(" " + format.format(vec.get(i)) + ",");
            }
            if (vec.size() > 2 * printSize) {
                System.out.print(" ...,");
            }
            for (int i = slot_count - printSize; i < slot_count; i++) {
                System.out.print(" " + format.format(vec.get(i)) + ((i != slot_count - 1) ? "," : " ]\n"));
            }
        }
        System.out.println();

        /*
        Restore the old std::cout formatting, which is not needed for Java.
        */
    }

    /**
     * Helper function: Convert a value into a hexadecimal string, e.g., uint64_t(17) --> "11".
     *
     * @param value the value.
     * @return the string.
     */
    static String uint64ToHexString(long value) {
        return UintCore.uintToHexString(new long[]{value}, 1);
    }
}
