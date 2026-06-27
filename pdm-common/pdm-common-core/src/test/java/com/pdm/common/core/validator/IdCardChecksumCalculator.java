package com.pdm.common.core.validator;

public class IdCardChecksumCalculator {
    private static final int[] WEIGHT = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
    private static final char[] CHECK_CODE = { '1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2' };

    public static char calculateChecksum(String id17) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id17.charAt(i) - '0') * WEIGHT[i];
        }
        return CHECK_CODE[sum % 11];
    }

    public static void main(String[] args) {
        String[] ids = { "11010119900307663", // VALID_MALE_1990
                "32010019951212002", // VALID_FEMALE_1995
                "44010019880702001", // VALID_FEMALE_1988
        };

        for (String id17 : ids) {
            char checksum = calculateChecksum(id17);
            System.out.println(id17 + checksum);
        }
    }
}
