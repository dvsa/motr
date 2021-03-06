package uk.gov.dvsa.motr.web.cookie;

import org.junit.Before;
import org.junit.Test;

import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MotrSessionTest {

    private static final String CONFIRMATION_ID = "123ABC";
    private static final String VRM = "VRZ";
    private static final String EMAIL = "test@test.com";
    private static final String PHONE_NUMBER = "07801987627";
    private static final String CHANNEL = "email";
    private static final String EMAIL_CHANNEL = "email";
    private static final String TEXT_CHANNEL = "text";

    private MotrSession motrSession;

    @Before
    public void setUp() {
        motrSession = new MotrSession();
    }

    @Test
    public void isAllowedOnPageReturnsTrueWhenVrmAndEmailSessionEntered() {

        motrSession.setEmail(EMAIL);
        motrSession.setVrm(VRM);
        boolean actual = motrSession.isAllowedOnReviewPage();
        assertTrue(actual);
    }

    @Test
    public void isAllowedOnPageReturnsFalseWhenNoEmailAndNoPhoneNumberSessionEntered() {

        motrSession.setVrm(VRM);
        boolean actual = motrSession.isAllowedOnReviewPage();
        assertFalse(actual);
    }

    @Test
    public void isAllowedOnPageReturnsTrueWhenNoEmailAndPhoneNumberIsSet() {

        motrSession.setVrm(VRM);
        motrSession.setPhoneNumber(PHONE_NUMBER);
        boolean actual = motrSession.isAllowedOnReviewPage();
        assertTrue(actual);
    }

    @Test
    public void isAllowedOnPageReturnsFalseWhenNoVrmSessionEntered() {

        motrSession.setEmail(EMAIL);
        boolean actual = motrSession.isAllowedOnReviewPage();
        assertFalse(actual);
    }

    @Test
    public void isAllowedOnPageReturnsFalseWhenNoSessionEntered() {

        boolean actual = motrSession.isAllowedOnReviewPage();
        assertFalse(actual);
    }

    @Test
    public void isAllowedOnEmailPageReturnsFalseWhenNoSessionEntered() {

        boolean actual = motrSession.isAllowedOnEmailPage();
        assertFalse(actual);
    }

    @Test
    public void isAllowedOnEmailPageReturnsTrueWhenVrmSessionEntered() {

        motrSession.setVrm(VRM);
        boolean actual = motrSession.isAllowedOnEmailPage();
        assertTrue(actual);
    }

    @Test
    public void isAllowedOnSmsConfirmationCodePageReturnsFalseWhenNoSessionEntered() {

        boolean actual = motrSession.isAllowedOnSmsConfirmationCodePage();
        assertFalse(actual);
    }

    @Test
    public void isAllowedOnSmsConfirmationCodePageReturnsTrueWhenVrmAndPhoneNumberExistsInSession() {

        motrSession.setVrm(VRM);
        motrSession.setPhoneNumber(PHONE_NUMBER);
        boolean actual = motrSession.isAllowedOnSmsConfirmationCodePage();
        assertTrue(actual);
    }

    @Test
    public void isAllowedToPostOnSmsConfirmationCodePageReturnsFalseWhenConfirmationIdDoesNotExistInSession() {

        motrSession.setVrm(VRM);
        motrSession.setPhoneNumber(PHONE_NUMBER);
        boolean actual = motrSession.isAllowedToPostOnSmsConfirmationCodePage();
        assertFalse(actual);
    }

    @Test
    public void isAllowedToPostOnSmsConfirmationCodePageReturnsTrueWhenVrmAndPhoneNumberAndConfirmationIdExistsInSession() {

        motrSession.setVrm(VRM);
        motrSession.setPhoneNumber(PHONE_NUMBER);
        motrSession.setConfirmationId(CONFIRMATION_ID);
        boolean actual = motrSession.isAllowedToPostOnSmsConfirmationCodePage();
        assertTrue(actual);
    }

    @Test
    public void isAllowedToResendSmsConfirmationCodeReturnsFalseWhenConfirmationIdDoesNotExistInSession() {

        motrSession.setVrm(VRM);
        motrSession.setPhoneNumber(PHONE_NUMBER);
        boolean actual = motrSession.isAllowedToResendSmsConfirmationCode();
        assertFalse(actual);
    }

    @Test
    public void isAllowedToResendSmsConfirmationCodeReturnsTrueWhenConfirmationIdExistsInSession() {

        motrSession.setVrm(VRM);
        motrSession.setPhoneNumber(PHONE_NUMBER);
        motrSession.setConfirmationId(CONFIRMATION_ID);
        boolean actual = motrSession.isAllowedToResendSmsConfirmationCode();
        assertTrue(actual);
    }

    @Test
    public void getRegFromSessionReturnsRegWhenInSession() {

        motrSession.setVrm(VRM);
        String actual = motrSession.getVrmFromSession();
        assertEquals("VRZ", actual);
    }

    @Test
    public void getRegFromSessionReturnsEmptyStringWhenNoRegInSession() {

        String actual = motrSession.getVrmFromSession();
        assertEquals("", actual);
    }

    @Test
    public void getEmailFromSessionReturnsEmailWhenInSession() {

        motrSession.setEmail(EMAIL);
        String actual = motrSession.getEmailFromSession();
        assertEquals("test@test.com", actual);
    }

    @Test
    public void getConfirmationIdFromSessionReturnsConfirmationIdWhenInSession() {

        motrSession.setConfirmationId(CONFIRMATION_ID);
        String actual = motrSession.getConfirmationIdFromSession();
        assertEquals(CONFIRMATION_ID, actual);
    }

    @Test
    public void getPhoneNumberFromSessionReturnsPhoneNumberWhenInSession() {

        motrSession.setPhoneNumber(PHONE_NUMBER);
        String actual = motrSession.getPhoneNumberFromSession();
        assertEquals(PHONE_NUMBER, actual);
    }

    @Test
    public void getChannelFromSessionReturnsChannelWhenInSession() {

        motrSession.setChannel(CHANNEL);
        String actual = motrSession.getChannelFromSession();
        assertEquals(CHANNEL, actual);
    }

    @Test
    public void visitingFromReviewPageReturnsFalseWhenNotSet() {

        boolean actual = motrSession.visitingFromReviewPage();
        assertFalse(actual);
    }

    @Test
    public void visitingFromReviewPageReturnsTrueWhenSet() {

        motrSession.setVisitingFromReview(true);
        boolean actual = motrSession.visitingFromReviewPage();
        assertTrue(actual);
    }

    @Test
    public void visitingFromContactEntryPageReturnsFalseWhenNotSet() {

        boolean actual = motrSession.visitingFromContactEntryPage();
        assertFalse(actual);
    }

    @Test
    public void visitingFromContactEntryPageReturnsTrueWhenSet() {

        motrSession.setVisitingFromContactEntry(true);
        boolean actual = motrSession.visitingFromContactEntryPage();
        assertTrue(actual);
    }

    @Test
    public void getEmailFromSessionReturnsEmptyStringWhenNoEmailInSession() {

        String actual = motrSession.getEmailFromSession();
        assertEquals("", actual);
    }

    @Test
    public void getPhoneNumberFromSessionReturnsEmptyStringWhenNoEmailInSession() {

        String actual = motrSession.getPhoneNumberFromSession();
        assertEquals("", actual);
    }

    @Test
    public void getChannelFromSessionReturnsEmptyStringWhenNoEmailInSession() {

        String actual = motrSession.getChannelFromSession();
        assertEquals("", actual);
    }

    @Test
    public void isUsingEmailChannelReturnsTrueWhenUsingEmailChannel() {

        motrSession.setChannel(EMAIL_CHANNEL);
        assertTrue(motrSession.isUsingEmailChannel());
    }

    @Test
    public void isUsingSmsChannelReturnsFalseWhenUsingEmailChannel() {

        motrSession.setChannel(EMAIL_CHANNEL);
        assertFalse(motrSession.isUsingSmsChannel());
    }

    @Test
    public void isUsingSmsChannelReturnsTrueWhenUsingTextChannel() {

        motrSession.setChannel(TEXT_CHANNEL);
        assertTrue(motrSession.isUsingSmsChannel());
    }

    @Test
    public void isUsingEmailChannelReturnsFalseWhenUsingSmsChannel() {

        motrSession.setChannel(TEXT_CHANNEL);
        assertFalse(motrSession.isUsingEmailChannel());
    }

    @Test
    public void whenVehicleDetailsIsSetItCanBeRetrievedCorrectly() {

        VehicleDetails vehicleDetails = new VehicleDetails();
        vehicleDetails.setMake("TEST-MAKE");

        motrSession.setVehicleDetails(vehicleDetails);
        VehicleDetails actual = motrSession.getVehicleDetailsFromSession();
        assertEquals("TEST-MAKE", actual.getMake());
    }

    @Test
    public void isAllowedOnChannelSelectionPageReturnsFalseWithEmptyVrm() {

        assertFalse(motrSession.isAllowedOnChannelSelectionPage());
    }

    @Test
    public void isAllowedOnChannelSelectionPageReturnsTrueWithVrm() {

        motrSession.setVrm(VRM);

        assertTrue(motrSession.isAllowedOnChannelSelectionPage());
    }

    @Test
    public void isAllowedOnUnknownTestDatePage_trueIfExpiryDateNull() {
        motrSession.setVehicleDetails(new VehicleDetails().setMotExpiryDate(null));

        assertTrue(motrSession.isAllowedOnUnknownTestDatePage());
    }

    @Test
    public void isAllowedOnUnknownTestDatePage_falseIfExpiryDateSet() {
        motrSession.setVehicleDetails(new VehicleDetails().setMotExpiryDate(LocalDate.parse("2018-06-14")));

        assertFalse(motrSession.isAllowedOnUnknownTestDatePage());
    }
}
