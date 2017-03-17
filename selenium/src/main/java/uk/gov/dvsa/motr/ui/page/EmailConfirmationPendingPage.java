package uk.gov.dvsa.motr.ui.page;

import uk.gov.dvsa.motr.navigation.GotoUrl;
import uk.gov.dvsa.motr.ui.base.Page;

@GotoUrl("/email-confirmation-pending")
public class EmailConfirmationPendingPage extends Page {

    @Override
    protected String getIdentity() {

        return "You’ve nearly finished";
    }
}
