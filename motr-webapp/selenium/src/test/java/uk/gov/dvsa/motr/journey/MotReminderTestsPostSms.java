package uk.gov.dvsa.motr.journey;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import uk.gov.dvsa.motr.base.BaseTest;
import uk.gov.dvsa.motr.helper.RandomGenerator;
import uk.gov.dvsa.motr.ui.page.EmailConfirmationPendingPage;
import uk.gov.dvsa.motr.ui.page.EmailPage;
import uk.gov.dvsa.motr.ui.page.PhoneConfirmPage;
import uk.gov.dvsa.motr.ui.page.PhoneNumberEntryPage;
import uk.gov.dvsa.motr.ui.page.ReviewPage;
import uk.gov.dvsa.motr.ui.page.SubscriptionConfirmationPage;
import uk.gov.dvsa.motr.ui.page.TestExpiredPage;
import uk.gov.dvsa.motr.ui.page.UnknownTestDueDatePage;
import uk.gov.dvsa.motr.ui.page.UnsubscribeConfirmationPage;
import uk.gov.dvsa.motr.ui.page.UnsubscribeErrorPage;
import uk.gov.dvsa.motr.ui.page.VrmPage;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MotReminderTestsPostSms extends BaseTest {

    public static final String CONFIRMATION_PAGE_TITLE = "You’ve signed up for an MOT reminder";
    public static final String CONFIRMATION_PAGE_TITLE_FOR_HGV_PSV = "You’ve signed up for annual test (MOT) reminders";

    @Test(description = "Owner of a vehicle with a expired test cannot subscribe to reminder and is redirected to TestExpired error page",
             groups = {"PostSms"})
    public void tryToSubscribeWithExpiredTest() throws IOException, InterruptedException {

        //Given I am a MOTR user
        //When I enter the vehicle vrm which test has already expired
        TestExpiredPage testExpiredPage = motReminder.enterVrmAndTryToSubscribe("TEST-EXPIRED");

        //Then I should be redirected to TestExpired page
        //And proper elements are displayed
        assertEquals(testExpiredPage.getRemindersLink().getText(), "Sign up a different vehicle");
        assertEquals(testExpiredPage.getPublicationsLink().getText(), "Check if your vehicle doesn't need an annual test");
    }

    @Test(dataProvider = "dataProviderCreateEmailMotReminderForMyVehicle",
            description = "Owner of a vehicle with a mot is able to set up an email MOT reminder with their VRM and email and " +
                    "unsubscribe from it",
            groups = {"PostSms"})
    public void createEmailMotReminderForMyVehicleThenUnsubscribe(String vrm, String email) throws IOException, InterruptedException {

        //Given I am a vehicle owner on the MOTR start page
        //When I enter the vehicle vrm and my email address
        //And I confirm my email address
        motReminder.subscribeToReminderAndConfirmEmailPostSms(vrm, email);

        //When I select to unsubscribe from an email reminder
        //And confirm that I would like to unsubscribe
        UnsubscribeConfirmationPage unsubscribeConfirmed = motReminder.unsubscribeFromReminder(vrm, email);

        //Then my MOT reminder subscription has been cancelled
        assertEquals(unsubscribeConfirmed.getBannerTitle(), "You’ve unsubscribed");

        //And I am shown a link to complete the unsubscribe survey
        assertTrue(unsubscribeConfirmed.isSurveyLinkDisplayed());
    }

    @Test(dataProvider = "dataProviderCreateEmailMotReminderForMyVehicle",
            description = "Reminder subscriber with an active email subscription creates another email subscription with the same VRM and" +
                    " email does not need to confirm their email again",
            groups = {"PostSms"})
    public void createDuplicateEmailMotReminderDoesNotNeedToConfirmEmailAddressAgain(String vrm, String email)
            throws IOException, InterruptedException {

        // Given I am a user of the MOT reminders service with an active subscription
        motReminder.subscribeToReminderAndConfirmEmailPostSms(vrm, email);

        // When I create another MOT reminder subscription with the same VRM and email
        // Then I do not need to confirm my email address and am taken directly to the subscription confirmed page
        motReminder.enterAndConfirmPendingReminderDetailsSecondTimePostSms(vrm, email);
    }

    @Test(dataProvider = "dataProviderCreateEmailMotReminderForMyVehicle",
            description = "Email reminder subscriber with multiple pending email subscriptions should use the same confirmatio link ",
            groups = {"PostSms"})
    public void userWithDuplicatePendingEmailMotSubscriptionsIsGivenSameConfirmationLink(
            String vrm, String email
    ) throws IOException, InterruptedException {

        // Given I am a user of the MOT reminders service with a pending subscription
        motReminder.enterAndConfirmPendingReminderDetailsPostSms(vrm, email);
        String oldConfirmationId = motReminder.subscriptionDb.findConfirmationIdByVrmAndEmail(vrm, email);

        // When I create another MOT reminder subscription with the same VRM and email
        // And I select an old confirm email link
        motReminder.enterAndConfirmPendingReminderDetailsPostSms(vrm, email);
        String newConfirmationId = motReminder.subscriptionDb.findConfirmationIdByVrmAndEmail(vrm, email);

        // Then the two confirmation email links should be identical
        assertEquals(oldConfirmationId, newConfirmationId);

        // And I can still confirm my email address using newest email
        motReminder.navigateToEmailConfirmationPage(newConfirmationId);
    }

    @Test(dataProvider = "dataProviderCreateEmailMotReminderForMyVehicle",
            description = "A user who has previously unsubscribed from an email reminder will be displayed the unsubscribe error page",
            groups = {"PostSms"})
    public void emailReminderThatHasBeenUnsubscribedDisplaysErrorPage(String vrm, String email) {

        //Given I am a user of the MOT reminders service with an active subscription
        //When I unsubscribe from the email reminder via the unsubscribe link
        motReminder.subscribeToReminderAndConfirmEmailPostSms(vrm, email);
        String subscriptionId = motReminder.subscriptionDb.findUnsubscribeIdByVrmAndEmail(vrm, email);
        motReminder.unsubscribeFromReminder(vrm, email);

        //And select the unsubscribe link again
        UnsubscribeErrorPage errorPage = motReminder.navigateToUnsubscribeExpectingErrorPage(subscriptionId);

        //Then I receive a error message informing me that I have already unsubscribed
        assertEquals(errorPage.getErrorMessageText(), "You’ve already unsubscribed or the link hasn’t worked.");
    }

    @Test(description = "Owner of a vehicle with a mot can change their email when creating an MOT email reminder",
            groups = {"PostSms"})
    public void canChangeEmailFromReviewWhenCreatingEmailReminder() {

        //Given I am a vehicle owner on the MOTR start page
        //When I enter the vehicle vrm and my email address
        ReviewPage reviewPage = motReminder.enterReminderDetailsSmsToggleOnUsingEmailChannel(RandomGenerator.generateVrm(),
                RandomGenerator.generateEmail());

        //And I update my email address
        EmailPage emailPageFromReview = reviewPage.clickChangeEmail();
        ReviewPage reviewPageSubmit = emailPageFromReview.enterEmailAddress(RandomGenerator.generateEmail());

        //Then my mot reminder is set up successfully with the updated email address
        EmailConfirmationPendingPage confirmPage = reviewPageSubmit.confirmSubscriptionDetailsOnEmailChannel();
        assertEquals(confirmPage.getTitle(), "One more step");
    }

    @Test(description = "Owner of a vehicle with a mot can change their vrm when creating MOT reminder",
            groups = {"PostSms"})
    public void canChangeVrmFromReviewWhenCreatingReminder() {

        //Given I am a vehicle owner on the MOTR start page
        //When I enter the vehicle vrm and my email address
        ReviewPage reviewPage = motReminder.enterReminderDetailsSmsToggleOnUsingEmailChannel(RandomGenerator.generateVrm(),
                RandomGenerator.generateEmail());

        //And I update my vehicle vrm
        VrmPage vrmPageFromReview = reviewPage.clickChangeVrm();
        ReviewPage reviewPageSubmit = vrmPageFromReview.enterVrmExpectingReturnToReview(RandomGenerator.generateVrm());

        //Then my mot reminder is set up successfully with the updated vehicle vrm
        EmailConfirmationPendingPage confirmPage = reviewPageSubmit.confirmSubscriptionDetailsOnEmailChannel();
        assertEquals(confirmPage.getTitle(), "One more step");
    }

    @Test(description = "Owner of a new vehicle with no mot is able to set up a MOT email reminder with their VRM and email",
            groups = {"PostSms"})
    public void canCreateAnEmailReminderWhenVehicleDoesNotHaveAnMotYet() {

        //Given I am an owner of a new vehicle
        //When I enter the vehicle vrm and my email address
        //And I confirm my email address
        SubscriptionConfirmationPage subscriptionConfirmationPage =
                motReminder.subscribeToReminderAndConfirmEmailPostSms(
                        RandomGenerator.generateDvlaVrm(), RandomGenerator.generateEmail());

        //Then the confirmation page is displayed confirming my active reminder subscription
        assertEquals(subscriptionConfirmationPage.getHeaderTitle(), CONFIRMATION_PAGE_TITLE);
    }

    @Test(description = "Owner of a vehicle with a mot is able to set up a MOT reminder with their VRM and mobile number",
            dataProvider = "dataProviderCreateSmsMotReminderForMyVehicle",
            groups = {"PostSms"})
    public void createSmsMotReminderForMyVehicleUsingMobile(String vrm, String mobileNumber) {

        //Given I am a vehicle owner on the MOTR start page
        //When I enter the vehicle vrm and my mobile number
        //And I can confirm my mobile number via the sent code
        SubscriptionConfirmationPage subscriptionConfirmationPage =
                motReminder.subscribeToReminderAndConfirmMobileNumber(vrm, mobileNumber);

        //Then the confirmation page is displayed confirming my active reminder subscription
        assertEquals(subscriptionConfirmationPage.getHeaderTitle(), CONFIRMATION_PAGE_TITLE);
    }

    @Test(description = "Owner of a new vehicle with a mot is able to set up a MOT reminder with their VRM and mobile number",
            groups = {"PostSms"})
    public void canCreateSmsReminderWhenVehicleDoesNotHaveAnMotYet() {

        //Given I am an owner of a new vehicle
        //When I enter the new vehicle vrm and my mobile number
        //And I confirm my mobile number
        SubscriptionConfirmationPage subscriptionConfirmationPage =
                motReminder.subscribeToReminderAndConfirmMobileNumber(
                        RandomGenerator.generateDvlaVrm(), RandomGenerator.generateMobileNumber());

        //Then the confirmation page is displayed confirming my active reminder subscription
        assertEquals(subscriptionConfirmationPage.getHeaderTitle(), CONFIRMATION_PAGE_TITLE);
    }

    @Test(description = "Owner of a new vehicle with a mot is able to set up a MOT reminder for " +
            "HGV vehicle with their VRM and mobile number",
            groups = {"PostSms"})
    public void canCreateSmsReminderForHgvVehicleWhenVehicleDoesNotHaveAnMotYet() {

        //Given I am an owner of a new HGV vehicle
        //When I enter the new vehicle vrm and my mobile number
        //And I confirm my mobile number
        SubscriptionConfirmationPage subscriptionConfirmationPage =
                motReminder.subscribeToReminderAndConfirmMobileNumber(
                        "HGV-NOTEST", RandomGenerator.generateMobileNumber());

        //Then the confirmation page is displayed with correct header confirming my active reminder subscription
        assertEquals(subscriptionConfirmationPage.getHeaderTitle(), CONFIRMATION_PAGE_TITLE_FOR_HGV_PSV);
    }

    @Test(description = "Owner of a vehicle with a mot can change their mobile number when creating MOT reminder",
            dataProvider = "dataProviderCreateSmsMotReminderForMyVehicle",
            groups = {"PostSms"})
    public void canChangeMobileNumberFromReviewWhenCreatingReminder(String vrm, String mobileNumber) {

        //Given I am a vehicle owner on the MOTR start page
        //When I enter the vehicle vrm and mobile number
        ReviewPage reviewPage = motReminder.enterReminderDetailsUsingMobileChannel(vrm, mobileNumber);

        //And I update my mobile number
        String newMobileNumber = RandomGenerator.generateMobileNumber();
        PhoneNumberEntryPage phoneNumberEntryPageFromReview = reviewPage.clickChangeMobileNumber();
        ReviewPage reviewPageSubmit = phoneNumberEntryPageFromReview.enterPhoneNumber(newMobileNumber);

        //Then confirm mobile number
        PhoneConfirmPage phoneConfirmPage = reviewPageSubmit.confirmSubscriptionDetailsOnMobileChannel();
        SubscriptionConfirmationPage subscriptionConfirmationPage =
                phoneConfirmPage.enterConfirmationCode(motReminder.smsConfirmationCode(vrm, newMobileNumber));

        //Then my mot reminder is set up successfully with the updated mobile number
        assertEquals(subscriptionConfirmationPage.getHeaderTitle(), CONFIRMATION_PAGE_TITLE);
    }

    @Test(description = "Reminder subscriber with one active SMS subscription creates another subscription with the same VRM and " +
            "mobile number does not need to confirm their number again",
            dataProvider = "dataProviderCreateSmsMotReminderForMyVehicle",
            groups = {"PostSms"})
    public void createDuplicateMotSmsReminderDoesNotNeedToConfirmMobileNumberAgain(String vrm, String mobileNumber) {

        // Given I am a user of the MOT reminders service with an active SMS subscription
        motReminder.subscribeToReminderAndConfirmMobileNumber(vrm, mobileNumber);

        // When I create another MOT reminder subscription with the same VRM and mobile number
        SubscriptionConfirmationPage subscriptionConfirmationPage =
                motReminder.enterAndConfirmReminderDetailsSecondTimeOnMobileChannel(vrm, mobileNumber);

        // Then I do not need to confirm my mobile number again and am taken directly to the subscription confirmed page
        assertEquals(subscriptionConfirmationPage.getHeaderTitle(), CONFIRMATION_PAGE_TITLE);
    }

    @Test(description = "If we were unable to determine first annual test due date of HGV or PSV, we should redirect " +
            "to a page that informs about it", groups = {"PostSms"})
    public void searchingForHgvPsvWithUnknownTestDueDateRedirectsToProperInformationPage() {
        // Given I am an owner of HGV or PSV
        // And due date of its first annual test is unknown
        UnknownTestDueDatePage unknownDatePage = motReminder.enterVrmWithUnknownExpiryDate("PSV-UNKNEXP");

        //Then I should be redirected to UnknownTestDueDatePage
        //And a link pointing to DVSA contact info should be displayed
        assertEquals(unknownDatePage.getContactDvsaLink().getText(), "Contact DVSA");
    }

    @DataProvider(name = "dataProviderCreateEmailMotReminderForMyVehicle")
    public Object[][] dataProviderCreateEmailMotReminderForMyVehicle() throws IOException {

        return new Object[][]{{RandomGenerator.generateVrm(), RandomGenerator.generateEmail()}};
    }

    @DataProvider(name = "dataProviderCreateSmsMotReminderForMyVehicle")
    public Object[][] dataProviderCreateSmsMotReminderForMyVehicle() throws IOException {

        return new Object[][]{{RandomGenerator.generateVrm(), RandomGenerator.generateMobileNumber()}};
    }
}
