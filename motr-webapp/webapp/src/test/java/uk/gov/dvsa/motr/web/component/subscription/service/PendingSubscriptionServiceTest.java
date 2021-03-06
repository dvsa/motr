package uk.gov.dvsa.motr.web.component.subscription.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import uk.gov.dvsa.motr.notifications.service.NotifyService;
import uk.gov.dvsa.motr.vehicledetails.MotIdentification;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetailsClient;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.component.subscription.helper.UrlHelper;
import uk.gov.dvsa.motr.web.component.subscription.model.ContactDetail;
import uk.gov.dvsa.motr.web.component.subscription.model.PendingSubscription;
import uk.gov.dvsa.motr.web.component.subscription.model.Subscription;
import uk.gov.dvsa.motr.web.component.subscription.persistence.PendingSubscriptionRepository;
import uk.gov.dvsa.motr.web.component.subscription.persistence.SubscriptionRepository;
import uk.gov.dvsa.motr.web.component.subscription.response.PendingSubscriptionServiceResponse;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static java.util.Optional.empty;

public class PendingSubscriptionServiceTest {

    private final PendingSubscriptionRepository pendingSubscriptionRepository = mock(PendingSubscriptionRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final NotifyService notifyService = mock(NotifyService.class);
    private final UrlHelper urlHelper = mock(UrlHelper.class);
    private final VehicleDetailsClient client = mock(VehicleDetailsClient.class);

    private static final String TEST_VRM = "TEST-REG";
    private static final String EMAIL = "test@test.com";
    private static final String MOBILE = "07912345678";
    private static final String CONFIRMATION_ID = "Asd";
    private static final String CONFIRMATION_LINK = "CONFIRMATION_LINK";
    private static final String EMAIL_ALREADY_CONFIRMED_LINK = "ALREADY_CONFIRMED_LINK";
    private static final String PHONE_ALREADY_CONFIRMED_LINK = "PHONE_ALREADY_CONFIRMED_LINK";
    private static final String CONFIRMATION_PENDING_LINK = "CONFIRMATION_PENDING_LINK";
    private static final String TEST_MOT_TEST_NUMBER = "123456";
    private static final String TEST_DVLA_ID = "3456789";
    private static final Subscription.ContactType CONTACT_TYPE_EMAIL = Subscription.ContactType.EMAIL;
    private static final Subscription.ContactType CONTACT_TYPE_MOBILE = Subscription.ContactType.MOBILE;
    private static final ContactDetail CONTACT_EMAIL = new ContactDetail(EMAIL, CONTACT_TYPE_EMAIL);
    private static final ContactDetail CONTACT_MOBILE = new ContactDetail(MOBILE, CONTACT_TYPE_MOBILE);

    private PendingSubscriptionService subscriptionService;

    @Before
    public void setUp() {

        this.subscriptionService = new PendingSubscriptionService(
                pendingSubscriptionRepository,
                subscriptionRepository,
                notifyService,
                urlHelper,
                client
        );
        this.subscriptionService.setRandomIdGenerator(new RandomIdGenerator() {

            @Override
            public String generateId() {
                return String.valueOf(new Random().nextInt());
            }
        });

        when(urlHelper.confirmSubscriptionLink(CONFIRMATION_ID)).thenReturn(CONFIRMATION_LINK);
        when(urlHelper.emailConfirmedNthTimeLink()).thenReturn(EMAIL_ALREADY_CONFIRMED_LINK);
        when(urlHelper.phoneConfirmedNthTimeLink()).thenReturn(PHONE_ALREADY_CONFIRMED_LINK);
        when(urlHelper.emailConfirmationPendingLink()).thenReturn(CONFIRMATION_PENDING_LINK);
    }

    @Test
    public void createPendingSubscriptionWithEmailCallsDbToSaveDetailsAndSendsNotification() throws Exception {
        VehicleDetails vehicleDetails = getMockVehicleDetails();

        when(client.fetchByVrm(eq(TEST_VRM))).thenReturn(Optional.of(vehicleDetails));

        withExpectedSubscription(empty(), EMAIL);
        doNothing().when(notifyService).sendEmailAddressConfirmationEmail(EMAIL, CONFIRMATION_LINK, "TEST-MAKE TEST-MODEL, ");

        this.subscriptionService.createPendingSubscription(TEST_VRM, EMAIL, vehicleDetails.getMotExpiryDate(), CONFIRMATION_ID,
                new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID), CONTACT_TYPE_EMAIL, vehicleDetails.getVehicleType());

        verify(pendingSubscriptionRepository, times(1)).save(any(PendingSubscription.class));
        verify(notifyService, times(1)).sendEmailAddressConfirmationEmail(
                EMAIL,
                CONFIRMATION_LINK,
                "TEST-MAKE TEST-MODEL, TEST-REG"
        );
    }

    @Test
    public void createPendingSubscriptionWithMobileCallsDbToSaveDetailsAndDoesNotSendNotification() throws Exception {
        VehicleDetails vehicleDetails = getMockVehicleDetails();
        when(client.fetchByVrm(eq(TEST_VRM))).thenReturn(Optional.of(vehicleDetails));

        withExpectedSubscription(empty(), MOBILE);

        this.subscriptionService.createPendingSubscription(TEST_VRM, MOBILE, vehicleDetails.getMotExpiryDate(), CONFIRMATION_ID,
                new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID), CONTACT_TYPE_MOBILE, vehicleDetails.getVehicleType());

        verify(pendingSubscriptionRepository, times(1)).save(any(PendingSubscription.class));
        verifyZeroInteractions(notifyService);
    }

    @Test(expected = RuntimeException.class)
    public void whenDbSaveFailsConfirmationEmailIsNotSent() throws Exception {

        withExpectedSubscription(empty(), EMAIL);
        doThrow(new RuntimeException()).when(pendingSubscriptionRepository).save(any(PendingSubscription.class));
        LocalDate date = LocalDate.now();

        this.subscriptionService.createPendingSubscription(TEST_VRM, EMAIL, date, CONFIRMATION_ID,
                new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID), CONTACT_TYPE_EMAIL, VehicleType.MOT);
        verify(pendingSubscriptionRepository, times(1)).save(any(PendingSubscription.class));
        verifyZeroInteractions(notifyService);
    }

    @Test(expected = RuntimeException.class)
    public void whenPublicApiDataConfirmationFailsSubscriptionIsNotSaved() throws Exception {
        when(client.fetchByVrm(any())).thenReturn(Optional.empty());

        LocalDate date = LocalDate.now();

        this.subscriptionService.createPendingSubscription(TEST_VRM, EMAIL, date, CONFIRMATION_ID,
                new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID), CONTACT_TYPE_EMAIL, VehicleType.MOT);
        verify(pendingSubscriptionRepository, times(0)).save(any());
        verifyZeroInteractions(notifyService);
    }

    @Test
    public void handleSubscriptionWithExistingSubscriptionWillUpdateTestDueDateAndVehicleType() throws Exception {

        String originalEmail = EMAIL;
        String newEmailWithDifferentCase = originalEmail.toUpperCase();

        withExpectedSubscription(Optional.of(new Subscription()), originalEmail);
        LocalDate date = LocalDate.now();
        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        ContactDetail contactDetail = new ContactDetail(newEmailWithDifferentCase, CONTACT_TYPE_EMAIL);
        PendingSubscriptionServiceResponse pendingSubscriptionResponse = this.subscriptionService.handlePendingSubscriptionCreation(
                TEST_VRM, contactDetail, date, new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID), VehicleType.PSV);

        verify(subscriptionRepository, times(1)).save(subscriptionArgumentCaptor.capture());
        assertEquals(subscriptionArgumentCaptor.getValue().getMotDueDate(), date);
        assertEquals(subscriptionArgumentCaptor.getValue().getVehicleType(), VehicleType.PSV);
        assertEquals(EMAIL_ALREADY_CONFIRMED_LINK, pendingSubscriptionResponse.getRedirectUri());
        assertNull(pendingSubscriptionResponse.getConfirmationId());
        verifyZeroInteractions(pendingSubscriptionRepository);
    }

    @Test
    public void handleSubscriptionWithExistingMobileSubscriptionWillUpdateMotExpiryDateAndReturnCorrectRedirectUri() throws Exception {

        withExpectedSubscription(Optional.of(new Subscription()), MOBILE);
        LocalDate date = LocalDate.now();
        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        PendingSubscriptionServiceResponse pendingSubscriptionResponse = this.subscriptionService.handlePendingSubscriptionCreation(
                TEST_VRM, CONTACT_MOBILE, date, new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID), VehicleType.PSV);

        verify(subscriptionRepository, times(1)).save(subscriptionArgumentCaptor.capture());
        assertEquals(subscriptionArgumentCaptor.getValue().getMotDueDate(), date);
        assertEquals(subscriptionArgumentCaptor.getValue().getVehicleType(), VehicleType.PSV);
        assertEquals(PHONE_ALREADY_CONFIRMED_LINK, pendingSubscriptionResponse.getRedirectUri());
        assertNull(pendingSubscriptionResponse.getConfirmationId());
        verifyZeroInteractions(pendingSubscriptionRepository);
    }

    @Test
    public void handleSubscriptionWithEmailContactWillCreateNewPendingSubscription() throws Exception {
        VehicleDetails vehicleDetails = getMockVehicleDetails();

        when(client.fetchByVrm(eq(TEST_VRM))).thenReturn(Optional.of(vehicleDetails));
        withExpectedSubscription(empty(), EMAIL);
        when(pendingSubscriptionRepository.findByVrmAndContactDetails(TEST_VRM, EMAIL)).thenReturn(Optional.empty());
        ArgumentCaptor<PendingSubscription> pendingSubscriptionArgumentCaptor = ArgumentCaptor.forClass(PendingSubscription.class);

        PendingSubscriptionServiceResponse pendingSubscriptionResponse = this.subscriptionService.handlePendingSubscriptionCreation(
                TEST_VRM, CONTACT_EMAIL, vehicleDetails.getMotExpiryDate(), new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID),
                vehicleDetails.getVehicleType());

        verify(pendingSubscriptionRepository, times(1)).save(pendingSubscriptionArgumentCaptor.capture());
        verify(notifyService, times(1)).sendEmailAddressConfirmationEmail(any(), any(), any());
        assertEquals(pendingSubscriptionArgumentCaptor.getValue().getMotDueDate(), vehicleDetails.getMotExpiryDate());
        assertEquals(pendingSubscriptionArgumentCaptor.getValue().getContactDetail().getValue(), EMAIL);
        assertEquals(pendingSubscriptionArgumentCaptor.getValue().getVrm(), TEST_VRM);
        assertEquals(CONFIRMATION_PENDING_LINK, pendingSubscriptionResponse.getRedirectUri());
        assertNull(pendingSubscriptionResponse.getConfirmationId());
    }

    @Test
    public void handleSubscriptionWithMobileContactWillCreateNewPendingSubscription() throws Exception {

        VehicleDetails vehicleDetails = getMockVehicleDetails();

        when(client.fetchByVrm(eq(TEST_VRM))).thenReturn(Optional.of(vehicleDetails));
        withExpectedSubscription(empty(), MOBILE);
        LocalDate date = LocalDate.now();
        when(pendingSubscriptionRepository.findByVrmAndContactDetails(TEST_VRM, MOBILE)).thenReturn(Optional.empty());
        ArgumentCaptor<PendingSubscription> pendingSubscriptionArgumentCaptor = ArgumentCaptor.forClass(PendingSubscription.class);

        PendingSubscriptionServiceResponse pendingSubscriptionResponse = this.subscriptionService.handlePendingSubscriptionCreation(
                TEST_VRM, CONTACT_MOBILE, date, new MotIdentification(TEST_MOT_TEST_NUMBER, TEST_DVLA_ID),
                vehicleDetails.getVehicleType());

        verifyZeroInteractions(notifyService);
        verify(pendingSubscriptionRepository, times(1)).save(pendingSubscriptionArgumentCaptor.capture());
        assertEquals(pendingSubscriptionArgumentCaptor.getValue().getMotDueDate(), date);
        assertEquals(pendingSubscriptionArgumentCaptor.getValue().getContactDetail().getValue(), MOBILE);
        assertEquals(pendingSubscriptionArgumentCaptor.getValue().getVrm(), TEST_VRM);
        assertNotNull(pendingSubscriptionResponse.getConfirmationId());
        assertNull(pendingSubscriptionResponse.getRedirectUri());
    }

    private void withExpectedSubscription(Optional<Subscription> subscription, String contact) {
        when(subscriptionRepository.findByVrmAndEmail(TEST_VRM, contact)).thenReturn(subscription);
    }

    private VehicleDetails getMockVehicleDetails() {
        LocalDate date = LocalDate.now();
        VehicleDetails vehicleDetails = new VehicleDetails();
        vehicleDetails.setMake("TEST-MAKE");
        vehicleDetails.setModel("TEST-MODEL");
        vehicleDetails.setVehicleType(VehicleType.MOT);
        vehicleDetails.setMotExpiryDate(date);
        vehicleDetails.setDvlaId(TEST_DVLA_ID);
        vehicleDetails.setMotTestNumber(TEST_MOT_TEST_NUMBER);

        return vehicleDetails;
    }
}
