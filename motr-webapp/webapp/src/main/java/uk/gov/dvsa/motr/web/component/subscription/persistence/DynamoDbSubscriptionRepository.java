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

import uk.gov.dvsa.motr.vehicledetails.MotIdentification;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.component.subscription.model.ContactDetail;
import uk.gov.dvsa.motr.web.component.subscription.model.Subscription;
import uk.gov.dvsa.motr.web.helper.SystemVariableParam;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import static uk.gov.dvsa.motr.web.system.SystemVariable.DB_TABLE_SUBSCRIPTION;
import static uk.gov.dvsa.motr.web.system.SystemVariable.REGION;

@Singleton
public class DynamoDbSubscriptionRepository implements SubscriptionRepository {

    private final DynamoDB dynamoDb;
    private final String tableName;

    @Inject
    public DynamoDbSubscriptionRepository(
            @SystemVariableParam(DB_TABLE_SUBSCRIPTION) String tableName,
            @SystemVariableParam(REGION) String region) {

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
        this.dynamoDb = new DynamoDB(client);
        this.tableName = tableName;
    }

    @Override
    public Optional<Subscription> findByUnsubscribeId(String id) {
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

        return Optional.of(mapItemToSubscription(item));
    }

    @Override
    public Optional<Subscription> findByVrmAndEmail(String vrm, String email) {

        QuerySpec query = new QuerySpec()
                .withKeyConditionExpression("vrm = :vrm AND email = :email")
                .withValueMap(new ValueMap().withString(":vrm", vrm).withString(":email", email));

        Table table = dynamoDb.getTable(tableName);

        ItemCollection<QueryOutcome> items = table.query(query);
        Iterator<Item> resultIterator = items.iterator();

        if (!resultIterator.hasNext()) {
            return Optional.empty();
        }

        Item item = resultIterator.next();

        return Optional.of(mapItemToSubscription(item));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int findByEmail(String email) {

        List<Subscription> subscriptions = new ArrayList<>();
        ItemCollection<QueryOutcome> items = queryOutcomeItemCollection(email);

        for (Item item : items) {
            subscriptions.add(mapItemToSubscription(item));
        }

        return subscriptions.size();
    }

    @Override
    public void save(Subscription subscription) {

        Item item = new Item()
                .withString("id", subscription.getUnsubscribeId())
                .withString("vrm", subscription.getVrm())
                .withString("email", subscription.getContactDetail().getValue())
                .withString("vehicle_type", subscription.getVehicleType().toString())
                .withString("mot_due_date", subscription.getMotDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .withString("mot_due_date_md", subscription.getMotDueDate().format(DateTimeFormatter.ofPattern("MM-dd")))
                .withString("created_at", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .withString("contact_type", subscription.getContactDetail().getContactType().getValue());

        subscription.getMotIdentification().getMotTestNumber().ifPresent(
                motTestNumber -> item.withString("mot_test_number", motTestNumber)
        );
        subscription.getMotIdentification().getDvlaId().ifPresent(dvlaId -> item.withString("dvla_id", dvlaId));

        dynamoDb.getTable(tableName).putItem(item);
    }

    @Override
    public void delete(Subscription subscription) {
        PrimaryKey key = new PrimaryKey("vrm", subscription.getVrm(), "email", subscription.getContactDetail().getValue());
        Map<String, Object> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":id", subscription.getUnsubscribeId());

        dynamoDb.getTable(tableName).deleteItem(
                key,
                "id = :id",
                null,
                expressionAttributeValues
        );
    }

    private Subscription mapItemToSubscription(Item item) {

        ContactDetail contactDetail = new ContactDetail(
                item.getString("email"),
                Subscription.ContactType.valueOf(item.getString("contact_type"))
        );

        Subscription subscription = new Subscription();
        subscription.setUnsubscribeId(item.getString("id"));
        subscription.setVrm(item.getString("vrm"));
        subscription.setVehicleType(VehicleType.getFromString(item.getString("vehicle_type")));
        subscription.setMotDueDate(LocalDate.parse(item.getString("mot_due_date")));
        subscription.setMotIdentification(new MotIdentification(item.getString("mot_test_number"), item.getString("dvla_id")));
        subscription.setContactDetail(contactDetail);
        return subscription;
    }

    private ItemCollection<QueryOutcome> queryOutcomeItemCollection(String email) {

        return dynamoDb
                .getTable(tableName)
                .query(new QuerySpec().withHashKey("email", email));
    }
}
