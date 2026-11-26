package com.example.academic.course;

import jakarta.persistence.*;
import lombok.*;

/** Fixed business codes; display labels may change without changing identity. */
@Entity @Table(name="audience_groups",uniqueConstraints=@UniqueConstraint(name="uk_audience_code",columnNames="code"))
@Getter @Setter
public class AudienceGroup {
  public enum Code { PRESCHOOL, PRIMARY, SECONDARY, HIGH_SCHOOL, ADULT }
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Version private long version;
  @Column(nullable=false,length=32) private String code;
  @Column(nullable=false,length=100) private String name;
  @Column(nullable=false) private boolean active=true;
  @Column(nullable=false) private int sortOrder;
}
