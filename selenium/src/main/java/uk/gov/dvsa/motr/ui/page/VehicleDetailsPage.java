package uk.gov.dvsa.motr.ui.page;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import uk.gov.dvsa.motr.navigation.GotoUrl;
import uk.gov.dvsa.motr.ui.base.Page;

@GotoUrl("/vehicle-details")
public class VehicleDetailsPage extends Page {

    @FindBy(id = "regNumber")
    private WebElement vrmField;
    @FindBy(id = "continue")
    private WebElement continueButton;

    @Override
    protected String getIdentity() {
        return "What is your vehicle's registration (number plate)?";
    }

    public boolean isVrmContinueButtonDisplayed() {
        return continueButton.isDisplayed();
    }
}
