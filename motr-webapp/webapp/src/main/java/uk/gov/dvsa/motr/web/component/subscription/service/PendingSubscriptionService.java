package uk.gov.dvsa.motr.web.component.subscription.service;

import uk.gov.dvsa.motr.eventlog.EventLogger;
import uk.gov.dvsa.motr.notifications.service.NotifyService;
import uk.gov.dvsa.motr.remote.vehicledetails.VehicleDetailsService;
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
import uk.gov.dvsa.motr.web.eventlog.subscription.PendingSubscriptionCreatedEvent;
import uk.gov.dvsa.motr.web.eventlog.subscription.PendingSubscriptionCreationFailedEvent;
import uk.gov.dvsa.motr.web.formatting.MakeModelFormatter;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

public class PendingSubscriptionService {

    private final PendingSubscriptionRepository pendingSubscriptionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotifyService notifyService;
    private final UrlHelper urlHelper;
    private final VehicleDetailsClient client;
    private RandomIdGenerator randomIdGenerator = new RandomIdGenerator();

    @Inject
    public PendingSubscriptionService(
            PendingSubscriptionRepository pendingSubscriptionRepository,
            SubscriptionRepository subscriptionRepository,
            NotifyService notifyService,
            UrlHelper urlHelper,
            VehicleDetailsClient client
    ) {

        this.pendingSubscriptionRepository = pendingSubscriptionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.notifyService = notifyService;
        this.urlHelper = urlHelper;
        this.client = client;
    }

    public PendingSubscriptionServiceResponse handlePendingSubscriptionCreation(
            String vrm,
            ContactDetail contactDetail,
            LocalDate motDueDate,
            MotIdentification motIdentification,
            VehicleType vehicleType) {

        String contact = contactDetail.getValue();
        Subscription.ContactType contactType = contactDetail.getContactType();

        Optional<Subscription> subscription = subscriptionRepository.findByVrmAndEmail(vrm, contact);
        PendingSubscriptionServiceResponse pendingSubscriptionResponse = new PendingSubscriptionServiceResponse();

        if (subscription.isPresent()) {
            updateSubscriptionVehicleData(subscription.get(), motDueDate, vehicleType);
            return getRedirectUrlWhenSubscriptionAlreadyExisis(contactType, pendingSubscriptionResponse);
        } else {
            return getRedirectUrlWhenNewSubscription(vrm, contact, motDueDate, motIdentification,
                    contactType, pendingSubscriptionResponse, vehicleType);
        }
    }

    private PendingSubscriptionServiceResponse getRedirectUrlWhenSubscriptionAlreadyExisis(
            Subscription.ContactType contactType,
            PendingSubscriptionServiceResponse pendingSubscriptionResponse) {

        String redirectUri = (contactType == Subscription.ContactType.EMAIL
                ? urlHelper.emailConfirmedNthTimeLink() : urlHelper.phoneConfirmedNthTimeLink());

        return pendingSubscriptionResponse.setRedirectUri(redirectUri);
    }

    private PendingSubscriptionServiceResponse getRedirectUrlWhenNewSubscription(
            String vrm, String contact, LocalDate motDueDate,
            MotIdentification motIdentification,
            Subscription.ContactType contactType,
            PendingSubscriptionServiceResponse pendingSubscriptionResponse,
            VehicleType vehicleType
    ) {

        String confirmationId;
        Optional<PendingSubscription> pendingSubscription = pendingSubscriptionRepository.findByVrmAndContactDetails(vrm, contact);
        if (pendingSubscription.isPresent()) {
            confirmationId = pendingSubscription.get().getConfirmationId();
        } else {
            confirmationId = randomIdGenerator.generateId();
        }
        createPendingSubscription(vrm, contact, motDueDate, confirmationId, motIdentification, contactType, vehicleType);

        return contactType == Subscription.ContactType.EMAIL
                ? pendingSubscriptionResponse.setRedirectUri(urlHelper.emailConfirmationPendingLink())
                : pendingSubscriptionResponse.setConfirmationId(confirmationId);
    }

    /**
     * Creates pending subscription in the system to be confirmed later by confirmation link
     * @param vrm        subscription vrm
     * @param contact      subscription contact
     * @param motDueDate most recent mot due date
     * @param confirmationId confirmation id
     * @param motIdentification the identifier for this vehicle (may be dvla id or mot test number)
     * @param vehicleType type of vehicle
     */
    public void createPendingSubscription(
            String vrm, String contact, LocalDate motDueDate,
            String confirmationId, MotIdentification motIdentification,
            Subscription.ContactType contactType, VehicleType vehicleType
    ) {

        try {
            // We have the relevant data in motrSession, but for security reasons we don't won't to create
            // a subscription based on untrusted data. We call directly for vehicle details here.
            VehicleDetails vehicleDetails = VehicleDetailsService.getVehicleDetails(vrm, client);

            PendingSubscription pendingSubscription = new PendingSubscription()
                    .setConfirmationId(confirmationId)
                    .setContactDetail(new ContactDetail(contact, contactType))
                    .setVrm(vrm)
                    .setMotDueDate(vehicleDetails.getMotExpiryDate())
                    .setMotIdentification(vehicleDetails.getMotIdentification())
                    .setVehicleType(vehicleDetails.getVehicleType());

            pendingSubscriptionRepository.save(pendingSubscription);

            if (contactType == Subscription.ContactType.EMAIL) {

                notifyService.sendEmailAddressConfirmationEmail(
                        contact,
                        urlHelper.confirmSubscriptionLink(pendingSubscription.getConfirmationId()),
                        MakeModelFormatter.getMakeModelDisplayStringFromVehicleDetails(vehicleDetails, ", ") + vrm
                );
            }

            EventLogger.logEvent(
                    new PendingSubscriptionCreatedEvent().setVrm(vrm).setEmail(contact).setMotDueDate(vehicleDetails.getMotExpiryDate())
                    .setMotIdentification(vehicleDetails.getMotIdentification()).setVehicleType(vehicleDetails.getVehicleType())
            );
        } catch (Exception e) {
            EventLogger.logErrorEvent(
                    new PendingSubscriptionCreationFailedEvent().setVrm(vrm).setEmail(contact).setMotDueDate(motDueDate)
                    .setMotIdentification(motIdentification).setVehicleType(vehicleType), e);
            throw e;
        }
    }

    private Subscription updateSubscriptionVehicleData(Subscription subscription, LocalDate motDueDate, VehicleType vehicleType) {
        subscription.setMotDueDate(motDueDate);
        subscription.setVehicleType(vehicleType);
        subscriptionRepository.save(subscription);

        return subscription;
    }

    public PendingSubscriptionService setRandomIdGenerator(RandomIdGenerator randomIdGenerator) {
        this.randomIdGenerator = randomIdGenerator;
        return this;
    }
}
