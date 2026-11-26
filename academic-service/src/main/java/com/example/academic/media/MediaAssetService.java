package com.example.academic.media;
import com.example.academic.common.*;
import com.example.academic.security.*;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
@Service @RequiredArgsConstructor
public class MediaAssetService {
 private final MediaAssetRepository repository;
 private final JdbcTemplate jdbc;
 private final jakarta.persistence.EntityManager em;
 public record LibraryItem(MediaAsset asset,boolean inUse,boolean canDelete){}
 @Transactional(readOnly=true) public java.util.List<LibraryItem> library(String category,Long branch,int page){
  if(!java.util.Set.of("courses","branches","teachers","banners").contains(category)||page<0||page>10000)throw ApiException.bad("Bộ lọc ảnh không hợp lệ");
  var u=Users.current();
  return em.createQuery("select a from MediaAsset a where a.deleted=false and a.category=:category and (a.branchId=:branch or a.branchId is null) order by a.id desc",MediaAsset.class).setParameter("category",category).setParameter("branch",branch).setFirstResult(page*20).setMaxResults(20).getResultStream().map(a->{boolean used=referenced(a.getUrl());boolean owner=u.role()==Role.SUPER_ADMIN||(a.getUploadedBy().equals(u.accountId())&&a.getBranchId()!=null&&u.branchIds().contains(a.getBranchId()));return new LibraryItem(a,used,!used&&owner);}).toList();
 }
 @Transactional public MediaAsset record(CloudinaryMediaController.Uploaded image){
  var a=new MediaAsset();a.setPublicId(image.publicId());a.setAssetId(image.assetId());a.setUrl(image.url());a.setCategory(image.category());a.setBranchId(image.branchId());a.setUploadedBy(image.uploadedBy());a.setBytes(image.bytes());a.setWidth(image.width());a.setHeight(image.height());a.setFormat(image.format());a.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));return repository.saveAndFlush(a);
 }
 private void access(MediaAsset a){var u=Users.current();if(u.role()!=Role.SUPER_ADMIN&&(!a.getUploadedBy().equals(u.accountId())||a.getBranchId()==null||!u.branchIds().contains(a.getBranchId())))throw ApiException.missing();}
 /** Called inside the CMS transaction, locking the asset against concurrent deletion. */
 @Transactional public void validateUse(String url,String kind,Long sourceId){
  if(url==null||url.isBlank())return;
  var asset=repository.lockUrl(url);
  if(asset.isEmpty())try{var uri=java.net.URI.create(url);if("res.cloudinary.com".equals(uri.getHost())){String last=uri.getPath().substring(uri.getPath().lastIndexOf('/')+1).split("\\.")[0];if(last.matches("lc_[a-f0-9]{32}"))asset=repository.lockPublicId(last);}}catch(IllegalArgumentException e){throw ApiException.bad("URL ảnh không hợp lệ");}
  if(asset.isEmpty())return; // Existing external URLs remain supported.
  if(!url.equals(asset.get().getUrl()))throw ApiException.bad("Hãy chọn URL gốc của ảnh đã tải lên từ thư viện");
  var a=asset.get();if(a.isDeleted())throw ApiException.conflict("Ảnh đã bị xóa. Hãy tải ảnh khác.");if(Users.current().role()!=Role.SUPER_ADMIN&&a.getBranchId()!=null&&!Users.current().branchIds().contains(a.getBranchId()))throw ApiException.missing();
  String category=switch(kind){case "TEACHER"->"teachers";case "BRANCH"->"branches";case "PROGRAM"->"banners";default->"courses";};
  if(!category.equals(a.getCategory()))throw ApiException.bad("Ảnh không thuộc nhóm nội dung này");
  Long branch=switch(kind){case "BRANCH"->sourceId;case "COURSE"->sourceBranch("courses",sourceId);case "CLASS"->sourceBranch("school_classes",sourceId);default->null;};
  if(a.getBranchId()!=null&&!a.getBranchId().equals(branch))throw ApiException.forbidden();
 }
 private Long sourceBranch(String table,Long id){
  var rows=jdbc.query("select branch_id from "+table+" where id=?",(r,n)->{long value=r.getLong(1);return r.wasNull()?null:Long.valueOf(value);},id);
  if(rows.isEmpty())throw ApiException.missing();return rows.get(0);
 }
 public boolean referenced(String url){
  return jdbc.queryForObject("select (select count(*) from website_publications where image_url=?)+(select count(*) from website_teacher_profiles where image_url=?)+(select count(*) from website_edit_events where locate(?,snapshot)>0)",Long.class,url,url,url)>0;
 }
 @Transactional public void delete(Long id,java.util.function.Consumer<String> destroy){
  var a=repository.lock(id).orElseThrow(ApiException::missing);access(a);if(a.isDeleted())return;
  if(referenced(a.getUrl()))throw ApiException.conflict("Ảnh còn trong nội dung hoặc lịch sử biên tập, chưa thể xóa vĩnh viễn.");
  destroy.accept(a.getPublicId());a.setDeleted(true);repository.saveAndFlush(a);
 }
}
