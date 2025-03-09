package com.example.demo.controller;

import com.example.demo.dto.ProductDTO;
import com.example.demo.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;

import static org.mockito.ArgumentMatchers.anyLong;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Disabled
@ExtendWith(MockitoExtension.class)
@WebMvcTest(ProductController.class)     // 只加载 web 层, 不加载 Service 和 Repository
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;        // Mock Api 请求

    @MockitoBean
    private ProductService productService;// Mock Service 层

    @Autowired
    private ObjectMapper objectMapper;      // JSON 解析工具

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetAllProducts() throws Exception {
        List<ProductDTO> mockProducts = Arrays.asList(
                new ProductDTO(1L ,"Product1", 100.0),
                new ProductDTO(2L,"Product2", 300.0)
        );

        // Mock Service 返回数据
        when(productService.getAllProducts()).thenReturn(mockProducts);

        // 执行 GET 请求 /products
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].name").value("Product2"))
                .andExpect(jsonPath("$[1].price").value("300.0"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetProductById() throws Exception {
        ProductDTO mockProduct = new ProductDTO(1L,"Laptop", 5000.0);

        // Mock Service 层
        when(productService.getProductById(anyLong())).thenReturn(mockProduct);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value("5000.0"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAddProduct() throws Exception {
        ProductDTO mockProduct = new ProductDTO(1L,"Banana", 20.0);

        mockMvc.perform(post("/products")
                        .with(jwt())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(mockProduct)))
                .andExpect(status().isOk());

    }
}
