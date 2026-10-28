package com.floop.phonebook.config;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);

    }

    @Test
    void generateToken_whenGenerated_thenReturnsSameName(){
        String token = jwtUtil.generateToken("ali");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("ali");
    }

    @Test
    void validateToken_whenTokenIsValid_returnsTrue(){
        String token = jwtUtil.generateToken("a");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_whenTokenExpired_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String token = jwtUtil.generateToken("a");


        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_whenTokenIsChanged_returnsFalse(){
        String token = jwtUtil.generateToken("a");

        assertThat(jwtUtil.validateToken(token+"x")).isFalse();
    }

    @Test
    void validateToken_whenSecretKeyIsDifferent_returnsFalse(){
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", "test-secret-ccc-must-bb-tt-aaaaa-67-bssss-long");
        ReflectionTestUtils.setField(util, "expiration", 60_000L);
        String token = util.generateToken("utilUser");

        assertThat(jwtUtil.validateToken(token)).isFalse();
        assertThat(util.validateToken(token)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc"})
    void validateToken_whenTokenIsInvalid_returnsFalse(String token) {
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void extractUsername_whenGetsInvalidToken_throwsJwtException(){
        String token = jwtUtil.generateToken("a");

        assertThatThrownBy(()->jwtUtil.extractUsername(token + 'x')).isInstanceOf(JwtException.class);
    }

}