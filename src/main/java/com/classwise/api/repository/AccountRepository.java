package com.classwise.api.repository;

import com.classwise.api.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByProviderIdAndAccountId(String providerId, String accountId);

    Optional<Account> findByUserIdAndProviderId(String userId, String providerId);
}
