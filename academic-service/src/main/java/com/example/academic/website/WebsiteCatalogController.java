package com.example.academic.website;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/academic")
public class WebsiteCatalogController {
  private final WebsiteCatalogService service;
  private final com.example.academic.media.MediaAssetService media;
  @GetMapping("/public/audiences")
  public ResponseEntity<List<com.example.academic.course.CourseClassificationView.Audience>> audiences(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.audiences());}
  // Enum paths intentionally use PROGRAM/COURSE/BRANCH/CLASS consistently in both APIs.
  @GetMapping("/public/catalog/{kind}")
  public ResponseEntity<WebsiteDto.Page> list(@PathVariable WebsitePublication.Kind kind,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="24") int size,@RequestParam(required=false) String program,@RequestParam(required=false) String course,@RequestParam(required=false) String branch,@RequestParam(required=false) String keyword,@RequestParam(required=false) java.math.BigDecimal maxTuition,@RequestParam(required=false) String audience){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(audience==null?service.list(kind,page,size,program,course,branch,keyword,maxTuition):service.list(kind,page,size,program,course,branch,keyword,maxTuition,audience));}
  @GetMapping("/public/catalog/{kind}/{slug}")
  public ResponseEntity<WebsiteDto.Item> detail(@PathVariable WebsitePublication.Kind kind,@PathVariable String slug){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(kind,slug));}
  @GetMapping("/website/publications/{kind}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public List<WebsitePublication> edits(@PathVariable WebsitePublication.Kind kind,@RequestParam(defaultValue="0") int page){return service.edits(kind,page);}
  @PutMapping("/website/publications/{kind}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  @org.springframework.transaction.annotation.Transactional
  public WebsitePublication edit(@PathVariable WebsitePublication.Kind kind,@Valid @RequestBody WebsiteDto.Edit body){media.validateUse(body.imageUrl(),kind.name(),body.sourceId());return service.edit(kind,body);}
  @GetMapping("/website/sources/{kind}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public List<WebsiteCatalogService.SourceOption> sources(@PathVariable WebsitePublication.Kind kind,@RequestParam(defaultValue="0") int page){return service.sources(kind,page);}
  @GetMapping("/website/history/{kind}/{id}") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public List<WebsiteEditEvent> history(@PathVariable WebsitePublication.Kind kind,@PathVariable Long id){return service.history(kind,id);}
}
