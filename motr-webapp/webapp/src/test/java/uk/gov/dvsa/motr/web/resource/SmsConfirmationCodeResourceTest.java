package uk.gov.dvsa.motr.web.resource;

import org.junit.Before;
import org.junit.Test;

import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.analytics.SmartSurveyFeedback;
import uk.gov.dvsa.motr.web.component.subscription.helper.UrlHelper;
import uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService;
import uk.gov.dvsa.motr.web.cookie.MotrSession;
import uk.gov.dvsa.motr.web.test.render.TemplateEngineStub;
import uk.gov.dvsa.motr.web.validator.SmsConfirmationCodeValidator;
import uk.gov.dvsa.motr.web.viewmodel.SmsConfirmationCodeViewModel;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService.Confirmation.CODE_NOT_VALID;
import static uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService.Confirmation.CODE_NOT_VALID_MAX_ATTEMPTS_REACHED;
import static uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService.Confirmation.CODE_VALID;

public class SmsConfirmationCodeResourceTest {

    private static final TemplateEngineStub TEMPLATE_ENGINE_STUB = new TemplateEngineStub();
    private static final UrlHelper URL_HELPER = mock(UrlHelper.class);
    private static final String CONFIRMATION_LINK = "CONFIRMATION_LINK";
    private static final String PHONE_NUMBER = "07801856718";
    private static final String CONFIRMATION_ID = "ABC123";
    private static final String CONFIRMATION_CODE = "12345";
    private static final String INVALID_CONFIRMATION_CODE = "65432";
    private static final String VRM = "YN13NTX";


    private MotrSession motrSession;
    private SmsConfirmationCodeResource resource;
    private SmsConfirmationService smsConfirmationService;

    @Before
    public void setup() {

        smsConfirmationService = mock(SmsConfirmationService.class);
        motrSession = mock(MotrSession.class);
        SmartSurveyFeedback smartSurveyHelper = new SmartSurveyFeedback();
        this.resource = new SmsConfirmationCodeResource(motrSession, TEMPLATE_ENGINE_STUB, smsConfirmationService, URL_HELPER,
                smartSurveyHelper);

        VehicleDetails vehicleDetails = new VehicleDetails();
        vehicleDetails.setVehicleType(VehicleType.MOT);
        vehicleDetails.setRegNumber(VRM);

        when(URL_HELPER.confirmSubscriptionLink(CONFIRMATION_ID)).thenReturn(CONFIRMATION_LINK);
        when(motrSession.getPhoneNumberFromSession()).thenReturn(PHONE_NUMBER);
        when(motrSession.getVrmFromSession()).thenReturn(VRM);
        when(motrSession.getConfirmationIdFromSession()).thenReturn(CONFIRMATION_ID);
        when(motrSession.getVehicleDetailsFromSession()).thenReturn(vehicleDetails);
    }

    @Test
    public void smsConfirmationCodeTemplateIsRenderedOnGetWithViewModel() throws Exception {

        when(motrSession.isAllowedOnSmsConfirmationCodePage()).thenReturn(true);
        assertEquals(200, resource.smsConfirmationCodePageGet().getStatus());
        assertEquals("sms-confirmation-code", TEMPLATE_ENGINE_STUB.getTemplate());

        SmsConfirmationCodeViewModel expectedViewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(PHONE_NUMBER);

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("continue_button_text", "Continue");
        expectedMap.put("resendUrl", "resend");
        expectedMap.put("viewModel", expectedViewModel);
        assertEquals(expectedMap.get("continue_button_text"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("continue_button_text"));
        assertEquals(expectedMap.get("resendUrl"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("resendUrl"));
        assertEquals(SmsConfirmationCodeViewModel.class, TEMPLATE_ENGINE_STUB.getContext(Map.class).get("viewModel").getClass());
    }

    @Test
    public void smsConfirmationCodeTemplateIsRenderedOnGetWithViewModelUnderResendRateLimiting() throws Exception {

        when(motrSession.isAllowedOnSmsConfirmationCodePage()).thenReturn(true);
        when(motrSession.isSmsConfirmResendLimited()).thenReturn(true);
        assertEquals(200, resource.smsConfirmationCodePageGet().getStatus());
        assertEquals("sms-confirmation-code", TEMPLATE_ENGINE_STUB.getTemplate());

        SmsConfirmationCodeViewModel expectedViewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(PHONE_NUMBER);

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("continue_button_text", "Continue");
        expectedMap.put("resendUrl", "resend");
        expectedMap.put("viewModel", expectedViewModel);
        expectedMap.put("showInLIne", false);
        expectedMap.put("message", SmsConfirmationCodeValidator.CODE_ALREADY_RESENT);

        assertEquals(expectedMap.get("continue_button_text"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("continue_button_text"));
        assertEquals(expectedMap.get("resendUrl"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("resendUrl"));
        assertEquals(expectedMap.get("showInLIne"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("showInLIne"));
        assertEquals(expectedMap.get("message"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("message"));
        assertEquals(SmsConfirmationCodeViewModel.class, TEMPLATE_ENGINE_STUB.getContext(Map.class).get("viewModel").getClass());
    }

    @Test
    public void userIsRedirectedAfterSuccessfullFormSubmissionWithValidSmsConfirmationCode() throws Exception {

        when(motrSession.isAllowedToPostOnSmsConfirmationCodePage()).thenReturn(true);
        when(motrSession.getConfirmationIdFromSession()).thenReturn(CONFIRMATION_ID);
        when(smsConfirmationService.verifySmsConfirmationCode(any(), any(), any(), any())).thenReturn(CODE_VALID);

        Response response = resource.smsConfirmationCodePagePost(CONFIRMATION_CODE);

        verify(smsConfirmationService, times(1)).verifySmsConfirmationCode(
                eq(VRM), eq(PHONE_NUMBER), eq(CONFIRMATION_ID), eq(CONFIRMATION_CODE));
        assertEquals(302, response.getStatus());
        assertEquals(CONFIRMATION_LINK, response.getLocation().toString());
    }

    @Test
    public void onPostWithEmptyConfirmationCodeFormatMessageWillBePassedToView() throws Exception {

        when(motrSession.isAllowedToPostOnSmsConfirmationCodePage()).thenReturn(true);

        SmsConfirmationCodeViewModel expectedViewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(PHONE_NUMBER);

        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("message", SmsConfirmationCodeValidator.EMPTY_CONFIRMATION_CODE_MESSAGE);
        expectedMap.put("messageAtField", SmsConfirmationCodeValidator.EMPTY_CONFIRMATION_CODE_MESSAGE_AT_FIELD);
        expectedMap.put("continue_button_text", "Continue");
        expectedMap.put("resendUrl", "resend");
        expectedMap.put("viewModel", expectedViewModel);
        expectedMap.put("dataLayer", "{\"error\":\"" + SmsConfirmationCodeValidator.EMPTY_CONFIRMATION_CODE_MESSAGE + "\"}");

        Response response = resource.smsConfirmationCodePagePost("");
        assertEquals(200, response.getStatus());
        assertEquals("sms-confirmation-code", TEMPLATE_ENGINE_STUB.getTemplate());

        assertEquals(expectedMap.get("continue_button_text"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("continue_button_text"));
        assertEquals(expectedMap.get("resendUrl"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("resendUrl"));
        assertEquals(expectedMap.get("message"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("message"));
        assertEquals(expectedMap.get("messageAtField"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("messageAtField"));
        assertEquals(SmsConfirmationCodeViewModel.class, TEMPLATE_ENGINE_STUB.getContext(Map.class).get("viewModel").getClass());
    }

    @Test
    public void onPostWithInvalidConfirmationCodeFormatMessageWillBePassedToView() throws Exception {

        when(motrSession.isAllowedToPostOnSmsConfirmationCodePage()).thenReturn(true);

        SmsConfirmationCodeViewModel expectedViewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(PHONE_NUMBER);

        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("message", SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE);
        expectedMap.put("messageAtField", SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE_AT_FIELD);
        expectedMap.put("continue_button_text", "Continue");
        expectedMap.put("resendUrl", "resend");
        expectedMap.put("viewModel", expectedViewModel);
        expectedMap.put("dataLayer", "{\"error\":\"" + SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE + "\"}");

        Response response = resource.smsConfirmationCodePagePost("123");
        assertEquals(200, response.getStatus());
        assertEquals("sms-confirmation-code", TEMPLATE_ENGINE_STUB.getTemplate());

        assertEquals(expectedMap.get("continue_button_text"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("continue_button_text"));
        assertEquals(expectedMap.get("resendUrl"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("resendUrl"));
        assertEquals(expectedMap.get("message"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("message"));
        assertEquals(expectedMap.get("messageAtField"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("messageAtField"));
        assertEquals(SmsConfirmationCodeViewModel.class, TEMPLATE_ENGINE_STUB.getContext(Map.class).get("viewModel").getClass());
    }

    @Test
    public void onPostWithInvalidConfirmationCodeWithCorrectFormatMessageWillBePassedToView() throws Exception {

        when(motrSession.isAllowedToPostOnSmsConfirmationCodePage()).thenReturn(true);

        when(motrSession.getConfirmationIdFromSession()).thenReturn(CONFIRMATION_ID);
        when(smsConfirmationService.verifySmsConfirmationCode(any(), any(), any(), any())).thenReturn(CODE_NOT_VALID);

        SmsConfirmationCodeViewModel expectedViewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(PHONE_NUMBER);
        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("message", SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE);
        expectedMap.put("messageAtField", SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE_AT_FIELD);
        expectedMap.put("continue_button_text", "Continue");
        expectedMap.put("resendUrl", "resend");
        expectedMap.put("viewModel", expectedViewModel);
        expectedMap.put("dataLayer", "{\"error\":\"" + SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE + "\"}");

        Response response = resource.smsConfirmationCodePagePost(INVALID_CONFIRMATION_CODE);

        verify(smsConfirmationService, times(1)).verifySmsConfirmationCode(
                eq(VRM), eq(PHONE_NUMBER), eq(CONFIRMATION_ID), eq(INVALID_CONFIRMATION_CODE));

        assertEquals(200, response.getStatus());
        assertEquals("sms-confirmation-code", TEMPLATE_ENGINE_STUB.getTemplate());

        assertEquals(expectedMap.get("continue_button_text"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("continue_button_text"));
        assertEquals(expectedMap.get("resendUrl"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("resendUrl"));
        assertEquals(expectedMap.get("message"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("message"));
        assertEquals(expectedMap.get("messageAtField"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("messageAtField"));
        assertEquals(SmsConfirmationCodeViewModel.class, TEMPLATE_ENGINE_STUB.getContext(Map.class).get("viewModel").getClass());
    }

    @Test
    public void onPostWithInvalidConfirmationCode_AndMaxAttemptsReached_CorrectValuesPassedToView() throws Exception {

        when(motrSession.isAllowedToPostOnSmsConfirmationCodePage()).thenReturn(true);

        when(motrSession.getConfirmationIdFromSession()).thenReturn(CONFIRMATION_ID);
        when(smsConfirmationService.verifySmsConfirmationCode(any(), any(), any(), any()))
                .thenReturn(CODE_NOT_VALID_MAX_ATTEMPTS_REACHED);

        SmsConfirmationCodeViewModel expectedViewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(PHONE_NUMBER);
        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("message", SmsConfirmationCodeValidator.CODE_INCORRECT_3_TIMES);
        expectedMap.put("continue_button_text", "Continue");
        expectedMap.put("resendUrl", "resend");

        Response response = resource.smsConfirmationCodePagePost(INVALID_CONFIRMATION_CODE);

        verify(smsConfirmationService, times(1)).verifySmsConfirmationCode(
                eq(VRM), eq(PHONE_NUMBER), eq(CONFIRMATION_ID), eq(INVALID_CONFIRMATION_CODE));

        assertEquals(200, response.getStatus());
        assertEquals("sms-confirmation-code", TEMPLATE_ENGINE_STUB.getTemplate());

        assertEquals(expectedMap.get("continue_button_text"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("continue_button_text"));
        assertEquals(expectedMap.get("resendUrl"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("resendUrl"));
        assertEquals(expectedMap.get("message"), TEMPLATE_ENGINE_STUB.getContext(Map.class).get("message"));
        assertEquals(SmsConfirmationCodeViewModel.class, TEMPLATE_ENGINE_STUB.getContext(Map.class).get("viewModel").getClass());
    }
}
