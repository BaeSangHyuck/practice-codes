package com.tutorial.jwtsecurity.service;

import com.tutorial.jwtsecurity.controller.dto.MemberRequestDto;
import com.tutorial.jwtsecurity.controller.dto.MemberResponseDto;
import com.tutorial.jwtsecurity.controller.dto.TokenRequestDto;
import com.tutorial.jwtsecurity.controller.dto.TokenDto;
import com.tutorial.jwtsecurity.entity.Member;
import com.tutorial.jwtsecurity.jwt.TokenProvider;
import com.tutorial.jwtsecurity.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthenticationManagerBuilder authenticationManagerBuilder;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenProvider tokenProvider;

  @Transactional
  public MemberResponseDto signup(MemberRequestDto memberRequestDto) {
    if (memberRepository.existsByEmail(memberRequestDto.getEmail())) {
      throw new RuntimeException("이미 가입되어 있는 유저입니다");
    }

    Member member = memberRequestDto.toMember(passwordEncoder);
    return MemberResponseDto.of(memberRepository.save(member));
  }

  @Transactional
  public HttpHeaders login(MemberRequestDto memberRequestDto) {
    // 1. Login ID/PW 를 기반으로 AuthenticationToken 생성
    UsernamePasswordAuthenticationToken authenticationToken = memberRequestDto.toAuthentication();

    // 2. 실제로 검증 (사용자 비밀번호 체크) 이 이루어지는 부분
    //    authenticate 메서드가 실행이 될 때 CustomUserDetailsService 에서 만들었던 loadUserByUsername 메서드가 실행됨
    Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

    // 3. 인증 정보를 기반으로 JWT 토큰 생성
    TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

    // 4. 토큰 발급
    return setHeader(tokenDto);
  }

  @Transactional
  public HttpHeaders reissue(TokenRequestDto tokenRequestDto) {
    // 1. Refresh Token 검증
    if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
      throw new RuntimeException("Refresh Token 이 유효하지 않습니다.");
    }

    // 2. Access Token 에서 Member ID 가져오기
    Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

    // 3. 저장소에서 Member ID 를 기반으로 Refresh Token 값 가져옴
    String refreshToken = tokenProvider.getRefreshToken(authentication.getName());

    // 4. Refresh Token 일치하는지 검사
    if (!refreshToken.equals(tokenRequestDto.getRefreshToken())) {
      throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
    }

    // 5. 새로운 토큰 생성
    TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

    // 6. 저장소 정보 업데이트
    tokenProvider.updateRefreshToken(authentication.getName(), tokenDto.getRefreshToken());

    // 토큰 발급
    return setHeader(tokenDto);
  }

  public HttpHeaders setHeader(TokenDto tokenDto) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("accessToken", tokenDto.getAccessToken());
    headers.add("refreshToken", tokenDto.getRefreshToken());
    return headers;
  }
}
