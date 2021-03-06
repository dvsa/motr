package uk.gov.dvsa.motr.web.resource;

import uk.gov.dvsa.motr.vehicledetails.VehicleDetails;
import uk.gov.dvsa.motr.web.analytics.DataLayerHelper;
import uk.gov.dvsa.motr.web.analytics.DataLayerMessageId;
import uk.gov.dvsa.motr.web.analytics.DataLayerMessageType;
import uk.gov.dvsa.motr.web.analytics.SmartSurveyFeedback;
import uk.gov.dvsa.motr.web.component.subscription.model.Subscription;
import uk.gov.dvsa.motr.web.cookie.MotrSession;
import uk.gov.dvsa.motr.web.formatting.PhoneNumberFormatter;
import uk.gov.dvsa.motr.web.render.TemplateEngine;
import uk.gov.dvsa.motr.web.validator.PhoneNumberValidator;

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
@Path("/phone-number")
@Produces("text/html")
public class PhoneNumberResource {

    private static final String PHONE_NUMBER_TEMPLATE = "phone-number";
    private static final String PHONE_NUMBER_MODEL_KEY = "phoneNumber";
    private static final String MESSAGE_MODEL_KEY = "message";
    private static final String MESSAGE_AT_FIELD_MODEL_KEY = "messageAtField";
    private static final String INPUT_FIELD_ID = "phone-number-input";
    private static final String INPUT_FIELD_ID_MODEL_KEY = "inputFieldId";

    private final TemplateEngine renderer;
    private final MotrSession motrSession;
    private final PhoneNumberValidator validator;
    private final DataLayerHelper dataLayerHelper;
    private final SmartSurveyFeedback smartSurveyFeedback;

    @Inject
    public PhoneNumberResource(
            MotrSession motrSession,
            TemplateEngine renderer,
            PhoneNumberValidator validator,
            SmartSurveyFeedback smartSurveyFeedback
    ) {
        this.motrSession = motrSession;
        this.renderer = renderer;
        this.dataLayerHelper = new DataLayerHelper();
        this.validator = validator;
        this.smartSurveyFeedback = smartSurveyFeedback;
    }

    @GET
    public Response phoneNumberPageGet() throws Exception {

        if (!motrSession.isAllowedOnPhoneNumberEntryPage()) {
            return redirect(HomepageResource.HOMEPAGE_URL);
        }

        if (!motrSession.visitingFromReviewPage()) {
            motrSession.setVisitingFromContactEntry(true);
        }

        Map<String, Object> modelMap = new HashMap<>();
        ReviewFlowUpdater.updateMapBasedOnReviewFlow(
                modelMap,
                motrSession.visitingFromContactEntryPage(),
                motrSession.visitingFromReviewPage()
        );

        String phoneNumber = motrSession.getUnnormalizedPhoneNumberFromSession();
        modelMap.put(PHONE_NUMBER_MODEL_KEY, phoneNumber);

        addDetailsForSurveyFromSession(modelMap);

        return Response.ok(renderer.render(PHONE_NUMBER_TEMPLATE, modelMap)).build();
    }

    @POST
    public Response phoneNumberPagePost(@FormParam("phoneNumber") String phoneNumber) throws Exception {

        phoneNumber = PhoneNumberFormatter.trimWhitespace(phoneNumber);

        if (validator.isValid(phoneNumber)) {
            String normalizedUkPhoneNumber = PhoneNumberFormatter.normalizeUkPhoneNumber(phoneNumber);

            motrSession.setChannel("text");
            motrSession.setPhoneNumber(normalizedUkPhoneNumber);
            motrSession.setUnnormalizedPhoneNumber(phoneNumber);

            return redirect("review");
        }

        Map<String, Object> modelMap = new HashMap<>();

        modelMap.put(MESSAGE_MODEL_KEY, validator.getMessage());
        modelMap.put(MESSAGE_AT_FIELD_MODEL_KEY, validator.getMessageAtField());
        dataLayerHelper.setMessage(DataLayerMessageId.PHONE_NUMBER_VALIDATION_ERROR,
                DataLayerMessageType.USER_INPUT_ERROR,
                validator.getMessage());
        ReviewFlowUpdater.updateMapBasedOnReviewFlow(
                modelMap,
                motrSession.visitingFromContactEntryPage(),
                motrSession.visitingFromReviewPage()
        );

        modelMap.put(PHONE_NUMBER_MODEL_KEY, phoneNumber);
        modelMap.put(INPUT_FIELD_ID_MODEL_KEY, INPUT_FIELD_ID);
        modelMap.putAll(dataLayerHelper.formatAttributes());
        dataLayerHelper.clear();

        addDetailsForSurveyFromSession(modelMap);

        return Response.ok(renderer.render(PHONE_NUMBER_TEMPLATE, modelMap)).build();
    }

    private void addDetailsForSurveyFromSession(Map<String, Object> modelMap) {

        VehicleDetails vehicle = motrSession.getVehicleDetailsFromSession();
        smartSurveyFeedback.addContactType(Subscription.ContactType.MOBILE.getValue());
        smartSurveyFeedback.addVrm(vehicle.getRegNumber());
        smartSurveyFeedback.addVehicleType(vehicle.getVehicleType());
        smartSurveyFeedback.addIsSigningBeforeFirstMotDue(vehicle.hasNoMotYet());

        modelMap.putAll(smartSurveyFeedback.formatAttributes());
        smartSurveyFeedback.clear();
    }
}
