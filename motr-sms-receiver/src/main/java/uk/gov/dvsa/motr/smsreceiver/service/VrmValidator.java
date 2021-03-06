package uk.gov.dvsa.motr.smsreceiver.service;

import java.util.regex.Pattern;

public class VrmValidator {

    private static final String REGISTRATION_EMPTY_MESSAGE = "Empty vrm";
    private static final String REGISTRATION_TOO_LONG_MESSAGE = "Vrm longer thanthan 14 characters";
    private static final String REGISTRATION_CAN_ONLY_CONTAIN_LETTERS_NUMBERS_AND_HYPHENS_MESSAGE = "Vrm contains invalid characters";
    private static final int REGISTRATION_MAX_LENGTH = 13;
    private static final String VALID_REGISTRATION_REGEX = "^[a-zA-Z0-9-]*$";

    private String message;

    public boolean isValid(String vehicleRegistration) {

        if (vehicleRegistration == null || "".equals(vehicleRegistration)) {
            this.message = REGISTRATION_EMPTY_MESSAGE;
            return false;
        }

        if (vehicleRegistration.length() > REGISTRATION_MAX_LENGTH) {
            this.message = REGISTRATION_TOO_LONG_MESSAGE;
            return false;
        }

        if (!Pattern.compile(VALID_REGISTRATION_REGEX).matcher(vehicleRegistration).matches()) {
            this.message = REGISTRATION_CAN_ONLY_CONTAIN_LETTERS_NUMBERS_AND_HYPHENS_MESSAGE;
            return false;
        }

        return true;
    }

    public String getMessage() {

        return this.message;
    }
}
