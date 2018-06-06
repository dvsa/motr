package uk.gov.dvsa.motr.test.integration.subscriptionloader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import uk.gov.dvsa.motr.subscriptionloader.processing.loader.LoadReport;
import uk.gov.dvsa.motr.subscriptionloader.processing.model.Subscription;
import uk.gov.dvsa.motr.test.environment.variables.TestEnvironmentVariables;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.model.SubscriptionItem;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.model.SubscriptionTable;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;

import static org.junit.Assert.assertEquals;

public class SubscriptionLoaderHgvPsvTests extends SubscriptionLoaderBase {

    @Rule
    public final EnvironmentVariables environmentVariables = new TestEnvironmentVariables();

    private SubscriptionItem subscriptionItem;

    @Before
    public void setUp() {
        super.setUp();
        subscriptionItem = new SubscriptionItem().generateDvlaId();
    }

    @After
    public void cleanUp() {
        fixture.removeItem(subscriptionItem);
    }

    @Test
    public void runLoaderForOneMonthHgvReminderThenEnsureItemsAddedToQueue() throws Exception {
        subscriptionItem.setVehicleType(VehicleType.HGV);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();
        String testTime = subscriptionItem.getMotDueDate().minusDays(30) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertSubscriptionIsAddedToQueue();
        assertReportIsUpdatedCorrectly(loadReport);
    }

    @Test
    public void runLoaderForTwoMonthsPsvReminderThenEnsureItemsAddedToQueue() throws Exception {
        subscriptionItem.setVehicleType(VehicleType.PSV);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();
        String testTime = subscriptionItem.getMotDueDate().minusDays(60) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertSubscriptionIsAddedToQueue();
        assertReportIsUpdatedCorrectly(loadReport);
    }

    private void assertSubscriptionIsAddedToQueue() throws java.io.IOException {
        Subscription subscription = getQueuedSubscription();

        assertEquals(subscriptionItem.getVrm(), subscription.getVrm());
        assertEquals(subscriptionItem.getEmail(), subscription.getContactDetail().getValue());
        assertEquals(subscriptionItem.getContactType().getValue(), subscription.getContactDetail().getContactType().getValue());
        assertEquals(subscriptionItem.getVehicleType(), subscription.getVehicleType());
        assertEquals(subscriptionItem.getMotTestNumber(), subscription.getMotTestNumber());
        assertEquals(subscriptionItem.getMotDueDate(), subscription.getMotDueDate());
        assertEquals(subscriptionItem.getId(), subscription.getId());
    }

    private void assertReportIsUpdatedCorrectly(LoadReport loadReport) {

        assertEquals(1, loadReport.getSubmittedForProcessing());
        assertEquals(1, loadReport.getDvlaVehiclesProcessed());
        assertEquals(0, loadReport.getNonDvlaVehiclesProcessed());
        assertEquals(1, loadReport.getTotalProcessed());
    }
}
