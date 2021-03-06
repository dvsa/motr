package uk.gov.dvsa.motr.test.persistence;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import org.junit.Before;
import org.junit.Test;

import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.core.DynamoDbFixture;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.model.PendingSubscriptionItem;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.model.PendingSubscriptionTable;
import uk.gov.dvsa.motr.vehicledetails.MotIdentification;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.component.subscription.model.ContactDetail;
import uk.gov.dvsa.motr.web.component.subscription.model.PendingSubscription;
import uk.gov.dvsa.motr.web.component.subscription.persistence.DynamoDbPendingSubscriptionRepository;
import uk.gov.dvsa.motr.web.component.subscription.persistence.PendingSubscriptionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static uk.gov.dvsa.motr.test.integration.dynamodb.DynamoDbIntegrationHelper.client;
import static uk.gov.dvsa.motr.test.integration.dynamodb.DynamoDbIntegrationHelper.pendingSubscriptionTableName;
import static uk.gov.dvsa.motr.test.integration.dynamodb.DynamoDbIntegrationHelper.region;
import static uk.gov.dvsa.motr.test.integration.dynamodb.DynamoDbIntegrationHelper.waitUntilPresent;

import static java.time.format.DateTimeFormatter.ofPattern;

public class DynamoDbPendingSubscriptionRepositoryTest {

    PendingSubscriptionRepository repo;
    DynamoDbFixture fixture;

    @Before
    public void setUp() {
        repo = new DynamoDbPendingSubscriptionRepository(pendingSubscriptionTableName(), region());
        fixture = new DynamoDbFixture(client());
    }

    @Test
    public void getByIdReturnsSubscription_ForMotVehicle_IfExistsInDb() {

        PendingSubscriptionItem expectedSubscriptionForMotVehicle = new PendingSubscriptionItem();
        expectedSubscriptionForMotVehicle.setDvlaId(null);

        fixture.table(new PendingSubscriptionTable().item(expectedSubscriptionForMotVehicle)).run();

        PendingSubscription actualSubscription = waitUntilPresent(
                () -> repo.findByConfirmationId(expectedSubscriptionForMotVehicle.getConfirmationId()),
                true,
                5000
        ).get();

        assertEquals(expectedSubscriptionForMotVehicle.getEmail(), actualSubscription.getContactDetail().getValue());
        assertEquals(expectedSubscriptionForMotVehicle.getVrm(), actualSubscription.getVrm());
        assertEquals(expectedSubscriptionForMotVehicle.getMotDueDate(), actualSubscription.getMotDueDate());
        assertEquals(expectedSubscriptionForMotVehicle.getMotTestNumber(),
                actualSubscription.getMotIdentification().getMotTestNumber().get());
    }

    @Test
    public void getByIdReturnsSubscription_ForDvlaVehicle_IfExistsInDb() {

        PendingSubscriptionItem expectedSubscriptionForDvlaVehicle = new PendingSubscriptionItem();
        expectedSubscriptionForDvlaVehicle.setMotTestNumber(null);

        fixture.table(new PendingSubscriptionTable().item(expectedSubscriptionForDvlaVehicle)).run();

        PendingSubscription actualSubscription = waitUntilPresent(
                () -> repo.findByConfirmationId(expectedSubscriptionForDvlaVehicle.getConfirmationId()),
                true,
                5000
        ).get();

        assertEquals(expectedSubscriptionForDvlaVehicle.getEmail(), actualSubscription.getContactDetail().getValue());
        assertEquals(expectedSubscriptionForDvlaVehicle.getVrm(), actualSubscription.getVrm());
        assertEquals(expectedSubscriptionForDvlaVehicle.getMotDueDate(), actualSubscription.getMotDueDate());
        assertEquals(expectedSubscriptionForDvlaVehicle.getDvlaId(), actualSubscription.getMotIdentification().getDvlaId().get());
    }

    @Test
    public void saveSubscriptionForMotVehicleCorrectlySavesToDb() {

        PendingSubscriptionItem subscriptionItemForMotVehicle = new PendingSubscriptionItem();

        MotIdentification motIdentification = new MotIdentification(subscriptionItemForMotVehicle.getMotTestNumber(), null);

        ContactDetail contactDetail =
                new ContactDetail(subscriptionItemForMotVehicle.getEmail(), subscriptionItemForMotVehicle.getContactType());

        PendingSubscription subscription = new PendingSubscription();
        subscription
                .setConfirmationId(subscriptionItemForMotVehicle.getConfirmationId())
                .setContactDetail(contactDetail)
                .setVehicleType(VehicleType.MOT)
                .setVrm(subscriptionItemForMotVehicle.getVrm())
                .setMotDueDate(subscriptionItemForMotVehicle.getMotDueDate())
                .setMotIdentification(motIdentification);

        repo.save(subscription);

        PendingSubscription actualSubscription = waitUntilPresent(
                () -> repo.findByConfirmationId(subscription.getConfirmationId()),
                true,
                5000
        ).get();

        assertEquals(subscriptionItemForMotVehicle.getEmail(), actualSubscription.getContactDetail().getValue());
        assertEquals(subscriptionItemForMotVehicle.getVrm(), actualSubscription.getVrm());
        assertEquals(subscriptionItemForMotVehicle.getVehicleType(), actualSubscription.getVehicleType());
        assertEquals(subscriptionItemForMotVehicle.getMotDueDate(), actualSubscription.getMotDueDate());
        assertEquals(subscriptionItemForMotVehicle.getMotTestNumber(), actualSubscription.getMotIdentification().getMotTestNumber().get());
    }

    @Test
    public void saveSubscriptionForDvlaVehicleCorrectlySavesToDb() {

        PendingSubscriptionItem subscriptionItemForDvlaVehicle = new PendingSubscriptionItem();

        MotIdentification motIdentification = new MotIdentification(null, subscriptionItemForDvlaVehicle.getDvlaId());

        ContactDetail contactDetail =
                new ContactDetail(subscriptionItemForDvlaVehicle.getEmail(), subscriptionItemForDvlaVehicle.getContactType());

        PendingSubscription subscription = new PendingSubscription();
        subscription
                .setConfirmationId(subscriptionItemForDvlaVehicle.getConfirmationId())
                .setContactDetail(contactDetail)
                .setVehicleType(VehicleType.MOT)
                .setVrm(subscriptionItemForDvlaVehicle.getVrm())
                .setMotDueDate(subscriptionItemForDvlaVehicle.getMotDueDate())
                .setMotIdentification(motIdentification);

        repo.save(subscription);

        PendingSubscription actualSubscription = waitUntilPresent(
                () -> repo.findByConfirmationId(subscription.getConfirmationId()),
                true,
                5000
        ).get();

        assertEquals(subscriptionItemForDvlaVehicle.getEmail(), actualSubscription.getContactDetail().getValue());
        assertEquals(subscriptionItemForDvlaVehicle.getVrm(), actualSubscription.getVrm());
        assertEquals(subscriptionItemForDvlaVehicle.getVehicleType(), actualSubscription.getVehicleType());
        assertEquals(subscriptionItemForDvlaVehicle.getMotDueDate(), actualSubscription.getMotDueDate());
        assertEquals(subscriptionItemForDvlaVehicle.getDvlaId(), actualSubscription.getMotIdentification().getDvlaId().get());
    }

    @Test
    public void saveSubscriptionForHgvVehicleCorrectlySavesToDb() {

        PendingSubscriptionItem subscriptionItemForHgvVehicle = new PendingSubscriptionItem();

        MotIdentification motIdentification = new MotIdentification(
                subscriptionItemForHgvVehicle.getMotTestNumber(),
                subscriptionItemForHgvVehicle.getDvlaId());

        ContactDetail contactDetail =
                new ContactDetail(subscriptionItemForHgvVehicle.getEmail(), subscriptionItemForHgvVehicle.getContactType());

        PendingSubscription subscription = new PendingSubscription();
        subscription
                .setConfirmationId(subscriptionItemForHgvVehicle.getConfirmationId())
                .setContactDetail(contactDetail)
                .setVehicleType(VehicleType.HGV)
                .setVrm(subscriptionItemForHgvVehicle.getVrm())
                .setMotDueDate(subscriptionItemForHgvVehicle.getMotDueDate())
                .setMotIdentification(motIdentification);

        repo.save(subscription);

        PendingSubscription actualSubscription = waitUntilPresent(
                () -> repo.findByConfirmationId(subscription.getConfirmationId()),
                true,
                5000
        ).get();

        assertEquals(subscriptionItemForHgvVehicle.getEmail(), actualSubscription.getContactDetail().getValue());
        assertEquals(subscriptionItemForHgvVehicle.getVrm(), actualSubscription.getVrm());
        assertEquals(
                "Wrong vehicle type for pending sub wth vrm: " + subscriptionItemForHgvVehicle.getVrm(),
                VehicleType.HGV, actualSubscription.getVehicleType());
        assertEquals(subscriptionItemForHgvVehicle.getMotDueDate(), actualSubscription.getMotDueDate());
        assertTrue(actualSubscription.getMotIdentification().getDvlaId().isPresent());
        assertEquals(subscriptionItemForHgvVehicle.getDvlaId(), actualSubscription.getMotIdentification().getDvlaId().get());
    }

    @Test
    public void saveSubscriptionCorrectlySavesNonModelAttributesToDb() {

        PendingSubscriptionItem subscriptionItem = new PendingSubscriptionItem();

        MotIdentification motIdentification = new MotIdentification(subscriptionItem.getMotTestNumber(), null);

        PendingSubscription subscription = new PendingSubscription();
        subscription
                .setConfirmationId(subscriptionItem.getConfirmationId())
                .setContactDetail(new ContactDetail(subscriptionItem.getEmail(), subscriptionItem.getContactType()))
                .setVrm(subscriptionItem.getVrm())
                .setVehicleType(VehicleType.MOT)
                .setMotDueDate(subscriptionItem.getMotDueDate())
                .setMotIdentification(motIdentification);

        repo.save(subscription);

        ValueMap specValueMap = new ValueMap()
                .withString(":vrm", subscription.getVrm())
                .withString(":email", subscription.getContactDetail().getValue());
        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("vrm = :vrm AND email = :email")
                .withValueMap(specValueMap);

        Item savedItem = new DynamoDB(client()).getTable(pendingSubscriptionTableName()).query(spec).iterator().next();

        assertNotNull("created_at cannot be null when saving db", savedItem.getString("created_at"));

        String dueDateMd = savedItem.getString("mot_due_date_md");
        assertEquals("due date md fragment is incorrect", dueDateMd, subscription.getMotDueDate().format(ofPattern("MM-dd")));
    }

    @Test
    public void getByIdReturnsEmptyIfSubscriptionDoesNotExist() {

        assertFalse(repo.findByConfirmationId("ID_THAT_DOES_NOT_EXIST").isPresent());
    }

    @Test
    public void subscriptionIsDeleted() {

        PendingSubscriptionItem sub = new PendingSubscriptionItem();
        fixture.table(new PendingSubscriptionTable().item(sub)).run();

        MotIdentification motIdentification = new MotIdentification(sub.getMotTestNumber(), null);

        PendingSubscription subscription = new PendingSubscription();
        subscription
                .setConfirmationId(sub.getConfirmationId())
                .setContactDetail(new ContactDetail(sub.getEmail(), sub.getContactType()))
                .setVrm(sub.getVrm())
                .setVehicleType(VehicleType.MOT)
                .setMotIdentification(motIdentification);

        repo.delete(subscription);

        waitUntilPresent(() -> repo.findByConfirmationId(sub.getConfirmationId()), false, 5000);
    }
}
