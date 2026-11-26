package com.example.academic.media;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.example.academic.common.ApiException;
import static org.junit.jupiter.api.Assertions.*;
class CloudinaryImageTest {
 @Test void rejectsFakeImage(){assertThrows(ApiException.class,()->CloudinaryMediaController.validate(new MockMultipartFile("file","x.png","image/png","<script>bad</script>".getBytes())));}
 @Test void rejectsEmptyImage(){assertThrows(ApiException.class,()->CloudinaryMediaController.validate(new MockMultipartFile("file",new byte[0])));}
 @Test void rejectsOversize(){assertThrows(ApiException.class,()->CloudinaryMediaController.validate(new MockMultipartFile("file",new byte[5242881])));}
 @Test void rejectsDimensions() throws Exception {var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8001,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);assertThrows(ApiException.class,()->CloudinaryMediaController.validate(new MockMultipartFile("file",out.toByteArray())));}
 @Test void acceptsPng() throws Exception {var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);assertArrayEquals(out.toByteArray(),CloudinaryMediaController.validate(new MockMultipartFile("file",out.toByteArray())));}
}
