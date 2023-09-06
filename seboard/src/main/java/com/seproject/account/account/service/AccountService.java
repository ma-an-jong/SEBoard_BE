package com.seproject.account.account.service;

import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.account.domain.repository.OAuthAccountRepository;
import com.seproject.account.account.persistence.AccountQueryRepository;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.board.common.Status;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.CustomUserNotFoundException;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.role.domain.repository.RoleRepository;
import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AccountQueryRepository accountQueryRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public boolean isExistLoginId(String loginId) {
        return accountQueryRepository.existsByLoginId(loginId);
    }
    public boolean isExistNickname(String nickname) {
        return accountQueryRepository.existsByNickname(nickname);
    }
    public boolean isOAuthUser(String loginId) {
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));
        return account.getClass() == OAuthAccount.class;
    }

    public boolean matchPassword(Account account, String password) {
       return passwordEncoder.matches(password,account.getPassword());
    }

    @Transactional
    public Long createOAuthAccount(String email,String name,String nickname,String password,List<Role> roles,String sub,String provider) {

        List<RoleAccount> roleAccounts = roles.stream()
                .map((role) -> new RoleAccount(null,role))
                .collect(Collectors.toList());

        OAuthAccount oAuthAccount = OAuthAccount.builder()
                .loginId(email)
                .name(name)
                .nickname(nickname)
                .password(passwordEncoder.encode(password))
                .roleAccounts(roleAccounts)
                .sub(sub)
                .provider(provider)
                .createdAt(LocalDateTime.now())
                .status(Status.NORMAL)
                .build();

        oAuthAccountRepository.save(oAuthAccount);

        return oAuthAccount.getAccountId();
    }

    @Transactional
    public Long createFormAccount(String email,String name,String nickname,String password,List<Role> roles) {

        List<RoleAccount> roleAccounts = roles.stream()
                .map((role) -> new RoleAccount(null, role))
                .collect(Collectors.toList());

        FormAccount formAccount = FormAccount.builder()
                .loginId(email)
                .name(name)
                .nickname(nickname)
                .password(passwordEncoder.encode(password))
                .roleAccounts(roleAccounts)
                .createdAt(LocalDateTime.now())
                .status(Status.NORMAL)
                .build();

        accountRepository.save(formAccount);
        return formAccount.getAccountId();
    }
    public Account findById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.USER_NOT_FOUND,null));
    }

    public Account findByLoginId(String loginId) {
        return accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.USER_NOT_FOUND,null));
    }

    public Optional<OAuthAccount> findOAuthAccountById(Long accountId) {
        return oAuthAccountRepository.findById(accountId);
    }

    @Transactional
    public Long createAccount(String loginId, String password,String name, String nickname,List<Role> roles) {
        List<RoleAccount> roleAccounts = roles.stream()
                .map((role) -> new RoleAccount(null, role))
                .collect(Collectors.toList());

        FormAccount account = FormAccount.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(password))
                .name(name)
                .nickname(nickname)
                .roleAccounts(roleAccounts)
                .status(Status.NORMAL)
                .createdAt(LocalDateTime.now())
                .build();

        accountRepository.save(account);
        return account.getAccountId();
    }
    @Transactional
    public Long updateAccount(Long accountId,String loginId, String password, String name, String nickname, List<Role> roles) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.USER_NOT_FOUND,null));

        List<RoleAccount> roleAccounts = roles.stream()
                .map((role) -> new RoleAccount(account,role))
                .collect(Collectors.toList());

        String encodedPassword = passwordEncoder.encode(password);

        account.update(
                loginId,
                encodedPassword,
                name,
                nickname,
                roleAccounts);

        return account.getAccountId();
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));

        if(isOAuthUser(account.getLoginId())) {
            OAuthAccount oAuthAccount = oAuthAccountRepository.findByLoginId(account.getLoginId())
                    .orElseThrow(() -> new RuntimeException("oAuth 유저 체크는 통과했으나 찾을수 없는 경우"));
            oAuthAccount.removeSub();
//            TODO : 외래키 제약조건 걸림 oAuthAccountRepository.delete(oAuthAccount);
        }

        account.delete(true);
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Account account1 = accountRepository.findByLoginId(username)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));

        Account account = accountRepository.findByLoginIdWithRole(username)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));
        return account;
    }

    @Transactional
    public Long resetPassword(String loginId, String newPassword) {
        Account account = accountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null));

        changePassword(account,newPassword);

        return account.getAccountId();
    }

    @Transactional
    public void changePassword(Account account, String newPassword) {
        account.changePassword(passwordEncoder.encode(newPassword));
    }

}
