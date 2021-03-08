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


public class SubscriptionLoaderTests extends SubscriptionLoaderBase {

    @Rule
    public final EnvironmentVariables environmentVariables = new TestEnvironmentVariables();

    private SubscriptionItem subscriptionItem;

    @Before
    public void setUp() {
        super.setUp();
        subscriptionItem = new SubscriptionItem()
                .generateMotTestNumber()
                .setVehicleType(VehicleType.MOT);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();
    }

    @After
    public void cleanUp() {
        fixture.removeItem(subscriptionItem);
    }

    @Test
    public void runLoaderForOneMonthReminderThenEnsureItemsAddedToQueue() throws Exception {
        String testTime = subscriptionItem.getMotDueDate().minusMonths(1) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertReportIsUpdatedCorrectly(loadReport);
    }

    @Test
    public void runLoaderForTwoWeeksReminderThenEnsureItemsAddedToQueue() throws Exception {
        String testTime = subscriptionItem.getMotDueDate().minusDays(14) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertReportIsUpdatedCorrectly(loadReport);
    }

    @Test
    public void runLoaderForOneDayAfterReminderThenEnsureItemsAddedToQueue() throws Exception {
        String testTime = subscriptionItem.getMotDueDate().plusDays(1L) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertReportIsUpdatedCorrectly(loadReport);
    }

    private void assertReportIsUpdatedCorrectly(LoadReport loadReport) {

        assertEquals(1, loadReport.getSubmittedForProcessing());
        assertEquals(0, loadReport.getMotDvlaVehiclesProcessed());
        assertEquals(1, loadReport.getMotNonDvlaVehiclesProcessed());
        assertEquals(1, loadReport.getTotalProcessed());
    }
}
