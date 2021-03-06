package uk.gov.dvsa.motr.web.resource;

import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.web.analytics.DataLayerHelper;
import uk.gov.dvsa.motr.web.analytics.DataLayerMessageId;
import uk.gov.dvsa.motr.web.analytics.DataLayerMessageType;
import uk.gov.dvsa.motr.web.analytics.SmartSurveyFeedback;
import uk.gov.dvsa.motr.web.component.subscription.helper.UrlHelper;
import uk.gov.dvsa.motr.web.component.subscription.model.Subscription;
import uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService;
import uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService.Confirmation;
import uk.gov.dvsa.motr.web.cookie.MotrSession;
import uk.gov.dvsa.motr.web.render.TemplateEngine;
import uk.gov.dvsa.motr.web.validator.FieldValidator;
import uk.gov.dvsa.motr.web.validator.SmsConfirmationCodeValidator;
import uk.gov.dvsa.motr.web.viewmodel.SmsConfirmationCodeViewModel;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static uk.gov.dvsa.motr.web.resource.RedirectResponseBuilder.redirect;

@Singleton
@Path("/confirm-phone")
@Produces("text/html")
public class SmsConfirmationCodeResource {

    private static final String SMS_CONFIRMATION_CODE_TEMPLATE = "sms-confirmation-code";
    private static final String CONFIRMATION_CODE_MODEL_KEY = "confirmationCode";
    private static final String MESSAGE_MODEL_KEY = "message";
    private static final String MESSAGE_AT_FIELD_MODEL_KEY = "messageAtField";
    private static final String INPUT_FIELD_ID = "confirm-code-input";
    private static final String INPUT_FIELD_ID_MODEL_KEY = "inputFieldId";

    private final TemplateEngine renderer;
    private final MotrSession motrSession;
    private final DataLayerHelper dataLayerHelper;
    private final SmsConfirmationService smsConfirmationService;
    private final UrlHelper urlHelper;
    private final SmartSurveyFeedback smartSurveyHelperFeedback;

    @Inject
    public SmsConfirmationCodeResource(
            MotrSession motrSession,
            TemplateEngine renderer,
            SmsConfirmationService smsConfirmationService,
            UrlHelper urlHelper,
            SmartSurveyFeedback smartSurveyHelperFeedback
    ) {
        this.motrSession = motrSession;
        this.renderer = renderer;
        this.dataLayerHelper = new DataLayerHelper();
        this.smsConfirmationService = smsConfirmationService;
        this.urlHelper = urlHelper;
        this.smartSurveyHelperFeedback = smartSurveyHelperFeedback;
    }

    @GET
    public Response smsConfirmationCodePageGet() throws Exception {

        if (!motrSession.isAllowedOnSmsConfirmationCodePage()) {
            return redirect(HomepageResource.HOMEPAGE_URL);
        }

        SmsConfirmationCodeViewModel viewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(
                motrSession.getUnnormalizedPhoneNumberFromSession()
        );
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("continue_button_text", "Continue");
        modelMap.put("resendUrl", "resend");
        modelMap.put("viewModel", viewModel);

        addDetailsForSurveyFromSession(modelMap);

        if (motrSession.isSmsConfirmResendLimited()) {
            modelMap.put(MESSAGE_MODEL_KEY, SmsConfirmationCodeValidator.CODE_ALREADY_RESENT);
            modelMap.put(MESSAGE_AT_FIELD_MODEL_KEY, false);
            modelMap.put("showInLIne", false);
            modelMap.put(INPUT_FIELD_ID_MODEL_KEY, INPUT_FIELD_ID);
            dataLayerHelper.setMessage(DataLayerMessageId.CODE_ALREADY_RESENT,
                    DataLayerMessageType.USER_INPUT_ERROR,
                    SmsConfirmationCodeValidator.CODE_ALREADY_RESENT);
            modelMap.putAll(dataLayerHelper.formatAttributes());
            dataLayerHelper.clear();
        }

        return Response.ok(renderer.render(SMS_CONFIRMATION_CODE_TEMPLATE, modelMap)).build();
    }

    @POST
    public Response smsConfirmationCodePagePost(@FormParam("confirmationCode") String confirmationCode) throws Exception {

        if (!motrSession.isAllowedToPostOnSmsConfirmationCodePage()) {
            return redirect(HomepageResource.HOMEPAGE_URL);
        }

        FieldValidator validator = new SmsConfirmationCodeValidator();
        boolean showInLine = true;

        if (validator.isValid(confirmationCode)) {

            String confirmationId = motrSession.getConfirmationIdFromSession();
            Confirmation codeValid = smsConfirmationService.verifySmsConfirmationCode(
                    motrSession.getVrmFromSession(),
                    motrSession.getPhoneNumberFromSession(),
                    confirmationId,
                    confirmationCode);

            switch (codeValid) {
                case CODE_NOT_VALID_MAX_ATTEMPTS_REACHED:
                    validator.setMessage(SmsConfirmationCodeValidator.CODE_INCORRECT_3_TIMES);
                    showInLine = false;
                    dataLayerHelper.setMessage(DataLayerMessageId.CODE_INCORRECT_3_TIMES,
                            DataLayerMessageType.USER_INPUT_ERROR,
                            validator.getMessage());
                    break;
                case CODE_NOT_VALID:
                    validator.setMessage(SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE);
                    validator.setMessageAtField(SmsConfirmationCodeValidator.INVALID_CONFIRMATION_CODE_MESSAGE_AT_FIELD);
                    showInLine = true;
                    dataLayerHelper.setMessage(DataLayerMessageId.CONFIRMATION_CODE_DOESNT_EXIST_MESSAGE,
                            DataLayerMessageType.USER_INPUT_ERROR,
                            validator.getMessage());
                    break;
                case CODE_VALID:
                    return redirect(urlHelper.confirmSubscriptionLink(confirmationId));
                default:
                    break;
            }
        } else {
            dataLayerHelper.setMessage(DataLayerMessageId.INVALID_CONFIRMATION_CODE_MESSAGE,
                    DataLayerMessageType.USER_INPUT_ERROR,
                    validator.getMessage());
        }

        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(MESSAGE_MODEL_KEY, validator.getMessage());
        modelMap.put(MESSAGE_AT_FIELD_MODEL_KEY, validator.getMessageAtField());

        SmsConfirmationCodeViewModel viewModel = new SmsConfirmationCodeViewModel().setPhoneNumber(motrSession.getPhoneNumberFromSession());
        modelMap.put("viewModel", viewModel);

        modelMap.put(CONFIRMATION_CODE_MODEL_KEY, confirmationCode);
        modelMap.put("continue_button_text", "Continue");
        modelMap.put("resendUrl", "resend");
        modelMap.put("showInLine", showInLine);
        modelMap.put(INPUT_FIELD_ID_MODEL_KEY, INPUT_FIELD_ID);
        modelMap.putAll(dataLayerHelper.formatAttributes());
        dataLayerHelper.clear();

        addDetailsForSurveyFromSession(modelMap);

        return Response.ok(renderer.render(SMS_CONFIRMATION_CODE_TEMPLATE, modelMap)).build();
    }

    private void addDetailsForSurveyFromSession(Map<String, Object> modelMap) {

        VehicleDetails vehicle = motrSession.getVehicleDetailsFromSession();
        smartSurveyHelperFeedback.addContactType(Subscription.ContactType.MOBILE.getValue());
        smartSurveyHelperFeedback.addVrm(vehicle.getRegNumber());
        smartSurveyHelperFeedback.addVehicleType(vehicle.getVehicleType());
        smartSurveyHelperFeedback.addIsSigningBeforeFirstMotDue(vehicle.hasNoMotYet());

        modelMap.putAll(smartSurveyHelperFeedback.formatAttributes());
        smartSurveyHelperFeedback.clear();
    }
}
