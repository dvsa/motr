package uk.gov.dvsa.motr.notifier.events;

public class HgvPsvTwoMonthEmailReminderEvent extends NotifyEvent {

    @Override
    public String getCode() {

        return "TWO-MONTH-HGV-PSV-EMAIL-SUCCESS";
    }
}
