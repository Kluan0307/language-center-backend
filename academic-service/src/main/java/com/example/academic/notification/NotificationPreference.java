package com.example.academic.notification;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="student_notification_preferences") @Getter @Setter
public class NotificationPreference {
 @Id private Long accountId;
 private boolean pushEnabled=false; private boolean schedule=true; private boolean grades=true; private boolean tuition=true; private boolean payment=true; private boolean documents=true; private boolean support=true;
 @Version private long version;
 public boolean allows(String c){return pushEnabled&&switch(c){case "SCHEDULE"->schedule;case "GRADES"->grades;case "TUITION"->tuition;case "PAYMENT"->payment;case "DOCUMENTS"->documents;case "SUPPORT"->support;default->false;};}
}
