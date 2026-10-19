package com.manage.nhakhoa.features.dashboard.service;

import com.manage.nhakhoa.features.dashboard.dto.response.AdminDashboardResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DTO Dashboard không được có trường PII/PHI và không được tham chiếu Entity JPA (BA V1 Q15). */
class DashboardDtoPrivacyTest {

    private static final List<String> FORBIDDEN = List.of(
            "hoten", "sodienthoai", "phone", "ngaysinh", "birth", "email", "diachi", "address",
            "chandoan", "diagnos", "trieuchung", "ketluan", "donthuoc", "prescription", "benhnhan", "patient",
            "matkhau", "password", "token", "cccd", "cmnd");

    @Test
    void dashboardDtoGraphHasNoPersonalOrClinicalFieldsAndNoEntities() {
        Set<Class<?>> visited = new HashSet<>();
        walk(AdminDashboardResponse.class, visited);
        assertTrue(visited.size() >= 10, "phải duyệt toàn bộ cây DTO");
        for (Class<?> type : visited) {
            assertFalse(type.getPackageName().contains(".entity"), "DTO tham chiếu entity: " + type);
            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    String name = component.getName().toLowerCase(Locale.ROOT);
                    for (String forbidden : FORBIDDEN) {
                        assertFalse(name.contains(forbidden), type.getSimpleName() + "." + component.getName());
                    }
                }
            }
        }
    }

    private static void walk(Type type, Set<Class<?>> visited) {
        if (type instanceof ParameterizedType parameterized) {
            walk(parameterized.getRawType(), visited);
            for (Type argument : parameterized.getActualTypeArguments()) {
                walk(argument, visited);
            }
            return;
        }
        if (!(type instanceof Class<?> clazz) || !clazz.getName().startsWith("com.manage.nhakhoa") || !visited.add(clazz)) {
            return;
        }
        if (clazz.isRecord()) {
            for (RecordComponent component : clazz.getRecordComponents()) {
                walk(component.getGenericType(), visited);
            }
        }
    }
}
