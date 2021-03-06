package uk.gov.dvsa.motr.web.resource;

import uk.gov.dvsa.motr.eventlog.EventLogger;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetailsClient;
import uk.gov.dvsa.motr.vehicledetails.VehicleDetailsClientException;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;
import uk.gov.dvsa.motr.web.analytics.DataLayerHelper;
import uk.gov.dvsa.motr.web.analytics.DataLayerMessageId;
import uk.gov.dvsa.motr.web.analytics.DataLayerMessageType;
import uk.gov.dvsa.motr.web.analytics.SmartSurveyFeedback;
import uk.gov.dvsa.motr.web.cookie.MotrSession;
import uk.gov.dvsa.motr.web.eventlog.HoneyPotTriggeredEvent;
import uk.gov.dvsa.motr.web.eventlog.vehicle.VehicleDetailsExceptionEvent;
import uk.gov.dvsa.motr.web.render.TemplateEngine;
import uk.gov.dvsa.motr.web.validator.MotDueDateValidator;
import uk.gov.dvsa.motr.web.validator.TrailerVrmValidator;
import uk.gov.dvsa.motr.web.validator.VrmValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static uk.gov.dvsa.motr.vehicledetails.VehicleType.MOT;
import static uk.gov.dvsa.motr.web.analytics.DataLayerHelper.VRM_KEY;
import static uk.gov.dvsa.motr.web.resource.RedirectResponseBuilder.redirect;

@Singleton
@Path("/vrm")
@Produces("text/html")
public class VrmResource {

    private static final String VRM_MODEL_KEY = "vrm";
    private static final String VEHICLE_NOT_FOUND_MESSAGE = "We don't hold information about this vehicle.<br/>" +
            "<br/>Check that you've typed in the correct registration number.";
    private static final String TRAILER_NOT_FOUND_MESSAGE = "We don't hold information about this trailer.<br/>" +
            "<br/>Check that you've typed in the correct trailer ID.";

    private static final String MESSAGE_KEY = "message";
    private static final String SHOW_INLINE_KEY = "showInLine";
    private static final String VRM_TEMPLATE_NAME = "vrm";
    private static final String SHOW_SYSTEM_ERROR = "showSystemError";
    private static final String INPUT_FIELD_ID = "reg-number-input";
    private static final String INPUT_FIELD_ID_MODEL_KEY = "inputFieldId";

    private final TemplateEngine renderer;
    private final VehicleDetailsClient client;
    private final MotDueDateValidator motDueDateValidator;
    private final MotrSession motrSession;
    private final DataLayerHelper dataLayerHelper;
    private final SmartSurveyFeedback smartSurveyFeedback;

    @Inject
    public VrmResource(
            MotrSession motrSession,
            TemplateEngine renderer,
            VehicleDetailsClient client,
            MotDueDateValidator motDueDateValidator,
            SmartSurveyFeedback smartSurveyFeedback
    ) {

        this.motrSession = motrSession;
        this.renderer = renderer;
        this.client = client;
        this.motDueDateValidator = motDueDateValidator;
        this.dataLayerHelper = new DataLayerHelper();
        this.smartSurveyFeedback = smartSurveyFeedback;
    }

    @GET
    public String vrmPageGet() throws Exception {

        String vrm = motrSession.getVrmFromSession();

        Map<String, Object> modelMap = new HashMap<>();
        updateMapBasedOnReviewFlow(modelMap);

        smartSurveyFeedback.addVrm(vrm);

        modelMap.put(VRM_MODEL_KEY, vrm);
        modelMap.putAll(smartSurveyFeedback.formatAttributes());
        smartSurveyFeedback.clear();

        return renderer.render("vrm", modelMap);
    }

    @POST
    public Response vrmPagePost(@FormParam("regNumber") String formParamVrm, @FormParam("honey") String formParamHoney) throws Exception {

        if (formParamHoney != null) {
            if (!formParamHoney.isEmpty()) {
                EventLogger.logEvent(new HoneyPotTriggeredEvent());
                return redirect("email-confirmation-pending");
            }
        }

        String vrm = normalizeFormInputVrm(formParamVrm);

        Map<String, Object> modelMap = new HashMap<>();
        dataLayerHelper.putAttribute(VRM_KEY, vrm);
        updateMapBasedOnReviewFlow(modelMap);

        modelMap.put(VRM_MODEL_KEY, vrm);
        modelMap.put(SHOW_INLINE_KEY, true);
        modelMap.put(SHOW_SYSTEM_ERROR, false);

        smartSurveyFeedback.addVrm(vrm);

        VrmValidator validator = new VrmValidator();
        TrailerVrmValidator trailerVrmValidator = new TrailerVrmValidator();
        if (validator.isValid(vrm)) {
            Boolean isTrailerSearch = trailerVrmValidator.isValid(vrm);

            try {
                Optional<VehicleDetails> vehicle = this.client.fetchByVrm(vrm);
                vehicle.ifPresent(dataLayerHelper::setVehicleDataOrigin);

                smartSurveyFeedback.addVrm(vrm);

                if (isFirstAnnualTestDateUnknown(vehicle)) {
                    motrSession.setVehicleDetails(vehicle.get());
                    if (isTrailer(vehicle)) {
                        return redirect(TrailerWithoutFirstAnnualTestResource.TRAILER_WITHOUT_FIRST_ANNUAL_TEST_PATH);
                    } else {
                        return redirect(UnknownTestDueDateResource.UNKNOWN_TEST_DATE_PATH);
                    }
                }
                if (vehicleDataIsValid(vehicle)) {
                    motrSession.setVrm(vrm);
                    motrSession.setVehicleDetails(vehicle.get());

                    if (testDateIsExpired(vehicle.get())) {
                        return redirect("test-expired");
                    }

                    return getRedirectAfterSuccessfulEdit();
                } else if (isTrailerSearch) {
                    addTrailerNotFoundErrorMessageToViewModel(modelMap);
                } else {
                    addVehicleNotFoundErrorMessageToViewModel(modelMap);
                }
            } catch (VehicleDetailsClientException exception) {

                EventLogger.logErrorEvent(new VehicleDetailsExceptionEvent().setVrm(vrm), exception);
                dataLayerHelper.setMessage(DataLayerMessageId.TRADE_API_CLIENT_EXCEPTION,
                        DataLayerMessageType.PUBLIC_API_REQUEST_ERROR,
                        "Something went wrong with the search. Try again later.");
                motrSession.setVrm(vrm);
                modelMap.put(SHOW_SYSTEM_ERROR, true);
            }
        } else {
            dataLayerHelper.setMessage(DataLayerMessageId.VRM_VALIDATION_ERROR,
                    DataLayerMessageType.USER_INPUT_ERROR,
                    validator.getMessage());
            modelMap.put(MESSAGE_KEY, validator.getMessage());
        }

        modelMap.put(VRM_MODEL_KEY, vrm);
        modelMap.put(INPUT_FIELD_ID_MODEL_KEY, INPUT_FIELD_ID);
        modelMap.putAll(dataLayerHelper.formatAttributes());
        modelMap.putAll(smartSurveyFeedback.formatAttributes());
        dataLayerHelper.clear();
        smartSurveyFeedback.clear();

        return Response.ok(renderer.render(VRM_TEMPLATE_NAME, modelMap)).build();
    }

    private boolean isFirstAnnualTestDateUnknown(Optional<VehicleDetails> vehicleDetails) {
        return vehicleDetails.filter(details ->
                details.getMotExpiryDate() == null
                && !(details.getVehicleType().equals(MOT))
        ).isPresent();
    }

    private void addVehicleNotFoundErrorMessageToViewModel(Map<String, Object> modelMap) {

        dataLayerHelper.setMessage(DataLayerMessageId.VEHICLE_NOT_FOUND,
                DataLayerMessageType.USER_INPUT_ERROR,
                VEHICLE_NOT_FOUND_MESSAGE);
        dataLayerHelper.clearVehicleDataOrigin();
        modelMap.put(MESSAGE_KEY, VEHICLE_NOT_FOUND_MESSAGE);
        modelMap.put(SHOW_INLINE_KEY, false);
    }

    private void addTrailerNotFoundErrorMessageToViewModel(Map<String, Object> modelMap) {

        dataLayerHelper.setMessage(DataLayerMessageId.TRAILER_NOT_FOUND,
                DataLayerMessageType.USER_INPUT_ERROR,
                TRAILER_NOT_FOUND_MESSAGE);
        dataLayerHelper.clearVehicleDataOrigin();
        modelMap.put(MESSAGE_KEY, TRAILER_NOT_FOUND_MESSAGE);
        modelMap.put(SHOW_INLINE_KEY, false);
    }

    private Response getRedirectAfterSuccessfulEdit() {

        if (this.motrSession.visitingFromReviewPage()) {
            return redirect("review");
        }

        if (motrSession.isAllowedOnChannelSelectionPage()) {
            return redirect("channel-selection");
        }
        return redirect("email");
    }

    private void updateMapBasedOnReviewFlow(Map<String, Object> modelMap) {

        if (this.motrSession.visitingFromReviewPage()) {
            modelMap.put("continue_button_text", "Save and return to review");
            modelMap.put("back_button_text", "Back");
            modelMap.put("back_url", "review");
        } else {
            modelMap.put("continue_button_text", "Continue");
            modelMap.put("back_button_text", "Back");
            modelMap.put("back_url", HomepageResource.HOMEPAGE_URL);
        }
    }

    private static String normalizeFormInputVrm(String formInput) {

        return formInput.replaceAll("\\s+", "").toUpperCase();
    }

    private boolean vehicleDataIsValid(Optional<VehicleDetails> vehicle) {

        return vehicle.isPresent() && motDueDateValidator.isDueDateValid(vehicle.get().getMotExpiryDate());
    }

    private boolean testDateIsExpired(VehicleDetails vehicle) {

        return !motDueDateValidator.isDueDateInTheFuture(vehicle.getMotExpiryDate());
    }

    private boolean isTrailer(Optional<VehicleDetails> vehicle) {
        return vehicle.isPresent()
                && VehicleType.isTrailer(vehicle.get().getVehicleType());
    }
}
