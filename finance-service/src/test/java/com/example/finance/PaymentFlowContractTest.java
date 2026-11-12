package com.example.finance;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.finance.common.GlobalExceptionHandler;
import com.example.finance.online.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PaymentFlowContractTest {
  OnlineOrderService service;
  MockMvc mvc;
  @BeforeEach void setup(){
    service=mock(OnlineOrderService.class);
    mvc=MockMvcBuilders.standaloneSetup(new OnlineController(service,mock(ReconcileService.class),mock(ReconcileJournal.class)))
      .setControllerAdvice(new GlobalExceptionHandler()).build();
  }
  @Test void explicitRejectionHasStableCodeAndOwnedOrderId() throws Exception {
    when(service.create(any(),anyString())).thenThrow(new PaymentFlowException(HttpStatus.CONFLICT,"PAYMENT_ALREADY_PENDING","Existing checkout","owned-id"));
    mvc.perform(post("/api/v1/finance/student/me/payment-orders").contentType(MediaType.APPLICATION_JSON)
      .content("{\"requestId\":\"11111111-1111-4111-8111-111111111111\",\"tuitionId\":13,\"amount\":\"1000.00\",\"currency\":\"VND\"}"))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_PENDING"))
      .andExpect(jsonPath("$.requestRejected").value(true)).andExpect(jsonPath("$.orderId").value("owned-id"));
  }
  @Test void contextAndBatchRoutesAreReadOnlyContracts() throws Exception {
    var context=new OnlineOrderService.PaymentContext(13L,"PAY",null,"Ready","1000.00","VND",null,true);
    when(service.context(13L)).thenReturn(context);when(service.contexts(List.of(13L,14L))).thenReturn(List.of(context));
    mvc.perform(get("/api/v1/finance/student/me/tuitions/13/payment-context")).andExpect(status().isOk()).andExpect(jsonPath("$.action").value("PAY"));
    mvc.perform(get("/api/v1/finance/student/me/payment-contexts").param("tuitionIds","13,14")).andExpect(status().isOk()).andExpect(jsonPath("$[0].tuitionId").value(13));
    verify(service,never()).create(any(),anyString());
  }
  @Test void summaryIsOptInToPreserveExistingClients() throws Exception {
    mvc.perform(get("/api/v1/finance/student/me/payment-orders").param("summary","true")).andExpect(status().isOk());
    verify(service).list(true,0,20,null,null,null,true);
    mvc.perform(get("/api/v1/finance/student/me/payment-orders")).andExpect(status().isOk());
    verify(service).list(true,0,20,null,null,null,false);
  }
}
