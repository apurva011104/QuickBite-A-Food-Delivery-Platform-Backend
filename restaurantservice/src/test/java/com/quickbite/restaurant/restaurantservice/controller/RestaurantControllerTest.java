package com.quickbite.restaurant.restaurantservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantApprovalRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.requestDto.RestaurantRequestDto;
import com.quickbite.restaurant.restaurantservice.dto.responseDto.RestaurantResponseDto;
import com.quickbite.restaurant.restaurantservice.service.RestaurantService;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private RestaurantController restaurantController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantController).build();
    }

    @Test
    void registerShouldReturnCreatedRestaurant() throws Exception {
        RestaurantRequestDto request = new RestaurantRequestDto(
                "Spice Hub",
                "Family dining",
                "Indian",
                "12 Lake Road",
                "Pune",
                18.5204,
                73.8567,
                "9999999999",
                5.0,
                150.0,
                30
        );
        RestaurantResponseDto response = new RestaurantResponseDto(10L, "Spice Hub", "Family dining",
                "Indian", "Pune", 0.0, false, false, 30);

        when(restaurantService.registerRestaurant(any(RestaurantRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/restaurants/owner/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(10))
                .andExpect(jsonPath("$.name").value("Spice Hub"))
                .andExpect(jsonPath("$.city").value("Pune"));
    }

    @Test
    void getByCityShouldReturnRestaurantList() throws Exception {
        RestaurantResponseDto response = new RestaurantResponseDto(11L, "Coastal Kitchen", "Seafood",
                "Seafood", "Chennai", 4.4, true, true, 25);

        when(restaurantService.getByCity("Chennai")).thenReturn(List.of(response));

        mockMvc.perform(get("/restaurants/public/city/Chennai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantId").value(11))
                .andExpect(jsonPath("$[0].approved").value(true));
    }

    @Test
    void rejectShouldForwardReasonToService() throws Exception {
        RestaurantApprovalRequestDto request = new RestaurantApprovalRequestDto();
        request.setReason("Incomplete license");
        RestaurantResponseDto response = new RestaurantResponseDto(15L, "Urban Bites", "Cafe",
                "Cafe", "Bengaluru", 0.0, false, false, 20);

        when(restaurantService.rejectRestaurant(eq(15L), eq("Incomplete license"))).thenReturn(response);

        mockMvc.perform(put("/restaurants/admin/reject/15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(15))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(restaurantService).rejectRestaurant(15L, "Incomplete license");
    }
}
