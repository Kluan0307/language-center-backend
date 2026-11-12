package com.example.finance.online;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction,Long>{Optional<ProviderTransaction> findByMerchantCodeAndTransactionNo(String merchant,String number);}
