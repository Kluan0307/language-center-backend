package com.example.teaching.grading;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
@Configuration
public class GradingBootstrap {
 @Bean ApplicationRunner gradingTemplates(GradingService service){return args->service.initialize();}
}
