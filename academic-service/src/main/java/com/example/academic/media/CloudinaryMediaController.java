package com.example.academic.media;

import com.example.academic.common.ApiException;
import com.example.academic.security.BranchScope;
import com.example.academic.security.Users;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/** Public marketing images only. Private files and student avatars use separate flows. */
@RestController
@RequestMapping("/api/v1/academic/media")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class CloudinaryMediaController {
  private final BranchScope branches;
  private final MediaAssetService assets;
  @Value("${CLOUDINARY_CLOUD_NAME:}") private String cloud;
  @Value("${CLOUDINARY_API_KEY:}") private String key;
  @Value("${CLOUDINARY_API_SECRET:}") private String secret;
  @Value("${CLOUDINARY_FOLDER:language-center/dev}") private String folder;
  @Value("${CLOUDINARY_ENABLED:true}") private boolean enabled;
  public CloudinaryMediaController(BranchScope branches,MediaAssetService assets){this.branches=branches;this.assets=assets;}
  private boolean ready(){return enabled&&cloud.matches("[a-zA-Z0-9_-]+")&&!key.isBlank()&&!secret.isBlank()&&folder.matches("[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+)*");}
  @GetMapping("/config") public Map<String,Object> config(){return Map.of("enabled",ready(),"maxBytes",5242880,"formats",List.of("jpeg","png"),"visibility","PUBLIC");}
  @GetMapping("/images") public java.util.List<MediaAssetService.LibraryItem> images(@RequestParam String category,@RequestParam(required=false) Long branchId,@RequestParam(defaultValue="0") int page){return assets.library(category,branches.filter(branchId),page);}
  public record Uploaded(String assetId,String publicId,String url,String format,long bytes,int width,int height,String category,Long branchId,Long uploadedBy){}
  @PostMapping(value="/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public MediaAsset upload(@RequestPart("file") MultipartFile file,@RequestParam String category,@RequestParam(required=false) Long branchId){
    if(!Set.of("courses","branches","teachers","banners").contains(category))throw ApiException.bad("Unsupported public image category");
    Long branch=branches.filter(branchId);
    if(!ready())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Image upload is not configured");
    byte[] content=validate(file);
    String publicId="lc_"+UUID.randomUUID().toString().replace("-","");
    var form=new LinkedMultiValueMap<String,Object>();
    form.add("file",new ByteArrayResource(content){@Override public String getFilename(){return "image";}});
    form.add("public_id",publicId);form.add("asset_folder",folder+"/"+category);form.add("overwrite","false");
    form.add("tags","language-center,account-"+Users.current().accountId()+(branch==null?"":",branch-"+branch));
    var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(20));
    try{
      Map<?,?> response=RestClient.builder().requestFactory(factory).build().post().uri("https://api.cloudinary.com/v1_1/"+cloud+"/image/upload")
        .headers(h->h.setBasicAuth(key,secret)).contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(Map.class);
      if(response==null||!publicId.equals(response.get("public_id"))||!(response.get("secure_url") instanceof String url)||!url.startsWith("https://res.cloudinary.com/"+cloud+"/"))throw new IllegalStateException();
      var uploaded=new Uploaded((String)response.get("asset_id"),publicId,(String)response.get("secure_url"),(String)response.get("format"),((Number)response.get("bytes")).longValue(),((Number)response.get("width")).intValue(),((Number)response.get("height")).intValue(),category,branch,Users.current().accountId());
      try{return assets.record(uploaded);}catch(Exception persistence){destroy(publicId);throw persistence;}
    }catch(Exception e){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Image provider unavailable. Please retry later.");}
  }
  @DeleteMapping("/images/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id){if(!ready())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Image upload is not configured");assets.delete(id,this::destroy);}
  private void destroy(String publicId){
    var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(20));
    var form=new LinkedMultiValueMap<String,String>();form.add("public_id",publicId);form.add("invalidate","true");
    try{var response=RestClient.builder().requestFactory(factory).build().post().uri("https://api.cloudinary.com/v1_1/"+cloud+"/image/destroy").headers(h->h.setBasicAuth(key,secret)).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);if(response==null||!Set.of("ok","not found").contains(response.get("result")))throw new IllegalStateException();}
    catch(Exception e){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa thể xóa ảnh. Hãy thử lại sau.");}
  }
  static byte[] validate(MultipartFile file){
    if(file.isEmpty()||file.getSize()>5242880)throw ApiException.bad("Image must be between 1 byte and 5 MB");
    try{
      byte[] bytes=file.getBytes();
      try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
        var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw ApiException.bad("Only JPEG and PNG images are supported");
        var reader=readers.next();try{reader.setInput(stream);String format=reader.getFormatName().toLowerCase(Locale.ROOT);if(!Set.of("jpeg","png").contains(format)||reader.getWidth(0)>8000||reader.getHeight(0)>8000||(long)reader.getWidth(0)*reader.getHeight(0)>20000000)throw ApiException.bad("Unsupported image format or dimensions");}finally{reader.dispose();}
      }
      return bytes;
    }catch(ApiException e){throw e;}catch(Exception e){throw ApiException.bad("Invalid image");}
  }
}
