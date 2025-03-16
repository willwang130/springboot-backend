package com.example.demo;

import com.example.demo.controller.ProductController;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.dto.ProductDTO;
import com.example.demo.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled
@Import(TestConfig.class)
@ExtendWith(MockitoExtension.class)
@WebMvcTest(ProductController.class)     // 只加载 web 层, 不加载 Service 和 Repository
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;        // Mock Api 请求

    @Autowired
    private ObjectMapper objectMapper;      // JSON 解析工具 MVC提供

    @MockitoBean
    private ProductService productService;// Mock Service 层


    @Test
    public void testGetAllProducts() throws Exception {
        List<ProductDTO> mockProducts = Arrays.asList(
                new ProductDTO(1L ,"Product1", 100.0),
                new ProductDTO(2L,"Product2", 300.0)
        );

        // Mock Service 返回数据
        when(productService.getAllProducts())
                .thenReturn(ResponseEntity.ok(new ApiResponseDTO<>(200, "", mockProducts)));
        // 执行 GET 请求 /products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].name").value("Product2"))
                .andExpect(jsonPath("$.data[1].price").value(300.0));
    }

    @Test
    public void testGetProductById() throws Exception {
        ProductDTO mockProduct = new ProductDTO(1L,"Laptop", 5000.0);

        // Mock Service 层
        when(productService.getProductById(anyLong()))
                .thenReturn(ResponseEntity.ok(new ApiResponseDTO<>(200, "", mockProduct)));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Laptop"))
                .andExpect(jsonPath("$.data.price").value(5000.0));
    }

    @Test
    public void testAddProduct() throws Exception {
        ProductDTO mockProduct = new ProductDTO(1L,"Banana", 20.0);

        // Mock Service 层
        when(productService.addProduct(any(ProductDTO.class))).
                thenReturn(ResponseEntity.ok(new ApiResponseDTO<>(200, "", mockProduct)));

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mockProduct)))
                .andExpect(status().isOk());

    }
}
