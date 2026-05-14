package com.quickbite.menu.menuservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.menu.menuservice.config.SecurityConfig;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.exception.GlobalExceptionHandler;
import com.quickbite.menu.menuservice.security.JwtAuthenticationFilter;
import com.quickbite.menu.menuservice.service.MenuService;

@WebMvcTest(MenuController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MenuControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MenuService menuService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void allowJwtFilterToContinueChain() throws Exception {
        doAnswer(invocation -> {
            invocation.<FilterChain>getArgument(2).doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousUserShouldAccessGetEndpoint() throws Exception {
        when(menuService.getItemsByVeg(true)).thenReturn(List.of());

        mockMvc.perform(get("/menu/veg"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserShouldBeRejectedForPostEndpoint() throws Exception {
        mockMvc.perform(post("/menu/category")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryPayload(1L, "Main Course", "Meals", "img.png", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerRoleShouldAccessPostEndpoint() throws Exception {
        when(menuService.addCategory(any())).thenReturn(new CategoryResponseDto(10L, 1L, "Main Course", "Meals", "img.png", 1, List.of()));

        mockMvc.perform(post("/menu/category")
                        .with(user("owner").roles("OWNER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryPayload(1L, "Main Course", "Meals", "img.png", 1))))
                .andExpect(status().isOk());
    }

    private record CategoryPayload(Long restaurantId, String name, String description, String imageUrl, int displayOrder) { }
}
