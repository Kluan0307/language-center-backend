package com.example.academic.website;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/academic")
public class WebsiteTeacherController {
  private final WebsiteTeacherService service;
  private final com.example.academic.media.MediaAssetService media;
  @GetMapping("/website/teacher-sources") @PreAuthorize("hasRole('SUPER_ADMIN')")
  public List<WebsiteCatalogService.SourceOption> sources(@RequestParam(defaultValue="0") int page){return service.sources(page);}
  @GetMapping("/website/teacher-history/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
  public List<WebsiteEditEvent> history(@PathVariable Long id){return service.history(id);}
  @GetMapping("/public/teachers")
  public ResponseEntity<WebsiteTeacherService.Page> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,@RequestParam(required=false) String branch){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(page,size,branch));}
  @GetMapping("/public/teachers/{slug}")
  public ResponseEntity<WebsiteTeacherService.Profile> detail(@PathVariable String slug){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(slug));}
  @GetMapping("/website/teachers") @PreAuthorize("hasRole('SUPER_ADMIN')")
  public List<WebsiteTeacherProfile> edits(@RequestParam(defaultValue="0") int page){return service.edits(page);}
  @PutMapping("/website/teachers") @PreAuthorize("hasRole('SUPER_ADMIN')")
  @org.springframework.transaction.annotation.Transactional
  public WebsiteTeacherProfile edit(@Valid @RequestBody WebsiteTeacherService.Edit r){media.validateUse(r.imageUrl(),"TEACHER",r.teacherId());return service.edit(r);}
}
