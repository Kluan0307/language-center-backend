package com.example.auth.company;
import jakarta.persistence.*;import lombok.*;
@Entity @Table(name="company_profile") @Getter @Setter
public class CompanyProfile {
 @Id private Long id;
 @Version private long version;
 @Column(nullable=false,length=120) private String name="Trung tâm ngoại ngữ";
 @Column(length=500) private String logoUrl;
 @Column(length=255) private String address;
 @Column(length=100) private String email;
 @Column(length=30) private String phone;
}
