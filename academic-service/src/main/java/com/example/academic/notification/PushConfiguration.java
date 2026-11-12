package com.example.academic.notification;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class PushConfiguration {
 @Value("${notifications.push.enabled:false}") private boolean enabled;
 @Value("${notifications.expo.project-id:}") private String projectId;
 @PostConstruct public void validate(){if(enabled){try{if(!UUID.fromString(projectId).toString().equals(projectId))throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new IllegalStateException("Push requires the configured Expo project UUID");}}}
}
