package uk.gov.dvsa.motr.test.integration.unloader;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import uk.gov.dvsa.motr.notifier.component.subscription.persistence.DynamoDbSubscriptionRepository;
import uk.gov.dvsa.motr.notifier.component.subscription.persistence.SubscriptionDbItem;
import uk.gov.dvsa.motr.notifier.handler.EventHandler;
import uk.gov.dvsa.motr.notifier.processing.unloader.NotifierReport;
import uk.gov.dvsa.motr.test.environmant.variables.TestEnvironmentVariables;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.core.DynamoDbFixture;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.model.SubscriptionItem;
import uk.gov.dvsa.motr.test.integration.dynamodb.fixture.model.SubscriptionTable;
import uk.gov.dvsa.motr.test.integration.lambda.LoaderHelper;
import uk.gov.dvsa.motr.test.integration.lambda.LoaderInvocationEvent;
import uk.gov.service.notify.NotificationClientException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static uk.gov.dvsa.motr.test.environmant.variables.TestEnvironmentVariables.region;
import static uk.gov.dvsa.motr.test.environmant.variables.TestEnvironmentVariables.subscriptionTableName;
import static uk.gov.dvsa.motr.test.integration.dynamodb.DynamoDbIntegrationHelper.dynamoDbClient;

public class SubscriptionDbItemQueueItemUnloaderTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new TestEnvironmentVariables();

    private static final LocalDate MOCK_API_RANDOM_VEHICLE_DATE = LocalDate.of(2017, 3, 9);
    private static final LocalDate DATE_NOT_MATCHING_VEHICLE_MOCK = LocalDate.of(2017, 5, 15);
    private static final String SPECIFIC_MOCKED_VEHICLE_VRM = "LOY-500";
    private static final LocalDate MOCK_API_SPECIFIC_VEHICLE_DATE = LocalDate.of(2017, 3, 14);
    private static final String MOCK_API_NOT_FOUND_VRM = "12345";

    private ObjectMapper jsonMapper = new ObjectMapper();
    private LoaderHelper loaderHelper;
    private EventHandler eventHandler;
    private SubscriptionItem subscriptionItem;
    private DynamoDbFixture fixture;
    private DynamoDbSubscriptionRepository repo;

    @Before
    public void setUp() {

        repo = new DynamoDbSubscriptionRepository(subscriptionTableName(), region());
        loaderHelper = new LoaderHelper();
        eventHandler = new EventHandler();
        fixture = new DynamoDbFixture(dynamoDbClient());
    }

    @After
    public void cleanUp() {

        fixture.removeItem(subscriptionItem);
    }

    @Test
    public void whenAnItemIsInTheDb_TheLoaderAddsToQueue_ThenTheNotifierSuccessfullyProcessesIt()
            throws IOException, InterruptedException, ExecutionException, NotificationClientException {

        //add a subscription item to the subscription table
        subscriptionItem = new SubscriptionItem();
        subscriptionItem.setMotDueDate(MOCK_API_RANDOM_VEHICLE_DATE);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();

        //invoke the subscription loader with correct date to load items from db into queue
        String testTime = subscriptionItem.getMotDueDate().minusMonths(1) + "T12:00:00Z";
        loaderHelper.invokeLoader(buildLoaderRequest(testTime));

        //invoke the notifiers handle event with correct date to read items from the queue
        NotifierReport report = eventHandler.handle(new Object(), buildContext());

        assertEquals(1, report.getSuccessfullyProcessed());
    }

    @Test
    public void whenProcessingASubscriptionWithAMismatchedMotDueDate_TheSubscriptionDateInTheDbIsUpdated() throws IOException,
            InterruptedException, ExecutionException, NotificationClientException {

        subscriptionItem = new SubscriptionItem();
        subscriptionItem
                .setMotDueDate(DATE_NOT_MATCHING_VEHICLE_MOCK)
                .setVrm(SPECIFIC_MOCKED_VEHICLE_VRM);
        fixture.table(new SubscriptionTable().item(subscriptionItem)).run();

        String testTime = subscriptionItem.getMotDueDate().minusMonths(1) + "T12:00:00Z";
        loaderHelper.invokeLoader(buildLoaderRequest(testTime));

        //assert that the db subscription date doesn't equal the mock api date
        SubscriptionDbItem originalSubscriptionDbItem = repo.findById(subscriptionItem.getId()).get();
        assertFalse(originalSubscriptionDbItem.getMotDueDate().equals(MOCK_API_SPECIFIC_VEHICLE_DATE));

        NotifierReport report = eventHandler.handle(new Object(), buildContext());

        //assert that the db subscription date now is equal to the mock api date
        SubscriptionDbItem changedSubscriptionDbItem = repo.findById(subscriptionItem.getId()).get();
        assertTrue(changedSubscriptionDbItem.getMotDueDate().equals(MOCK_API_SPECIFIC_VEHICLE_DATE));

        assertEquals(1, report.getSuccessfullyProcessed());
    }

    private String buildLoaderRequest(String testTime) throws JsonProcessingException {

        LoaderInvocationEvent loaderInvocationEvent = new LoaderInvocationEvent(testTime);
        return jsonMapper.writeValueAsString(loaderInvocationEvent);
    }

    private Context buildContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return UUID.randomUUID().toString();
            }

            @Override
            public String getLogGroupName() {
                return null;
            }

            @Override
            public String getLogStreamName() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                return null;
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {

                return 400000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 0;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        };
    }
}
