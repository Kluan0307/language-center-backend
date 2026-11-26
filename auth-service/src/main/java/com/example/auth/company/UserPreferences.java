package com.example.auth.company;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="user_preferences") @Getter @Setter
public class UserPreferences {
 @Id private Long accountId;
 private long version;
 private boolean compactTables;
 private int pageSize=10;
 @Column(nullable=false,length=10) private String calendarView="week";
 private Long defaultBranchId;
}
