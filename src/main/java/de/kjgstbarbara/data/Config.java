package de.kjgstbarbara.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Config {// TODO Setup Menu on first Start and for System Admins
    @Id
    private String id;
    private String value;

    public enum Key {
        SENDER_NATIONAL_NUMBER,
        SENDER_REGION_CODE,
        SENDER_EMAIL_ADDRESS,
        SENDER_NAME,
        SMTP_SERVER,
        SMTP_PORT,
        SMTP_PASSWORD,
        SETUP_DONE
    }
}