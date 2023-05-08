package com.seproject.account.controller;

import com.seproject.account.jwt.JwtDecoder;
import com.seproject.account.utils.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashMap;


@Slf4j
@AllArgsConstructor
@RestController
public class IndexController {

    @Autowired
    private JwtDecoder jwtDecoder;

    @GetMapping("/index")
    public String index() {
        return "<!DOCTYPE html>\n" +
                "\n" +
                "<html lang=\"ko\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Title</title>\n" +
                "</head>\n" +
                "    <body>\n" +
                "        <h1>jwt decode</h1>\n" +
                "            <form action=\"/\" method = \"POST\">\n" +
                "            <input type=\"text\" name=\"jwt\" id=\"jwt\">\n" +
                "\n" +
                "            <button>send</button>\n" +
                "            </form>\n" +
                "        <hr/>\n" +
                "\n" +
                "        <h1>로그인</h1>\n" +
                "        <!-- 시큐리티는 x-www-form-url-encoded 타입만 인식 -->\n" +
                "            <form action=\"/formLogin\" method=\"post\">\n" +
                "                id:  <input type=\"text\" name=\"username\" />\n" +
                "                </br>\n" +
                "                pwd: <input type=\"password\" name=\"password\" />\n" +
                "                </br>\n" +
                "                <button>로그인</button>\n" +
                "            </form>\n" +
                "        <a href=\"/oauth2/authorization/kakao\" >\n" +
                "            <img src=\"resources/templates/kakao_login_medium_narrow.png\"\n" +
                "                 alt=\"kakao\">\n" +
                "        </a>\n" +
                "\n" +
                "    </body>\n" +
                "</html>";
    }



    @GetMapping("/admin")
    public ResponseEntity<?> admin(HttpServletRequest request){
        return new ResponseEntity<>("admin",HttpStatus.OK);
    }

    @GetMapping("/method")
    public ResponseEntity<?> method(){
        return new ResponseEntity<>("method secured",HttpStatus.OK);
    }

    @GetMapping("/parseAccount")
    public ResponseEntity<?> parseAccount() {

        String loginId = SecurityUtils.getLoginId();
        Collection<? extends GrantedAuthority> authorities = SecurityUtils.getAuthorities();
        HashMap<String,Object> map = new HashMap<>();
        map.put("로그인한 아이디" , loginId);
        map.put("사용자가 가진 권한" , authorities);

        return new ResponseEntity<>(map,HttpStatus.OK);
    }
}
