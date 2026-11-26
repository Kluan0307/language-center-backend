package com.example.academic.media;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface MediaAssetRepository extends JpaRepository<MediaAsset,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from MediaAsset a where a.id=:id") Optional<MediaAsset> lock(@Param("id")Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from MediaAsset a where a.url=:url") Optional<MediaAsset> lockUrl(@Param("url")String url);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from MediaAsset a where a.publicId=:publicId") Optional<MediaAsset> lockPublicId(@Param("publicId")String publicId);
}
