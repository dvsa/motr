package uk.gov.dvsa.motr.notifier.notify;

import org.junit.Before;
import org.junit.Test;

import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotifyEmailServiceTest {

    private static final String EMAIL = "test@test.com";
    private static final String REG = "TEST-REG";
    private static final String UNSUBSCRIBE_LINK = "http://gov.uk";
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2017, 10, 10);
    private static final String PRESERVATION_STATEMENT_PREFIX =
            "You can get your MOT test done from tomorrow to keep the same expiry date ";
    private static final String PRESERVATION_STATEMENT_SUFFIX = " for next year.";

    private NotificationClient notificationClient = mock(NotificationClient.class);
    private String oneMonthNotificationTemplateId = "TEMPLATE-ONE-MONTH";
    private String twoWeekNotificationTemplateId = "TEMPLATE-TWO-WEEK";
    private String oneDayAfterNotificationTemplateId = "TEMPLATE-ONE-DAY-AFTER";

    private NotifyEmailService notifyEmailService;

    @Before
    public void setUp() {
        notifyEmailService = new NotifyEmailService(notificationClient, oneMonthNotificationTemplateId, twoWeekNotificationTemplateId,
                oneDayAfterNotificationTemplateId);
    }

    @Test
    public void oneMonthNotificationIsSentWithCorrectDetails_whenMotTestNumber() throws NotificationClientException {

        notifyEmailService.sendOneMonthNotificationEmail(EMAIL, REG, EXPIRY_DATE, UNSUBSCRIBE_LINK, "");

        StringBuilder preservationStatementSb = new StringBuilder(128)
                .append(PRESERVATION_STATEMENT_PREFIX)
                .append(DateFormatterForEmailDisplay.asFormattedForEmailDateWithoutYear(EXPIRY_DATE))
                .append(PRESERVATION_STATEMENT_SUFFIX);

        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("mot_expiry_date", DateFormatterForEmailDisplay.asFormattedForEmailDate(EXPIRY_DATE));
        personalisation.put("is_due_or_expires", "expires");
        personalisation.put("preservation_statement", preservationStatementSb.toString());

        verify(notificationClient, times(1)).sendEmail(
                oneMonthNotificationTemplateId,
                EMAIL,
                personalisation,
                ""
        );
    }

    @Test
    public void oneMonthNotificationIsSentWithCorrectDetails_whenDvlaId() throws NotificationClientException {

        notifyEmailService.sendOneMonthNotificationEmail(EMAIL, REG, EXPIRY_DATE, UNSUBSCRIBE_LINK, "12234");

        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("mot_expiry_date", DateFormatterForEmailDisplay.asFormattedForEmailDate(EXPIRY_DATE));
        personalisation.put("is_due_or_expires", "is due");
        personalisation.put("preservation_statement", "");

        verify(notificationClient, times(1)).sendEmail(
                oneMonthNotificationTemplateId,
                EMAIL,
                personalisation,
                ""
        );
    }

    @Test
    public void twoWeekNotificationIsSentWithCorrectDetails_whenMotTestNumber() throws NotificationClientException {

        notifyEmailService.sendTwoWeekNotificationEmail(EMAIL, REG, EXPIRY_DATE, UNSUBSCRIBE_LINK, "");

        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("mot_expiry_date", DateFormatterForEmailDisplay.asFormattedForEmailDate(EXPIRY_DATE));
        personalisation.put("is_due_or_expires", "expires");

        verify(notificationClient, times(1)).sendEmail(
                twoWeekNotificationTemplateId,
                EMAIL,
                personalisation,
                ""
        );
    }

    @Test
    public void twoWeekNotificationIsSentWithCorrectDetails_whenDvlaId() throws NotificationClientException {

        notifyEmailService.sendTwoWeekNotificationEmail(EMAIL, REG, EXPIRY_DATE, UNSUBSCRIBE_LINK, "122133");

        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("mot_expiry_date", DateFormatterForEmailDisplay.asFormattedForEmailDate(EXPIRY_DATE));
        personalisation.put("is_due_or_expires", "is due");

        verify(notificationClient, times(1)).sendEmail(
                twoWeekNotificationTemplateId,
                EMAIL,
                personalisation,
                ""
        );
    }

    @Test
    public void oneDayAfterNotificationIsSentWithCorrectDetails_whenMotTestNumber() throws NotificationClientException {


        notifyEmailService.sendOneDayAfterNotificationEmail(EMAIL, REG, EXPIRY_DATE, UNSUBSCRIBE_LINK, "");
        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("was_due_or_expired", "expired");

        verify(notificationClient, times(1)).sendEmail(
                oneDayAfterNotificationTemplateId,
                EMAIL,
                personalisation,
                "");
    }

    @Test
    public void oneDayAfterNotificationIsSentWithCorrectDetails_whenDvlaId() throws NotificationClientException {


        notifyEmailService.sendOneDayAfterNotificationEmail(EMAIL, REG, EXPIRY_DATE, UNSUBSCRIBE_LINK, "123456");
        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("was_due_or_expired", "was due");

        verify(notificationClient, times(1)).sendEmail(
                oneDayAfterNotificationTemplateId,
                EMAIL,
                personalisation,
                "");
    }

    @Test(expected = NotificationClientException.class)
    public void whenClientThrowsAnErrorItIsThrown() throws NotificationClientException {

        when(notificationClient.sendEmail(any(), any(), any(), any())).thenThrow(NotificationClientException.class);

        notifyEmailService.sendOneMonthNotificationEmail(
                "",
                "",
                LocalDate.of(2017, 10, 10),
                "",
                ""
        );
    }

    private Map<String, String> stubGenericPersonalisation() {

        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("vehicle_details", REG);
        personalisation.put("unsubscribe_link", UNSUBSCRIBE_LINK);

        return personalisation;
    }
}