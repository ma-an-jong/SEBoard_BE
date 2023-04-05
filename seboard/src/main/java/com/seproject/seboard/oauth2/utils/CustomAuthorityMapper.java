package com.seproject.seboard.oauth2.utils;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.Collection;
import java.util.HashSet;

public class CustomAuthorityMapper implements GrantedAuthoritiesMapper {

    private static final String prefix = "ROLE_";
    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        HashSet<GrantedAuthority> mapped = new HashSet<>(authorities.size());

        for (GrantedAuthority authority : authorities) {
            mapped.add(mapAuthority(authority.getAuthority()));
        }

        return mapped;
    }

    private GrantedAuthority mapAuthority(String name) { //google : http://google.com/asddasfasd.email
        if(name.lastIndexOf(".") > 0) {
            int i = name.lastIndexOf(".");
            name = "SCOPE_" + name.substring(i + 1);
        }

        if(prefix.length() > 0 && !name.startsWith(prefix)) {
            name = prefix + name;
        }

        return new SimpleGrantedAuthority(name);
    }
}