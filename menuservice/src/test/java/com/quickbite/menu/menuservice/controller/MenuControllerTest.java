package com.quickbite.menu.menuservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.menu.menuservice.dto.responseDto.CategoryResponseDto;
import com.quickbite.menu.menuservice.dto.responseDto.MenuItemResponseDto;
import com.quickbite.menu.menuservice.exception.GlobalExceptionHandler;
import com.quickbite.menu.menuservice.exception.ResourceNotFoundException;
import com.quickbite.menu.menuservice.security.JwtAuthenticationFilter;
import com.quickbite.menu.menuservice.service.MenuService;

@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MenuService menuService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void addCategoryShouldReturnOk() throws Exception {
        CategoryResponseDto response = new CategoryResponseDto(10L, 1L, "Main Course", "Meals", "img.png", 1, List.of());
        when(menuService.addCategory(any())).thenReturn(response);

        mockMvc.perform(post("/menu/category")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryPayload(1L, "Main Course", "Meals", "img.png", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(10))
                .andExpect(jsonPath("$.name").value("Main Course"));
    }

    @Test
    void addCategoryShouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/menu/category")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing restaurant\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMenuItemShouldReturnNotFoundWhenServiceThrows() throws Exception {
        when(menuService.getMenuItemById(99L)).thenThrow(new ResourceNotFoundException("Item not found"));

        mockMvc.perform(get("/menu/item/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item not found"));
    }

    @Test
    void updateMenuItemShouldReturnUpdatedDto() throws Exception {
        MenuItemResponseDto response = new MenuItemResponseDto(20L, 1L, 10L, "Paneer Wrap", "Fresh wrap",
                BigDecimal.valueOf(299), BigDecimal.valueOf(249), "wrap.png", true, 320, true, 4.5, List.of("veg"));
        when(menuService.updateMenuItem(any(), any())).thenReturn(response);

        mockMvc.perform(put("/menu/item/20")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuItemPayload(
                                1L, 10L, "Paneer Wrap", "Fresh wrap", BigDecimal.valueOf(299),
                                BigDecimal.valueOf(249), "wrap.png", true, 320, List.of("veg")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(20))
                .andExpect(jsonPath("$.categoryId").value(10));
    }

    @Test
    void searchShouldReturnItems() throws Exception {
        when(menuService.searchMenuItems("wrap")).thenReturn(List.of(
                new MenuItemResponseDto(20L, 1L, 10L, "Paneer Wrap", "Fresh wrap",
                        BigDecimal.valueOf(299), BigDecimal.valueOf(249), "wrap.png", true, 320, true, 4.5, List.of("veg"))));

        mockMvc.perform(get("/menu/search").param("query", "wrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Paneer Wrap"));
    }

    @Test
    void genericExceptionShouldReturnInternalServerError() throws Exception {
        doThrow(new RuntimeException("boom")).when(menuService).getItemsByVeg(true);

        mockMvc.perform(get("/menu/veg"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    private record CategoryPayload(Long restaurantId, String name, String description, String imageUrl, int displayOrder) { }

    private record MenuItemPayload(Long restaurantId, Long categoryId, String name, String description,
                                   BigDecimal price, BigDecimal discountedPrice, String imageUrl,
                                   boolean veg, double calories, List<String> tags) { }
}
