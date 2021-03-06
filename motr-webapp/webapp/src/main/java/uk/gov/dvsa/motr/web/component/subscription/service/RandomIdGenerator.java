package uk.gov.dvsa.motr.web.component.subscription.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

class RandomIdGenerator {

    public String generateId() {

        return encodeBase64URLSafeString(cryptoStrongRandomBytes(16));
    }

    private byte[] cryptoStrongRandomBytes(int size) {

        try {
            byte[] randomKey = new byte[size];
            SecureRandom.getInstanceStrong().nextBytes(randomKey);

            return randomKey;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot generate random id", e);
        }
    }
}
