package com.example.teaching.grading;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.example.teaching.grading.GradingModels.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/teaching/grading")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER')")
public class GradingController {
 private final GradingService service;
 @GetMapping("/templates") public List<TemplateView> templates(){return service.templates();}
 @PostMapping("/templates") @PreAuthorize("hasRole('SUPER_ADMIN')") public TemplateView revise(@Valid @RequestBody TemplateCommand r){return service.revise(r);}
 @GetMapping("/course-default") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public GradingCourseDefault course(@RequestParam Long courseId,@RequestParam Long branchId){return service.courseDefault(courseId,branchId);}
 @PutMapping("/course-default") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public GradingCourseDefault course(@Valid @RequestBody CourseAssignment r){return service.courseDefault(r);}
 @GetMapping("/classes/{classId}/binding") public BindingView binding(@PathVariable Long classId){return service.binding(classId);}
 @PutMapping("/classes/{classId}/binding") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") public BindingView assign(@PathVariable Long classId,@Valid @RequestBody Assignment r){return service.assign(classId,r);}
 @GetMapping("/classes/{classId}/activities") public List<ActivityView> list(@PathVariable Long classId){return service.activities(classId);}
 @PostMapping("/classes/{classId}/activities") public ActivityView create(@PathVariable Long classId,@Valid @RequestBody ActivityCommand r){return service.create(classId,r);}
 @PatchMapping("/classes/{classId}/activities/{id}") public ActivityView edit(@PathVariable Long classId,@PathVariable Long id,@Valid @RequestBody ActivityEdit r){return service.edit(classId,id,r);}
 @GetMapping("/classes/{classId}/activities/{id}/sheet") public Sheet sheet(@PathVariable Long classId,@PathVariable Long id){return service.sheet(classId,id);}
 @PutMapping("/classes/{classId}/activities/{id}/marks") public Sheet marks(@PathVariable Long classId,@PathVariable Long id,@Valid @RequestBody SaveMarks r){return service.save(classId,id,r);}
 @GetMapping("/classes/{classId}/history") public List<GradingEvent> history(@PathVariable Long classId){return service.history(classId);}
}
