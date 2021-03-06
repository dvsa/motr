package uk.gov.dvsa.motr.web.cookie;

import java.io.Serializable;

abstract class SubscriptionParams implements Serializable {

    private String contact;
    private String expiryDate;
    private String registration;
    private String dvlaId;
    private String motTestNumber;
    private String contactType;

    public String getContact() {

        return contact;
    }

    public void setContact(String contact) {

        this.contact = contact;
    }

    public String getExpiryDate() {

        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {

        this.expiryDate = expiryDate;
    }

    public String getRegistration() {

        return registration;
    }

    public void setRegistration(String registration) {

        this.registration = registration;
    }

    public String getDvlaId() {

        return dvlaId;
    }

    public void setDvlaId(String dvlaId) {

        this.dvlaId = dvlaId;
    }

    public String getMotTestNumber() {

        return motTestNumber;
    }

    public void setMotTestNumber(String motTestNumber) {

        this.motTestNumber = motTestNumber;
    }

    public String getContactType() {

        return contactType;
    }

    public void setContactType(String contactType) {

        this.contactType = contactType;
    }
}
