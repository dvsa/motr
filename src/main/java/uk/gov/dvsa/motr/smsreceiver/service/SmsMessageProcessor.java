package uk.gov.dvsa.motr.smsreceiver.service;

import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.dvsa.motr.eventlog.EventLogger;
import uk.gov.dvsa.motr.exception.InvalidNotifyCredentialsException;
import uk.gov.dvsa.motr.smsreceiver.events.FailedToFindSubscriptionEvent;
import uk.gov.dvsa.motr.smsreceiver.events.InvalidVrmSentEvent;
import uk.gov.dvsa.motr.smsreceiver.events.UnableToProcessMessageEvent;
import uk.gov.dvsa.motr.smsreceiver.events.UserAlreadyUnsubscribedEvent;
import uk.gov.dvsa.motr.smsreceiver.model.Message;
import uk.gov.dvsa.motr.smsreceiver.subscription.model.Subscription;
import uk.gov.dvsa.motr.smsreceiver.subscription.persistence.SubscriptionRepository;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

public class SmsMessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsMessageProcessor.class);
    private static final String ERROR_MESSAGE = "There is insufficient information in the received sms message to proceed with " +
            "processing for message with message body %s";
    private static final String REASON_FOR_CANCELLATION_USER_CANCELLED = "User cancelled";
    private static final String AUTHORISATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer ";

    private SubscriptionRepository subscriptionRepository;
    private SmsMessageValidator smsMessageValidator;
    private CancelledSubscriptionHelper cancelledSubscriptionHelper;
    private String token;
    private VrmValidator vrmValidator;
    private MessageExtractor messageExtractor;

    @Inject
    public SmsMessageProcessor(SubscriptionRepository subscriptionRepository,
            SmsMessageValidator smsMessageValidator,
            CancelledSubscriptionHelper cancelledSubscriptionHelper,
            String token,
            VrmValidator vrmValidator,
            MessageExtractor messageExtractor) {

        this.subscriptionRepository = subscriptionRepository;
        this.smsMessageValidator = smsMessageValidator;
        this.cancelledSubscriptionHelper = cancelledSubscriptionHelper;
        this.token = token;
        this.vrmValidator = vrmValidator;
        this.messageExtractor = messageExtractor;
    }

    public void run(AwsProxyRequest request) {

        try {
            verifyBearerToken(request);

            Message smsMessage = getSmsMessageFromRequest(request);
            String mobileNumber = messageExtractor.getMobileNumberWithoutInternationalCode(smsMessage.getSubscribersMobileNumber());
            String messageBody = smsMessage.getMessage();

            if (!smsMessageValidator.messageHasSufficientDetails(smsMessage)) {
                String errorMessage = String.format(ERROR_MESSAGE, messageBody);
                EventLogger.logEvent(new UnableToProcessMessageEvent().setReason(errorMessage));
            }

            String vrm = messageExtractor.getVrmFromMesageBody(messageBody).replaceAll("\\s+", "").toUpperCase();
            if (!vrmValidator.isValid(vrm)) {
                String validationMessage = vrmValidator.getMessage();
                EventLogger.logEvent(new InvalidVrmSentEvent().setVrm(vrm).setMessage(validationMessage));
                return;
            }

            Optional<Subscription> subscription = subscriptionRepository.findByVrmAndMobileNumber(vrm, mobileNumber);

            if (subscription.isPresent()) {
                Subscription subscriptionToProcess = subscription.get();
                cancelledSubscriptionHelper.createANewCancelledSubscriptionEntry(subscriptionToProcess);
                subscriptionRepository.delete(subscriptionToProcess);
            } else if (cancelledSubscriptionHelper.foundMatchingCancelledSubscription(vrm, mobileNumber)) {
                EventLogger.logEvent(new UserAlreadyUnsubscribedEvent().setVrm(vrm));
            } else {
                EventLogger.logEvent(new FailedToFindSubscriptionEvent().setVrm(vrm));
            }
        } catch (Exception e) {
            EventLogger.logEvent(new UnableToProcessMessageEvent().setReason(e.getMessage()));
        }
    }

    private void verifyBearerToken(AwsProxyRequest request) throws InvalidNotifyCredentialsException {

        String authorisationHeader = request.getHeaders().get(AUTHORISATION_HEADER);
        String bearerToken = StringUtils.substringAfter(authorisationHeader, BEARER_TOKEN);
        if (!bearerToken.equals(token)) {
            throw new InvalidNotifyCredentialsException("invalid token in request");
        }
    }

    private Message getSmsMessageFromRequest(AwsProxyRequest request) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(request.getBody(), Message.class);
    }
}
