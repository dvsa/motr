package uk.gov.dvsa.motr.web.component.subscription.persistence;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Index;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;

import uk.gov.dvsa.motr.eventlog.EventLogger;
import uk.gov.dvsa.motr.vehicledetails.MotIdentification;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.component.subscription.model.ContactDetail;
import uk.gov.dvsa.motr.web.component.subscription.model.PendingSubscription;
import uk.gov.dvsa.motr.web.component.subscription.model.Subscription;
import uk.gov.dvsa.motr.web.eventlog.subscription.PendingSubscriptionAlreadyDeletedEvent;
import uk.gov.dvsa.motr.web.eventlog.subscription.PendingSubscriptionDeletionFailedEvent;
import uk.gov.dvsa.motr.web.helper.SystemVariableParam;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import static uk.gov.dvsa.motr.web.system.SystemVariable.DB_TABLE_PENDING_SUBSCRIPTION;
import static uk.gov.dvsa.motr.web.system.SystemVariable.REGION;

@Singleton
public class DynamoDbPendingSubscriptionRepository implements PendingSubscriptionRepository {

    private static final int HOURS_TO_DELETION = 24;

    private final DynamoDB dynamoDb;
    private final String tableName;

    @Inject
    public DynamoDbPendingSubscriptionRepository(
            @SystemVariableParam(DB_TABLE_PENDING_SUBSCRIPTION) String tableName,
            @SystemVariableParam(REGION) String region) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
        this.dynamoDb = new DynamoDB(client);
        this.tableName = tableName;
    }

    @Override
    public Optional<PendingSubscription> findByConfirmationId(String id) {

        QuerySpec query = new QuerySpec()
                .withKeyConditionExpression("id = :id")
                .withValueMap(new ValueMap().withString(":id", id));

        Index table = dynamoDb.getTable(tableName).getIndex("id-vt-gsi");

        ItemCollection<QueryOutcome> items = table.query(query);

        Iterator<Item> resultIterator = items.iterator();

        if (!resultIterator.hasNext()) {
            return Optional.empty();
        }

        Item item = resultIterator.next();

        return Optional.of(mapItemToPendingSubscription(item));
    }

    @Override
    public Optional<PendingSubscription> findByVrmAndContactDetails(String vrm, String contactDetails) {

        QuerySpec query = new QuerySpec()
                .withKeyConditionExpression("vrm = :vrm AND email = :contact")
                .withValueMap(new ValueMap().withString(":vrm", vrm).withString(":contact", contactDetails));

        Table table = dynamoDb.getTable(tableName);

        ItemCollection<QueryOutcome> items = table.query(query);
        Iterator<Item> resultIterator = items.iterator();

        if (!resultIterator.hasNext()) {
            return Optional.empty();
        }

        Item item = resultIterator.next();

        return Optional.of(mapItemToPendingSubscription(item));
    }

    @Override
    public void save(PendingSubscription subscription) {

        Item item = new Item()
                .withString("id", subscription.getConfirmationId())
                .withString("vrm", subscription.getVrm())
                .withString("email", subscription.getContactDetail().getValue())
                .withString("mot_due_date", subscription.getMotDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .withString("vehicle_type", subscription.getVehicleType().name())
                .withString("mot_due_date_md", subscription.getMotDueDate().format(DateTimeFormatter.ofPattern("MM-dd")))
                .withString("created_at", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .withNumber("deletion_date", ZonedDateTime.now().plusHours(HOURS_TO_DELETION).toEpochSecond())
                .withString("contact_type", subscription.getContactDetail().getContactType().getValue());

        subscription.getMotIdentification().getMotTestNumber()
                .ifPresent(motTestNumber -> item.withString("mot_test_number", motTestNumber));
        subscription.getMotIdentification().getDvlaId().ifPresent(dvlaId -> item.withString("dvla_id", dvlaId));

        dynamoDb.getTable(tableName).putItem(item);
    }

    @Override
    public void delete(PendingSubscription subscription) {

        PrimaryKey key = new PrimaryKey("vrm", subscription.getVrm(), "email", subscription.getContactDetail().getValue());
        Map<String, Object> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":id", subscription.getConfirmationId());

        try {
            dynamoDb.getTable(tableName).deleteItem(
                    key,
                    "id = :id",
                    null,
                    expressionAttributeValues
            );
        } catch (ConditionalCheckFailedException e) {
            logPendingSubscriptionAlreadyDeletedEvent(e, subscription);
        } catch (AmazonDynamoDBException e) {
            logPendingSubscriptionDeletionFailedEvent(e, subscription);
            throw e;
        }
    }

    private void logPendingSubscriptionAlreadyDeletedEvent(
            ConditionalCheckFailedException exception,
            PendingSubscription subscription) {

        EventLogger.logEvent(new PendingSubscriptionAlreadyDeletedEvent()
                .setConfirmationId(subscription.getConfirmationId())
                .setVrm(subscription.getVrm())
                .setVehicleType(subscription.getVehicleType())
                .setContact(subscription.getContactDetail().getValue())
                .setMessage(exception.getMessage())
                .setErrorMessage(exception.getErrorMessage())
        );
    }

    private void logPendingSubscriptionDeletionFailedEvent(
            AmazonDynamoDBException exception,
            PendingSubscription subscription) {

        EventLogger.logEvent(new PendingSubscriptionDeletionFailedEvent()
                .setConfirmationId(subscription.getConfirmationId())
                .setVrm(subscription.getVrm())
                .setVehicleType(subscription.getVehicleType())
                .setContact(subscription.getContactDetail().getValue())
                .setMessage(exception.getMessage())
                .setErrorMessage(exception.getErrorMessage())
        );
    }

    private PendingSubscription mapItemToPendingSubscription(Item item) {

        ContactDetail contactDetail =
                new ContactDetail(item.getString("email"), Subscription.ContactType.valueOf(item.getString("contact_type")));

        PendingSubscription subscription = new PendingSubscription();
        subscription.setConfirmationId(item.getString("id"));
        subscription.setVrm(item.getString("vrm"));
        subscription.setVehicleType(VehicleType.getFromString(item.getString("vehicle_type")));
        subscription.setContactDetail(contactDetail);
        subscription.setMotDueDate(LocalDate.parse(item.getString("mot_due_date")));
        subscription.setMotIdentification(new MotIdentification(item.getString("mot_test_number"), item.getString("dvla_id")));
        return subscription;
    }
}
