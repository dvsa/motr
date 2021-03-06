package uk.gov.dvsa.motr.web.component.subscription.model;

import uk.gov.dvsa.motr.vehicledetails.MotIdentification;
import uk.gov.dvsa.motr.vehicledetails.VehicleType;

public class CancelledSubscription {
    private String unsubscribeId;

    private String vrm;

    private ContactDetail contactDetail;

    private MotIdentification motIdentification;

    private VehicleType vehicleType;

    private String reasonForCancellation;

    public String getUnsubscribeId() {
        return unsubscribeId;
    }

    public CancelledSubscription setUnsubscribeId(String id) {
        this.unsubscribeId = id;
        return this;
    }

    public String getVrm() {
        return vrm;
    }

    public CancelledSubscription setVrm(String vrm) {
        this.vrm = vrm;
        return this;
    }

    public ContactDetail getContactDetail() {
        return contactDetail;
    }

    public CancelledSubscription setContactDetail(ContactDetail contactDetail) {
        this.contactDetail = contactDetail;
        return this;
    }

    public String getReasonForCancellation() {
        return reasonForCancellation;
    }

    public CancelledSubscription setReasonForCancellation(String reasonForCancellation) {
        this.reasonForCancellation = reasonForCancellation;
        return this;
    }

    public MotIdentification getMotIdentification() {
        return motIdentification;
    }

    public CancelledSubscription setMotIdentification(MotIdentification motIdentification) {
        this.motIdentification = motIdentification;
        return this;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public CancelledSubscription setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
        return this;
    }
}
