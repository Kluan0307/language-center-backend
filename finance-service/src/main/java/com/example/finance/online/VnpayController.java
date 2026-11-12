package com.example.finance.online;
import com.example.finance.common.ApiException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/finance/payment-providers/vnpay")
public class VnpayController {
 private final VnpayProvider provider; private final ProviderInbox inbox; private final OnlineSettlement settlement; private final OnlineOrderRepository orders;
 @GetMapping("/ipn") public ResponseEntity<Map<String,String>> ipn(@RequestParam MultiValueMap<String,String> params){VnpayProvider.Verified value;try{value=provider.ipn(params);}catch(ApiException ex){if(ex.getStatus().value()==503)throw ex;return ResponseEntity.ok(Map.of("RspCode","97","Message","Invalid evidence"));}try{Long id=inbox.store(value,"IPN");String code=settlement.process(id,false);return ResponseEntity.ok(Map.of("RspCode",code,"Message",code.equals("00")?"Recorded":"Verification required"));}catch(RuntimeException ex){return ResponseEntity.status(503).body(Map.of("RspCode","99","Message","Retry required"));}}
 @GetMapping(value="/return",produces=MediaType.TEXT_HTML_VALUE)
 public ResponseEntity<String> back(@RequestParam MultiValueMap<String,String> params){
   String link="";
   try{var value=provider.ipn(params);var o=orders.findByMerchantRef(value.merchantRef()).orElse(null);
     if(o!=null&&o.getId().matches("[a-fA-F0-9-]{36}"))link="<p><a href=\"languagecenterstudent:///payment-orders/"+o.getId()+"\">Quay lại ứng dụng</a></p>";
   }catch(ApiException ignored){}
   String nonce=java.util.UUID.randomUUID().toString().replace("-","");
   String html="""
     <!doctype html><html lang="vi"><head><meta charset="utf-8">
     <meta name="viewport" content="width=device-width, initial-scale=1"><title>Language Center</title>
     <style nonce="%s">body{font-family:system-ui,sans-serif;background:#f5f7fb;color:#1f2937;margin:0;padding:24px}main{max-width:480px;margin:10vh auto;background:white;border-radius:20px;padding:28px}h1{font-size:24px}p{line-height:1.6;color:#475569}a{display:block;background:#2563eb;color:white;padding:14px;text-decoration:none;border-radius:12px;text-align:center}</style>
     </head><body><main><p>Language Center · VNPAY sandbox</p><h1>Kiểm tra kết quả thanh toán</h1>
     <p>Quay lại ứng dụng để xem kết quả và phiếu thu. Ứng dụng sẽ kiểm tra xác nhận từ trung tâm.</p>
     %s
     <p>Nếu nút không mở được ứng dụng hoặc bạn đang dùng Expo Go, hãy đóng trang này, quay lại app rồi mở Học phí → Lịch sử → Giao dịch online.</p>
     <p>Nếu ngân hàng đã trừ tiền nhưng chưa thấy phiếu thu, đừng thanh toán lại; hãy kiểm tra trạng thái hoặc liên hệ trung tâm.</p>
     </main></body></html>
     """.formatted(nonce,link);
   return ResponseEntity.ok().cacheControl(CacheControl.noStore())
     .header("Content-Security-Policy","default-src 'none'; style-src 'nonce-"+nonce+"'; base-uri 'none'; frame-ancestors 'none'")
     .header("X-Content-Type-Options","nosniff").body(html);
 }
}
