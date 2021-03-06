package uk.gov.dvsa.motr.web.component.subscription.model;

import uk.gov.dvsa.motr.vehicledetails.MotIdentification;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;

import java.time.LocalDate;

public class PendingSubscription {

    private String confirmationId;

    private String vrm;

    private ContactDetail contactDetail;

    private LocalDate motDueDate;

    private MotIdentification motIdentification;

    private VehicleType vehicleType;

    private String dvlaId;

    private String motTestNumber;

    public String getConfirmationId() {
        return confirmationId;
    }

    public PendingSubscription setConfirmationId(String id) {
        this.confirmationId = id;
        return this;
    }

    public String getVrm() {
        return vrm;
    }

    public PendingSubscription setVrm(String vrm) {
        this.vrm = vrm;
        return this;
    }

    public LocalDate getMotDueDate() {
        return motDueDate;
    }

    public PendingSubscription setMotDueDate(LocalDate motDueDate) {
        this.motDueDate = motDueDate;
        return this;
    }

    public MotIdentification getMotIdentification() {
        return motIdentification;
    }

    public PendingSubscription setMotIdentification(MotIdentification motIdentification) {
        this.motIdentification = motIdentification;
        return this;
    }

    public void setDvlaId(String dvlaId) {
        this.dvlaId = dvlaId;
        this.motIdentification = new MotIdentification(this.motTestNumber, dvlaId);
    }

    public void setMotTestNumber(String motTestNumber) {
        this.motTestNumber = motTestNumber;
        this.motIdentification = new MotIdentification(motTestNumber, this.dvlaId);
    }

    public ContactDetail getContactDetail() {
        return contactDetail;
    }

    public PendingSubscription setContactDetail(ContactDetail contactDetail) {
        this.contactDetail = contactDetail;
        return this;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public PendingSubscription setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
        return this;
    }
}
