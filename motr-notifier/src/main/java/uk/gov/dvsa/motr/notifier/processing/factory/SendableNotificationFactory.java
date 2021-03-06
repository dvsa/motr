package uk.gov.dvsa.motr.notifier.processing.factory;

import uk.gov.dvsa.motr.notifier.notify.NotificationTemplateIds;
import uk.gov.dvsa.motr.notifier.processing.model.SubscriptionQueueItem;
import uk.gov.dvsa.motr.notifier.processing.model.notification.email.HgvPsvOneMonthEmailNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.email.HgvPsvTwoMonthEmailNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.email.MotOneDayAfterEmailNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.email.MotOneMonthEmailNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.email.MotTwoWeekEmailNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.email.SendableEmailNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.sms.HgvPsvOneMonthSmsNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.sms.HgvPsvTwoMonthSmsNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.sms.MotOneDayAfterSmsNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.sms.MotOneMonthSmsNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.sms.MotTwoWeekSmsNotification;
import uk.gov.dvsa.motr.notifier.processing.model.notification.sms.SendableSmsNotification;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;

import java.time.LocalDate;
import java.util.Optional;

import static uk.gov.dvsa.motr.notifier.processing.service.SubscriptionHandlerHelper.oneDayAfterNotificationRequired;
import static uk.gov.dvsa.motr.notifier.processing.service.SubscriptionHandlerHelper.oneMonthNotificationRequired;
import static uk.gov.dvsa.motr.notifier.processing.service.SubscriptionHandlerHelper.twoMonthNotificationRequired;
import static uk.gov.dvsa.motr.notifier.processing.service.SubscriptionHandlerHelper.twoWeekNotificationRequired;

public class SendableNotificationFactory {

    private NotificationTemplateIds emailNotificationTemplateIds;
    private NotificationTemplateIds smsNotificationTemplateIds;

    private String webBaseUrl;
    private String vehicleDataUrl;
    private String checksumSalt;

    public SendableNotificationFactory(NotificationTemplateIds emailNotificationTemplateIds,
                                       NotificationTemplateIds smsNotificationTemplateIds, String webBaseUrl, String vehicleDataUrl,
                                       String checksumSalt) {

        this.emailNotificationTemplateIds = emailNotificationTemplateIds;
        this.smsNotificationTemplateIds = smsNotificationTemplateIds;
        this.webBaseUrl = webBaseUrl;
        this.vehicleDataUrl = vehicleDataUrl;
        this.checksumSalt = checksumSalt;
    }

    public Optional<SendableEmailNotification> getEmailNotification(LocalDate requestDate, SubscriptionQueueItem subscription,
                                                                    VehicleDetails vehicleDetails) {

        Optional<SendableEmailNotification> notification = Optional.empty();

        if (subscription.getVehicleType() == VehicleType.MOT) {
            if (oneMonthNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {

                String templateId = emailNotificationTemplateIds.getOneMonthNotificationTemplateId();

                notification = Optional.of(new MotOneMonthEmailNotification(webBaseUrl, vehicleDataUrl, checksumSalt)
                        .setTemplateId(templateId)
                );

            } else if (twoWeekNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {
                String templateId = emailNotificationTemplateIds.getTwoWeekNotificationTemplateId();

                notification = Optional.of(new MotTwoWeekEmailNotification(webBaseUrl)
                        .setTemplateId(templateId)
                );

            } else if (oneDayAfterNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {
                String templateId =  emailNotificationTemplateIds.getOneDayAfterNotificationTemplateId();

                notification = Optional.of(new MotOneDayAfterEmailNotification(webBaseUrl)
                        .setTemplateId(templateId)
                );
            }
        } else { // non-MOT vehicles (e.g. HGV, PSV)
            if (twoMonthNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {
                notification = Optional.of(new HgvPsvTwoMonthEmailNotification(webBaseUrl, vehicleDataUrl, checksumSalt)
                        .setTemplateId(emailNotificationTemplateIds.getTwoMonthHgvPsvNotificationTemplateId())
                );
            } else if (oneMonthNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {
                notification = Optional.of(new HgvPsvOneMonthEmailNotification(webBaseUrl)
                        .setTemplateId(emailNotificationTemplateIds.getOneMonthHgvPsvNotificationTemplateId())
                );
            }
        }

        notification.ifPresent(n -> {
            n.personalise(subscription, vehicleDetails);
        });

        return notification;
    }

    public Optional<SendableSmsNotification> getSmsNotification(SubscriptionQueueItem subscription, VehicleDetails vehicleDetails) {

        Optional<SendableSmsNotification> notification = Optional.empty();
        LocalDate requestDate = subscription.getLoadedOnDate();

        if (subscription.getVehicleType() == VehicleType.MOT) {
            if (oneMonthNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {

                notification = Optional.of(new MotOneMonthSmsNotification()
                        .setTemplateId(smsNotificationTemplateIds.getOneMonthNotificationTemplateId())
                );

            } else if (twoWeekNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {

                notification = Optional.of(new MotTwoWeekSmsNotification()
                        .setTemplateId(smsNotificationTemplateIds.getTwoWeekNotificationTemplateId())
                );

            } else if (oneDayAfterNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {

                notification = Optional.of(new MotOneDayAfterSmsNotification()
                        .setTemplateId(smsNotificationTemplateIds.getOneDayAfterNotificationTemplateId())
                );
            }
        } else { // non-MOT vehicles (e.g. HGV, PSV)
            if (twoMonthNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {
                notification = Optional.of(new HgvPsvTwoMonthSmsNotification()
                        .setTemplateId(smsNotificationTemplateIds.getTwoMonthHgvPsvNotificationTemplateId())
                );
            } else if (oneMonthNotificationRequired(requestDate, vehicleDetails.getMotExpiryDate())) {
                notification = Optional.of(new HgvPsvOneMonthSmsNotification()
                        .setTemplateId(smsNotificationTemplateIds.getOneMonthHgvPsvNotificationTemplateId())
                );
            }
        }

        notification.ifPresent(n -> n.personalise(vehicleDetails));

        return notification;
    }
}
