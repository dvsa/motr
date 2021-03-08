package uk.gov.dvsa.motr.test.environmant.variables;

import org.junit.contrib.java.lang.system.EnvironmentVariables;

import uk.gov.dvsa.motr.notifier.SystemVariable;

import java.util.Optional;

import static uk.gov.dvsa.motr.notifier.SystemVariable.CHECKSUM_SALT;
import static uk.gov.dvsa.motr.notifier.SystemVariable.DB_TABLE_SUBSCRIPTION;
import static uk.gov.dvsa.motr.notifier.SystemVariable.EU_GO_LIVE_DATE;
import static uk.gov.dvsa.motr.notifier.SystemVariable.GOV_NOTIFY_API_TOKEN;
import static uk.gov.dvsa.motr.notifier.SystemVariable.HGV_PSV_NOTIFICATIONS;
import static uk.gov.dvsa.motr.notifier.SystemVariable.HGV_PSV_ONE_MONTH_NOTIFICATION_TEMPLATE_ID;
import static uk.gov.dvsa.motr.notifier.SystemVariable.HGV_PSV_TWO_MONTH_NOTIFICATION_TEMPLATE_ID;
import static uk.gov.dvsa.motr.notifier.SystemVariable.LOG_LEVEL;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MESSAGE_RECEIVE_TIMEOUT;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MESSAGE_VISIBILITY_TIMEOUT;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MOTH_DIRECT_URL_PREFIX;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MOT_API_DVLA_ID_URI;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MOT_API_HGV_PSV_URI;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MOT_API_MOT_TEST_NUMBER_URI;
import static uk.gov.dvsa.motr.notifier.SystemVariable.MOT_TEST_REMINDER_INFO_TOKEN;
import static uk.gov.dvsa.motr.notifier.SystemVariable.ONE_DAY_AFTER_NOTIFICATION_TEMPLATE_ID_POST_EU;
import static uk.gov.dvsa.motr.notifier.SystemVariable.ONE_MONTH_NOTIFICATION_TEMPLATE_ID_POST_EU;
import static uk.gov.dvsa.motr.notifier.SystemVariable.REGION;
import static uk.gov.dvsa.motr.notifier.SystemVariable.REMAINING_TIME_THRESHOLD;
import static uk.gov.dvsa.motr.notifier.SystemVariable.SMS_HGV_PSV_ONE_MONTH_NOTIFICATION_TEMPLATE_ID;
import static uk.gov.dvsa.motr.notifier.SystemVariable.SMS_HGV_PSV_TWO_MONTH_NOTIFICATION_TEMPLATE_ID;
import static uk.gov.dvsa.motr.notifier.SystemVariable.SMS_ONE_DAY_AFTER_NOTIFICATION_TEMPLATE_ID_POST_EU;
import static uk.gov.dvsa.motr.notifier.SystemVariable.SMS_ONE_MONTH_NOTIFICATION_TEMPLATE_ID_POST_EU;
import static uk.gov.dvsa.motr.notifier.SystemVariable.SMS_TWO_WEEK_NOTIFICATION_TEMPLATE_ID_POST_EU;
import static uk.gov.dvsa.motr.notifier.SystemVariable.SUBSCRIPTIONS_QUEUE_URL;
import static uk.gov.dvsa.motr.notifier.SystemVariable.TWO_WEEK_NOTIFICATION_TEMPLATE_ID_POST_EU;
import static uk.gov.dvsa.motr.notifier.SystemVariable.VEHICLE_API_CLIENT_TIMEOUT;
import static uk.gov.dvsa.motr.notifier.SystemVariable.WEB_BASE_URL;
import static uk.gov.dvsa.motr.notifier.SystemVariable.WORKER_COUNT;


public class TestEnvironmentVariables extends EnvironmentVariables {

    public TestEnvironmentVariables() {

        set(LOG_LEVEL, "INFO");
        set(REGION, region());
        set(DB_TABLE_SUBSCRIPTION, subscriptionTableName());
        set(SUBSCRIPTIONS_QUEUE_URL, sqsEndpoint());
        set(MOT_TEST_REMINDER_INFO_TOKEN, motTestReminderInfoToken());
        set(MOT_API_MOT_TEST_NUMBER_URI, motTestNumberApiEndpoint());
        set(MOT_API_DVLA_ID_URI, dvlaIdApiEndpoint());
        set(MOT_API_HGV_PSV_URI, hgvPsvApiEndpoint());
        set(HGV_PSV_TWO_MONTH_NOTIFICATION_TEMPLATE_ID, notifyHgvPsvTwoMonthTemplateId());
        set(HGV_PSV_ONE_MONTH_NOTIFICATION_TEMPLATE_ID, notifyHgvPsvOneMonthTemplateId());
        set(ONE_MONTH_NOTIFICATION_TEMPLATE_ID_POST_EU, notifyOneMonthTemplateIdPostEu());
        set(TWO_WEEK_NOTIFICATION_TEMPLATE_ID_POST_EU, notifyTwoWeekTemplateIdPostEu());
        set(ONE_DAY_AFTER_NOTIFICATION_TEMPLATE_ID_POST_EU, notifyOneDayAfterTemplateIdPostEu());
        set(SMS_ONE_MONTH_NOTIFICATION_TEMPLATE_ID_POST_EU, notifySmsOneMonthTemplateIdPostEu());
        set(SMS_TWO_WEEK_NOTIFICATION_TEMPLATE_ID_POST_EU, notifySmsTwoWeekTemplateIdPostEu());
        set(SMS_ONE_DAY_AFTER_NOTIFICATION_TEMPLATE_ID_POST_EU, notifySmsOneDayAfterTemplateIdPostEu());
        set(SMS_HGV_PSV_TWO_MONTH_NOTIFICATION_TEMPLATE_ID, notifySmsHgvPsvTwoMonthTemplateId());
        set(SMS_HGV_PSV_ONE_MONTH_NOTIFICATION_TEMPLATE_ID, notifySmsHgvPsvOneMonthTemplateId());

        set(EU_GO_LIVE_DATE, euGoLiveDate());

        set(GOV_NOTIFY_API_TOKEN, govNotifyApiToken());
        set(WORKER_COUNT, "1");
        set(MESSAGE_RECEIVE_TIMEOUT, "10");
        set(MESSAGE_VISIBILITY_TIMEOUT, "60");
        set(REMAINING_TIME_THRESHOLD, "20");
        set(VEHICLE_API_CLIENT_TIMEOUT, "10");
        set(WEB_BASE_URL, "");
        set(MOTH_DIRECT_URL_PREFIX, mothDirectUrlPrefix());
        set(CHECKSUM_SALT, checksumSalt());
        set(HGV_PSV_NOTIFICATIONS, hgvPsvNotifications());

    }

    public static String motTestNumberApiEndpoint() {
        return lookupProperty("test.mottestnumber.api.integration.endpoint");
    }

    public static String dvlaIdApiEndpoint() {
        return lookupProperty("test.dvlaId.api.integration.endpoint");
    }

    public static String hgvPsvApiEndpoint() {
        return lookupProperty("test.hgvpsv.api.integration.endpoint");
    }

    public static String subscriptionTableName() {

        return lookupProperty("test.dynamoDB.integration.table.subscription");
    }

    public static String region() {

        return lookupProperty("test.dynamoDB.integration.region");
    }

    public static String loaderFunctionName() {

        return lookupProperty("test.lambda.integration.loaderfunction");
    }

    public static String sqsEndpoint() {

        return lookupProperty("test.sqs.integration.endpoint");
    }

    public static String govNotifyApiToken() {

        return lookupProperty("test.notify.api.integration.token");
    }

    public static String motTestReminderInfoToken() {

        return lookupProperty("test.vehicle.api.integration.token");
    }

    public static String notifyTwoWeekTemplateId() {

        return lookupProperty("test.notify.template.two.week");
    }

    public static String notifyHgvPsvTwoMonthTemplateId() {

        return lookupProperty("test.notify.template.hgv.psv.two.month");
    }

    public static String notifyHgvPsvOneMonthTemplateId() {

        return lookupProperty("test.notify.template.hgv.psv.one.month");
    }

    public static String notifySmsHgvPsvTwoMonthTemplateId() {

        return lookupProperty("test.notify.template.sms.hgv.psv.two.month");
    }

    public static String notifySmsHgvPsvOneMonthTemplateId() {

        return lookupProperty("test.notify.template.sms.hgv.psv.one.month");
    }

    public static String notifyOneMonthTemplateId() {

        return lookupProperty("test.notify.template.one.month");
    }

    public static String notifyOneDayAfterTemplateId() {

        return lookupProperty("test.notify.template.one.day.after");
    }

    public static String notifySmsTwoWeekTemplateId() {

        return lookupProperty("test.notify.template.sms.two.week");
    }

    public static String notifySmsOneMonthTemplateId() {

        return lookupProperty("test.notify.template.sms.one.month");
    }

    public static String notifySmsOneDayAfterTemplateId() {

        return lookupProperty("test.notify.template.sms.one.day.after");
    }

    public static String notifyTwoWeekTemplateIdPostEu() {

        return lookupProperty("test.notify.template.two.week.posteu");
    }

    public static String notifyOneMonthTemplateIdPostEu() {

        return lookupProperty("test.notify.template.one.month.posteu");
    }

    public static String notifyOneDayAfterTemplateIdPostEu() {

        return lookupProperty("test.notify.template.one.day.after.posteu");
    }

    public static String notifySmsTwoWeekTemplateIdPostEu() {

        return lookupProperty("test.notify.template.sms.two.week.posteu");
    }

    public static String notifySmsOneMonthTemplateIdPostEu() {

        return lookupProperty("test.notify.template.sms.one.month.posteu");
    }

    public static String notifySmsOneDayAfterTemplateIdPostEu() {

        return lookupProperty("test.notify.template.sms.one.day.after.posteu");
    }

    public static String mothDirectUrlPrefix() {

        return lookupProperty("test.moth.direct.url.prefix");
    }

    public static String checksumSalt() {

        return lookupProperty("test.checksum.salt");
    }

    public static String euGoLiveDate() {

        return lookupProperty("test.eu.go.live.date");
    }

    public static String hgvPsvNotifications() {

        return lookupProperty("test.hgv.psv.notification");
    }

    private static String lookupProperty(String property) {

        return Optional.ofNullable(System.getProperty(property)).orElseThrow(
                () -> new RuntimeException("Property: " + property + " is not defined!")
        );
    }

    private void set(SystemVariable var, String value) {

        set(var.getName(), value);
    }

}
