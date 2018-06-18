package uk.gov.dvsa.motr.web.resource;

import org.junit.Before;
import org.junit.Test;

import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetailsClient;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetailsClientException;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.cookie.MotrSession;
import uk.gov.dvsa.motr.web.test.render.TemplateEngineStub;
import uk.gov.dvsa.motr.web.validator.MotDueDateValidator;
import uk.gov.dvsa.motr.web.validator.VrmValidator;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import static junit.framework.TestCase.assertNull;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VrmResourceTest {

    private static final String INVALID_REG_NUMBER = "________";
    private static final String VALID_REG_NUMBER = "FP12345";
    private static final String SYSTEM_ERROR_VRM = "ABC123";
    private static final String HONEY_POT = "";

    private VehicleDetailsClient client;
    private MotDueDateValidator motDueDateValidator;
    private TemplateEngineStub templateEngine;
    private VrmResource resource;
    private MotrSession motrSession;

    @Before
    public void setUp() {

        templateEngine = new TemplateEngineStub();
        client = mock(VehicleDetailsClient.class);
        motrSession = mock(MotrSession.class);
        motDueDateValidator = mock(MotDueDateValidator.class);
        resource = new VrmResource(motrSession, templateEngine, client, motDueDateValidator);
        when(motrSession.getVrmFromSession()).thenReturn("VRZ");
    }

    @Test
    public void getResultsInVrmTemplate() throws Exception {

        when(motrSession.getVrmFromSession()).thenReturn("VRZ5555");

        resource.vrmPageGet();
        assertEquals("vrm", templateEngine.getTemplate());
    }

    @Test
    public void postWithValidVrmResultsInRedirectToEmail() throws Exception {

        when(client.fetchByVrm(eq(VALID_REG_NUMBER))).thenReturn(Optional.of(new VehicleDetails()));
        when(motDueDateValidator.isDueDateValid(any())).thenReturn(true);
        when(motDueDateValidator.isDueDateInTheFuture(any())).thenReturn(false);
        Response response = resource.vrmPagePost(VALID_REG_NUMBER, HONEY_POT);
        verify(motrSession, times(1)).setVehicleDetails(any(VehicleDetails.class));
        assertEquals(302, response.getStatus());
    }

    @Test
    public void postWithInvalidVrmResultsInVrmTemplateAndInlineErrorMessage() throws Exception {

        resource.vrmPagePost(INVALID_REG_NUMBER, HONEY_POT);

        assertEquals("vrm", templateEngine.getTemplate());

        HashMap<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("inputFieldId", "reg-number-input");
        expectedContext.put("message", VrmValidator.REGISTRATION_CAN_ONLY_CONTAIN_LETTERS_NUMBERS_AND_HYPHENS_MESSAGE);
        expectedContext.put("back_url", HomepageResource.HOMEPAGE_URL);
        expectedContext.put("vrm", INVALID_REG_NUMBER);
        expectedContext.put("continue_button_text", "Continue");
        expectedContext.put("showInLine", "true");
        expectedContext.put("back_button_text", "Back");
        expectedContext.put("showSystemError", false);
        expectedContext.put("dataLayer", "{\"vrm\":\"" + INVALID_REG_NUMBER + "\",\"error\":\"" +
                VrmValidator.REGISTRATION_CAN_ONLY_CONTAIN_LETTERS_NUMBERS_AND_HYPHENS_MESSAGE + "\"}");

        assertEquals(expectedContext.toString(), templateEngine.getContext(Map.class).toString());
    }

    @Test
    public void postWithValidVrmWhenVehicleServiceReturnsServiceErrorResultsInVrmTemplateAndSystemErrorMessage() throws Exception {

        doThrow(VehicleDetailsClientException.class).when(client).fetchByVrm(eq(SYSTEM_ERROR_VRM));
        resource.vrmPagePost(SYSTEM_ERROR_VRM, HONEY_POT);

        assertEquals("vrm", templateEngine.getTemplate());

        HashMap<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("inputFieldId", "reg-number-input");
        expectedContext.put("back_url", HomepageResource.HOMEPAGE_URL);
        expectedContext.put("vrm", SYSTEM_ERROR_VRM);
        expectedContext.put("continue_button_text", "Continue");
        expectedContext.put("showInLine", "true");
        expectedContext.put("back_button_text", "Back");
        expectedContext.put("showSystemError", true);
        expectedContext.put("dataLayer", "{\"vrm\":\"ABC123\",\"error\":\"Trade API error\"}");

        assertEquals(expectedContext.toString(), templateEngine.getContext(Map.class).toString());
    }

    @Test
    public void whenVehicleDetailsNotFoundShowErrorMessage() throws Exception {

        when(client.fetchByVrm(eq(VALID_REG_NUMBER))).thenReturn(Optional.empty());

        resource.vrmPagePost(VALID_REG_NUMBER, HONEY_POT);

        HashMap<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("inputFieldId", "reg-number-input");
        expectedContext.put("message", "We don't hold information about this vehicle.<br/>" +
                "<br/>Check that you've typed in the correct registration number.");
        expectedContext.put("back_url", HomepageResource.HOMEPAGE_URL);
        expectedContext.put("vrm", VALID_REG_NUMBER);
        expectedContext.put("continue_button_text", "Continue");
        expectedContext.put("showSystemError", false);
        expectedContext.put("showInLine", "false");
        expectedContext.put("back_button_text", "Back");
        expectedContext.put("dataLayer", "{\"vrm\":\"FP12345\",\"error\":\"Vehicle not found\"}");

        assertEquals(expectedContext.toString(), templateEngine.getContext(Map.class).toString());
        assertEquals("vrm", templateEngine.getTemplate());
    }

    @Test
    public void whenVehicleDetailsNotFoundHgvShowErrorMessage() throws Exception {

        when(client.fetchByVrm(eq(VALID_REG_NUMBER))).thenReturn(Optional.empty());
        when(motrSession.isHgvPsvVehiclesFeatureToggleOn()).thenReturn(true);

        resource.vrmPagePost(VALID_REG_NUMBER, HONEY_POT);

        HashMap<String, Object> expectedContext = new HashMap<>();
        expectedContext.put("inputFieldId", "reg-number-input");
        expectedContext.put("message", "We don't hold information about this vehicle.<br/>" +
                "<br/>Check that you've typed in the correct registration number.");
        expectedContext.put("back_url", HomepageResource.HOMEPAGE_URL);
        expectedContext.put("vrm", VALID_REG_NUMBER);
        expectedContext.put("continue_button_text", "Continue");
        expectedContext.put("showSystemError", false);
        expectedContext.put("showInLine", "false");
        expectedContext.put("back_button_text", "Back");
        expectedContext.put("dataLayer", "{\"vrm\":\"FP12345\",\"error\":\"Vehicle not found\"}");

        assertEquals(expectedContext.toString(), templateEngine.getContext(Map.class).toString());
        assertEquals("vrm", templateEngine.getTemplate());
    }

    @Test
    public void whenVehicleDetailsFoundWithDueDateInThePast_thenShouldRedirect() throws Exception {
        LocalDate testExpiryDate = LocalDate.now().minusDays(2);

        VehicleDetails vehicle = new VehicleDetails();
        vehicle.setMotExpiryDate(testExpiryDate);
        vehicle.setVehicleType(VehicleType.MOT);

        when(client.fetchByVrm(eq(VALID_REG_NUMBER))).thenReturn(Optional.of(vehicle));
        when(motDueDateValidator.isDueDateValid(any())).thenReturn(true);
        when(motDueDateValidator.isDueDateInTheFuture(any())).thenReturn(false);

        Response response = resource.vrmPagePost(VALID_REG_NUMBER, HONEY_POT);

        assertEquals(302, response.getStatus());
    }

    @Test
    public void whenVisitingFromReviewPage_thenButtonAndLinkShowCorrectText() throws Exception {

        when(motrSession.visitingFromReviewPage()).thenReturn(true);
        when(client.fetchByVrm(eq(VALID_REG_NUMBER))).thenReturn(Optional.of(new VehicleDetails()));
        when(motDueDateValidator.isDueDateValid(any())).thenReturn(true);
        when(motDueDateValidator.isDueDateInTheFuture(any())).thenReturn(true);

        resource = new VrmResource(motrSession, templateEngine, client, motDueDateValidator);
        Response response = resource.vrmPagePost(VALID_REG_NUMBER, HONEY_POT);

        assertEquals("review", response.getHeaders().get("Location").get(0).toString());
        assertEquals(302, response.getStatus());
        assertNull(response.getEntity());
    }

    @Test
    public void whenRetrievingPageWithExistingRegNumber_thenTheRegIsAddedToThePageModel() throws Exception {

        when(motrSession.visitingFromReviewPage()).thenReturn(true);
        resource.vrmPageGet();

        Map context = templateEngine.getContext(Map.class);
        assertEquals("VRZ", context.get("vrm"));
    }

    @Test
    public void whenHoneyPotValueIsNotEmpty_thenRedirectToEmailConfirmationPendingPage() throws Exception {

        when(motrSession.visitingFromReviewPage()).thenReturn(true);
        when(client.fetchByVrm(eq(VALID_REG_NUMBER))).thenReturn(Optional.of(new VehicleDetails()));
        when(motDueDateValidator.isDueDateValid(any())).thenReturn(true);
        when(motDueDateValidator.isDueDateInTheFuture(any())).thenReturn(true);

        resource = new VrmResource(motrSession, templateEngine, client, motDueDateValidator);
        Response response = resource.vrmPagePost(VALID_REG_NUMBER, "Honey");

        assertEquals("email-confirmation-pending", response.getHeaders().get("Location").get(0).toString());
        assertEquals(302, response.getStatus());
    }
}
