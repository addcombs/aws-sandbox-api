package combs.adam.awssandboxapi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email_address", unique = true)
    private String emailAddress;

    @Column(name = "created_date", updatable = false)
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

    protected User() {
    }

    @PrePersist
    void setCreationDates() {
        Instant now = Instant.now();
        if (createdDate == null) {
            createdDate = now;
        }
        updatedDate = now;
    }

    @PreUpdate
    void setUpdatedDate() {
        updatedDate = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getUpdatedDate() {
        return updatedDate;
    }
}
