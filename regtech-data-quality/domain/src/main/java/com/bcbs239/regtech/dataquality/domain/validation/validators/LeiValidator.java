package com.bcbs239.regtech.dataquality.domain.validation.validators;

/**
 * Utility class for validating Legal Entity Identifier (LEI) codes according to ISO 17442 standard.
 */
public class LeiValidator {

    /**
     * Validate if a LEI code has the correct format.
     * LEI format: 20 alphanumeric characters
     * Structure: 4-character LOU identifier + 2-character reserved area + 12-character entity identifier + 2-character check digits
     *
     * @param leiCode the LEI code to validate
     * @return true if the LEI code has valid format, false otherwise
     */
    public static boolean isValidFormat(String leiCode) {
        if (leiCode == null || leiCode.trim().isEmpty()) {
            return false;
        }

        String normalizedLei = leiCode.trim().toUpperCase();

        // Check length: exactly 20 characters
        if (normalizedLei.length() != 20) {
            return false;
        }

        // Check format: alphanumeric characters only
        if (!normalizedLei.matches("^[A-Z0-9]{20}$")) {
            return false;
        }

        // Validate check digits using MOD-97 algorithm
        return isValidCheckDigits(normalizedLei);
    }

    /**
     * Validate the check digits of a LEI code using the MOD-97 algorithm.
     *
     * @param leiCode the LEI code to validate (must be 20 characters, alphanumeric)
     * @return true if check digits are valid, false otherwise
     */
    private static boolean isValidCheckDigits(String leiCode) {
        try {
            // Extract the first 18 characters (without check digits)
            String leiWithoutCheckDigits = leiCode.substring(0, 18);

            // Extract the check digits
            String checkDigits = leiCode.substring(18, 20);

            // Convert letters to numbers (A=10, B=11, ..., Z=35)
            StringBuilder numericString = new StringBuilder();
            for (char c : leiWithoutCheckDigits.toCharArray()) {
                if (Character.isDigit(c)) {
                    numericString.append(c);
                } else {
                    // Convert letter to number (A=10, B=11, etc.)
                    numericString.append(c - 'A' + 10);
                }
            }

            // Append "00" for check digit calculation
            numericString.append("00");

            // Calculate MOD-97
            int remainder = calculateMod97(numericString.toString());

            // Check digits should make the remainder equal to 1
            int expectedCheckDigits = 98 - remainder;

            // Format expected check digits with leading zero if necessary
            String expectedCheckDigitsStr = String.format("%02d", expectedCheckDigits);

            return checkDigits.equals(expectedCheckDigitsStr);

        } catch (Exception e) {
            // If any error occurs during validation, consider it invalid
            return false;
        }
    }

    /**
     * Calculate MOD-97 for a numeric string.
     * This handles large numbers by processing them in chunks.
     *
     * @param numericString the numeric string to process
     * @return the MOD-97 result
     */
    private static int calculateMod97(String numericString) {
        int remainder = 0;

        // Process the string in chunks to handle large numbers
        for (int i = 0; i < numericString.length(); i++) {
            remainder = (remainder * 10 + Character.getNumericValue(numericString.charAt(i))) % 97;
        }

        return remainder;
    }

    /**
     * Get the normalized LEI code (uppercase, trimmed).
     *
     * @param leiCode the LEI code to normalize
     * @return the normalized LEI code, or null if invalid
     */
    public static String normalize(String leiCode) {
        if (!isValidFormat(leiCode)) {
            return null;
        }
        return leiCode.trim().toUpperCase();
    }

    /**
     * Extract the LOU (Local Operating Unit) identifier from a LEI code.
     *
     * @param leiCode the LEI code
     * @return the 4-character LOU identifier, or null if LEI is invalid
     */
    public static String extractLOU(String leiCode) {
        if (!isValidFormat(leiCode)) {
            return null;
        }
        return leiCode.trim().toUpperCase().substring(0, 4);
    }

    /**
     * Extract the entity identifier from a LEI code.
     *
     * @param leiCode the LEI code
     * @return the 12-character entity identifier, or null if LEI is invalid
     */
    public static String extractEntityIdentifier(String leiCode) {
        if (!isValidFormat(leiCode)) {
            return null;
        }
        return leiCode.trim().toUpperCase().substring(6, 18);
    }

    /**
     * Check if a LEI code is from a specific LOU.
     *
     * @param leiCode the LEI code to check
     * @param louCode the 4-character LOU code to match
     * @return true if the LEI is from the specified LOU, false otherwise
     */
    public static boolean isFromLOU(String leiCode, String louCode) {
        String extractedLOU = extractLOU(leiCode);
        return extractedLOU != null && extractedLOU.equals(louCode.trim().toUpperCase());
    }
}