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

public class NotifySmsServiceTest {

    private static final String PHONE_NUMBER = "0700000000";
    private static final String REG = "TEST-REG";
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2017, 10, 10);

    private NotificationClient notificationClient = mock(NotificationClient.class);
    private String oneMonthNotificationTemplateId = "TEMPLATE-ONE-MONTH";
    private String twoWeekNotificationTemplateId = "TEMPLATE-TWO-WEEK";
    private String oneDayAfterNotificationTemplateId = "TEMPLATE-ONE-DAY-AFTER";

    private NotifySmsService notifySmsService;

    @Before
    public void setUp() {
        notifySmsService = new NotifySmsService(notificationClient, oneMonthNotificationTemplateId, twoWeekNotificationTemplateId,
                oneDayAfterNotificationTemplateId);
    }

    @Test
    public void oneMonthNotificationIsSentWithCorrectDetails() throws NotificationClientException {

        notifySmsService.sendOneMonthNotificationSms(PHONE_NUMBER, REG, EXPIRY_DATE);

        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("mot_expiry_date", DateFormatterForSmsDisplay.asFormattedForSmsDate(EXPIRY_DATE));

        verify(notificationClient, times(1)).sendSms(
                oneMonthNotificationTemplateId,
                PHONE_NUMBER,
                personalisation,
                ""
        );
    }

    @Test
    public void twoWeekNotificationIsSentWithCorrectDetails() throws NotificationClientException {

        notifySmsService.sendTwoWeekNotificationSms(PHONE_NUMBER, REG, EXPIRY_DATE);

        Map<String, String> personalisation = stubGenericPersonalisation();
        personalisation.put("mot_expiry_date", DateFormatterForSmsDisplay.asFormattedForSmsDate(EXPIRY_DATE));

        verify(notificationClient, times(1)).sendSms(
                twoWeekNotificationTemplateId,
                PHONE_NUMBER,
                personalisation,
                ""
        );
    }

    @Test
    public void oneDayAfterNotificationIsSentWithCorrectDetails() throws NotificationClientException {
        
        notifySmsService.sendOneDayAfterNotificationSms(PHONE_NUMBER, REG);
        Map<String, String> personalisation = stubGenericPersonalisation();

        verify(notificationClient, times(1)).sendSms(
                oneDayAfterNotificationTemplateId,
                PHONE_NUMBER,
                personalisation,
                "");
    }

    @Test(expected = NotificationClientException.class)
    public void whenClientThrowsAnErrorItIsThrown() throws NotificationClientException {

        when(notificationClient.sendSms(any(), any(), any(), any())).thenThrow(NotificationClientException.class);

        notifySmsService.sendOneMonthNotificationSms(
                "",
                "",
                LocalDate.of(2017, 10, 10)
        );
    }

    private Map<String, String> stubGenericPersonalisation() {

        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("vehicle_vrm", REG);
        return personalisation;
    }
}
