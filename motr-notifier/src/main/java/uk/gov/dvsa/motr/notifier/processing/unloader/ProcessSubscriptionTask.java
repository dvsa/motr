package uk.gov.dvsa.motr.notifier.processing.unloader;

import uk.gov.dvsa.motr.eventlog.EventLogger;
import uk.gov.dvsa.motr.notifier.events.HgvPsvDetailsRetrievalFailedEvent;
import uk.gov.dvsa.motr.notifier.events.NotifyEvent;
import uk.gov.dvsa.motr.notifier.events.NotifyReminderFailedEvent;
import uk.gov.dvsa.motr.notifier.events.SubscriptionProcessedEvent;
import uk.gov.dvsa.motr.notifier.events.SubscriptionProcessingFailedEvent;
import uk.gov.dvsa.motr.notifier.events.SubscriptionQueueItemRemovalFailedEvent;
import uk.gov.dvsa.motr.notifier.events.SuccessfulSubscriptionProcessedEvent;
import uk.gov.dvsa.motr.notifier.events.VehicleDetailsRetrievalFailedEvent;
import uk.gov.dvsa.motr.notifier.events.VehicleNotFoundEvent;
import uk.gov.dvsa.motr.notifier.processing.model.SubscriptionQueueItem;
import uk.gov.dvsa.motr.notifier.processing.queue.QueueItemRemover;
import uk.gov.dvsa.motr.notifier.processing.queue.RemoveSubscriptionFromQueueException;
import uk.gov.dvsa.motr.notifier.processing.service.ProcessSubscriptionService;
import uk.gov.dvsa.motr.notifier.processing.service.VehicleNotFoundException;
import uk.gov.dvsa.motr.vehicledetails.HgvPsvDetailsClientException;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetailsClientException;
import uk.gov.service.notify.NotificationClientException;

public class ProcessSubscriptionTask {

    private SubscriptionQueueItem subscriptionQueueItemToProcess;
    private ProcessSubscriptionService processSubscriptionService;

    public ProcessSubscriptionTask(ProcessSubscriptionService processSubscriptionService) {

        this.processSubscriptionService = processSubscriptionService;
    }

    public void run(SubscriptionQueueItem subscriptionQueueItemToProcess) throws Exception {

        this.subscriptionQueueItemToProcess = subscriptionQueueItemToProcess;

        Long startedProcessingTime = System.currentTimeMillis();

        processSubscriptionService.processSubscription(subscriptionQueueItemToProcess);

        EventLogger.logEvent(new SuccessfulSubscriptionProcessedEvent()
                .setMessageProcessTimeProcessed(System.currentTimeMillis() - startedProcessingTime)
                .setMessageBody(subscriptionQueueItemToProcess.toString()));
    }

    private NotifyEvent populateEvent(NotifyEvent event) {
        event.setEmail(subscriptionQueueItemToProcess.getContactDetail().getValue())
                .setContactType(subscriptionQueueItemToProcess.getContactDetail().getContactType())
                .setVrm(subscriptionQueueItemToProcess.getVrm())
                .setExpiryDate(subscriptionQueueItemToProcess.getMotDueDate());

        if (subscriptionQueueItemToProcess.getMotTestNumber() == null) {
            event.setDvlaId(subscriptionQueueItemToProcess.getDvlaId());
        } else {
            event.setMotTestNumber(subscriptionQueueItemToProcess.getMotTestNumber());
        }
        return event;
    }

    private SubscriptionProcessedEvent populateEvent(SubscriptionProcessedEvent event) {
        event.setEmail(subscriptionQueueItemToProcess.getContactDetail().getValue())
                .setVrm(subscriptionQueueItemToProcess.getVrm())
                .setExpiryDate(subscriptionQueueItemToProcess.getMotDueDate());

        if (subscriptionQueueItemToProcess.getMotTestNumber() == null) {
            event.setDvlaId(subscriptionQueueItemToProcess.getDvlaId());
        } else {
            event.setMotTestNumber(subscriptionQueueItemToProcess.getMotTestNumber());
        }
        return event;
    }
}
