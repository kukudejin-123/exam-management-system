package com.exam.api.controller;

import com.exam.api.dto.ClassRequest;
import com.exam.common.exception.BizException;
import com.exam.common.page.PageResult;
import com.exam.repository.entity.SchoolClass;
import com.exam.security.jwt.AuthPrincipal;
import com.exam.service.clazz.ClassService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 班级接口的 Web 层测试。
 *
 * <p>用 @SpringBootTest + @AutoConfigureMockMvc 加载完整上下文——
 * 必须这样才能让 Spring Security 的过滤器链真正生效，
 * @PreAuthorize 与"未登录返回 401"才验证得到。
 * ClassService 打桩，不连数据库。
 */
@SpringBootTest(classes = com.exam.api.ApiTestApp.class)
@AutoConfigureMockMvc
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    /** 由 TestBeans 注入的 Mockito 桩 */
    @Autowired
    private ClassService classService;

    private static AuthPrincipal principal(long userId, String role, Long classId) {
        return new AuthPrincipal(userId, role, classId);
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(classService);
    }

    @Test
    @DisplayName("创建班级：参数合法时返回 200 与新 id")
    void create_ok() throws Exception {
        when(classService.create(anyString(), anyString(), any())).thenReturn(42L);

        mockMvc.perform(post("/api/classes")
                        .with(user(principal(2L, "TEACHER", null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ClassRequest("计科2401", "数据结构", 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(42));
    }

    @Test
    @DisplayName("创建班级：名称为空时返回 400 并指出具体字段")
    void create_rejects_blank_name() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .with(user(principal(2L, "TEACHER", null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ClassRequest("", "数据结构", 2L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value(containsString("班级名称")));
    }

    @Test
    @DisplayName("创建班级：科目为空时返回 400")
    void create_rejects_blank_subject() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .with(user(principal(2L, "TEACHER", null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ClassRequest("计科2401", null, 2L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("删除班级：学生角色被拒绝（403）")
    void delete_denied_for_student() throws Exception {
        mockMvc.perform(delete("/api/classes/1")
                        .with(user(principal(5L, "STUDENT", 7L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("删除班级：教师角色同样被拒绝，只有管理员能删")
    void delete_denied_for_teacher() throws Exception {
        mockMvc.perform(delete("/api/classes/1")
                        .with(user(principal(2L, "TEACHER", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("删除班级：管理员可以删")
    void delete_allowed_for_admin() throws Exception {
        mockMvc.perform(delete("/api/classes/1")
                        .with(user(principal(1L, "ADMIN", null))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("创建班级：学生角色被拒绝（403）")
    void create_denied_for_student() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .with(user(principal(5L, "STUDENT", 7L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ClassRequest("计科2401", "数据结构", 2L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("查询班级：未登录返回 401")
    void page_requires_auth() throws Exception {
        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("查询班级：登录后可访问，返回统一分页结构")
    void page_ok_when_authenticated() throws Exception {
        SchoolClass c = new SchoolClass();
        c.setId(1L);
        c.setName("计科2401");
        c.setSubject("数据结构");
        c.setArchived(0);
        when(classService.page(any(), any())).thenReturn(
                PageResult.of(List.of(c), 1L, 1L, 10L));

        mockMvc.perform(get("/api/classes")
                        .with(user(principal(5L, "STUDENT", 7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("计科2401"));
    }

    @Test
    @DisplayName("查询不存在的班级：返回 404 与业务错误码")
    void get_not_found() throws Exception {
        when(classService.getById(999L)).thenThrow(BizException.notFound("班级"));

        mockMvc.perform(get("/api/classes/999")
                        .with(user(principal(5L, "STUDENT", 7L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3001))
                .andExpect(jsonPath("$.message").value("班级不存在"));
    }
}
