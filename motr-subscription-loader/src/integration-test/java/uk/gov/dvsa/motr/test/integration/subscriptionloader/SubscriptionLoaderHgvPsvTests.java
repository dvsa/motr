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
        environmentVariables.set("HGV_PSV_SUBSCRIPTION_LOADER", "true");
        subscriptionItem.setVehicleType(VehicleType.HGV);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();
        String testTime = subscriptionItem.getMotDueDate().minusMonths(1) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertReportIsUpdatedCorrectly(loadReport, subscriptionItem.getVehicleType());
    }

    @Test
    public void runLoaderForTwoMonthsPsvReminderThenEnsureItemsAddedToQueue() throws Exception {
        environmentVariables.set("HGV_PSV_SUBSCRIPTION_LOADER", "true");
        subscriptionItem.setVehicleType(VehicleType.PSV);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();
        String testTime = subscriptionItem.getMotDueDate().minusDays(60) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertReportIsUpdatedCorrectly(loadReport, subscriptionItem.getVehicleType());
    }

    @Test
    public void runLoaderForTwoMonthsTrailerReminderThenEnsureItemsAddedToQueue() throws Exception {
        environmentVariables.set("HGV_PSV_SUBSCRIPTION_LOADER", "true");
        subscriptionItem.setVehicleType(VehicleType.TRAILER);
        subscriptionItem.setDvlaId(null);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();
        String testTime = subscriptionItem.getMotDueDate().minusDays(60) + "T12:00:00Z";

        LoadReport loadReport = eventHandler.handle(buildRequest(testTime), buildContext());

        assertReportIsUpdatedCorrectly(loadReport, subscriptionItem.getVehicleType());
    }

    private void assertReportIsUpdatedCorrectly(LoadReport loadReport, VehicleType vehicleType) {

        assertEquals(1, loadReport.getSubmittedForProcessing());
        assertEquals(0, loadReport.getMotDvlaVehiclesProcessed());
        assertEquals(0, loadReport.getMotNonDvlaVehiclesProcessed());
        assertEquals(vehicleType == VehicleType.HGV ? 1 : 0, loadReport.getHgvVehiclesProcessed());
        assertEquals(vehicleType == VehicleType.PSV ? 1 : 0, loadReport.getPsvVehiclesProcessed());
        assertEquals(vehicleType == VehicleType.TRAILER ? 1 : 0, loadReport.getHgvTrailersProcessed());
        assertEquals(1, loadReport.getTotalProcessed());
    }
}
