package dk.digitalidentity.sofd.dao.model.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "view_callcenter")
@Getter
public class Callcenter {

    @Id
    private String uuid;

    @Column
    private String type;

    @Column
    private String name;

    @Column
    private String orgUnit;

    @Column
    private String userId;

    @Column
    private String keywords;

    @Column
    private String phone;

    @Column
    @JsonIgnore
    private String phoneNumbers;

    @Column
    private String email;

    @Column
    private String address;

    @Column
    private String positionName;

    @Column
    private String openingHours;
    
    @Column
    private String orgUnitPhone;
    
    @Column
    private String managerName;
    
    @Column
    private String managerPhone;
    
    @Column
    private String notes;

    public String[] getOtherPhones() { return phoneNumbers != null ? phoneNumbers.split(",") : null; }

}
