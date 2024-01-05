package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;
import org.apache.commons.lang3.StringUtils;

/**
 * Examples Utils.
 *
 * @author Liqiang Peng
 * @date 2023/12/22
 */
public class ExamplesUtils {

    /**
     * Helper function: Prints the name of the example in a fancy banner.
     *
     * @param title the title.
     */
    public static void printExampleBanner(String title) {
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
    public static void printParameters(SealContext context) {
        SealContext.ContextData contextData = context.keyContextData();

        /*
        Which scheme are we using?
        */
        String schemeName;
        switch (contextData.parms().scheme()) {
            case BFV:
                schemeName = "BFV";
                break;
            case CKKS:
                schemeName = "CKKS";
                break;
            case BGV:
                schemeName = "BGV";
                break;
            default:
                throw new IllegalArgumentException("unsupported scheme");
        }
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
    public static void printMatrix(long[] matrix, int rowSize) {
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
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ((i != rowSize - 1)? "," : " ]\n"));
        }
        System.out.print("    [");
        for (int i = rowSize; i < rowSize + printSize; i++) {
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ",");
        }
        System.out.print(StringUtils.rightPad(" ...,", 3));
        for (int i = 2 * rowSize - printSize; i < 2 * rowSize; i++) {
            System.out.print(StringUtils.leftPad(String.valueOf(matrix[i]), 3) + ((i != 2 * rowSize - 1)? "," : " ]\n"));
        }
        System.out.print("\n");
    }

    /**
     * Helper function: Print line number.
     *
     * @param lineNumber the line number.
     */
    public static void printLine(int lineNumber) {
        System.out.print("Line " + StringUtils.leftPad(String.valueOf(lineNumber), 3) + " --> ");
    }

    /**
     * Helper function: Convert a value into a hexadecimal string, e.g., uint64_t(17) --> "11".
     *
     * @param value the value.
     * @return the string.
     */
    public static String uint64ToHexString(long value) {
        return UintCore.uintToHexString(new long[]{value}, 1);
    }
}
