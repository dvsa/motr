package uk.gov.dvsa.motr.web.system.binder;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import uk.gov.dvsa.motr.conversion.DataAnonymizer;
import uk.gov.dvsa.motr.notifications.service.NotifyService;
import uk.gov.dvsa.motr.web.analytics.SmartSurveyFeedback;
import uk.gov.dvsa.motr.web.analytics.SmartSurveySatisfactionSubscribe;
import uk.gov.dvsa.motr.web.analytics.SmartSurveySatisfactionUnsubscribe;
import uk.gov.dvsa.motr.web.component.subscription.helper.UrlHelper;
import uk.gov.dvsa.motr.web.component.subscription.service.PendingSubscriptionService;
import uk.gov.dvsa.motr.web.component.subscription.service.SmsConfirmationService;
import uk.gov.dvsa.motr.web.component.subscription.service.SubscriptionConfirmationService;
import uk.gov.dvsa.motr.web.component.subscription.service.SubscriptionsValidationService;
import uk.gov.dvsa.motr.web.component.subscription.service.UnsubscribeService;
import uk.gov.dvsa.motr.web.system.binder.factory.MotDueDateValidatorFactory;
import uk.gov.dvsa.motr.web.system.binder.factory.NotifyServiceFactory;
import uk.gov.dvsa.motr.web.validator.MotDueDateValidator;

public class ServiceBinder extends AbstractBinder {

    @Override
    protected void configure() {

        bindFactory(NotifyServiceFactory.class).to(NotifyService.class);
        bindFactory(MotDueDateValidatorFactory.class).to(MotDueDateValidator.class);
        bind(PendingSubscriptionService.class).to(PendingSubscriptionService.class);
        bind(SmsConfirmationService.class).to(SmsConfirmationService.class);
        bind(SubscriptionConfirmationService.class).to(SubscriptionConfirmationService.class);
        bind(UnsubscribeService.class).to(UnsubscribeService.class);
        bind(UrlHelper.class).to(UrlHelper.class);
        bind(SubscriptionsValidationService.class).to(SubscriptionsValidationService.class);
        bind(DataAnonymizer.class).to(DataAnonymizer.class);
        bind(SmartSurveySatisfactionSubscribe.class).to(SmartSurveySatisfactionSubscribe.class);
        bind(SmartSurveySatisfactionUnsubscribe.class).to(SmartSurveySatisfactionUnsubscribe.class);
        bind(SmartSurveyFeedback.class).to(SmartSurveyFeedback.class);
    }
}
