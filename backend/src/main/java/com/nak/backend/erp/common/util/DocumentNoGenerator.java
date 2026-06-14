package com.nak.backend.erp.common.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ERP 문서/전표 번호 자동 채번 유틸리티
 * Thread-safe하게 순차 번호를 생성합니다.
 */
public class DocumentNoGenerator {

    private static final AtomicInteger employeeNoCounter = new AtomicInteger(1);

    /**
     * 사번 생성 (EMP-0001 형식)
     * @param prefix 번호 접두어 (기본값 "EMP")
     * @return 생성된 사번 문자열
     */
    public static String generateEmployeeNo(String prefix) {
        String p = (prefix != null && !prefix.isEmpty()) ? prefix : "EMP";
        return p + "-" + String.format("%04d", employeeNoCounter.getAndIncrement());
    }

    /**
     * 사번 생성 (기본 접두어 "EMP" 사용)
     */
    public static String generateEmployeeNo() {
        return generateEmployeeNo("EMP");
    }

    /**
     * 현재 카운터 값을 반환합니다. (테스트/관리 목적)
     */
    public static int getCurrentCounter() {
        return employeeNoCounter.get();
    }

    /**
     * 카운터를 재설정합니다. (관리 목적)
     */
    public static void resetCounter(int value) {
        employeeNoCounter.set(value);
    }
}
