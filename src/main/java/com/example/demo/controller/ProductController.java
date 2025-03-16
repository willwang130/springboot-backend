package com.example.demo.controller;

import com.example.demo.dto.ProductDTO;
import com.example.demo.service.ProductService;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.swagger.ApiStandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController // Spring MVC 控制器, 返回是相应体内容（通常是JSON）
@RequestMapping("/api/products") //定义基础路径
@Tag(name = "1. 产品管理", description = "产品管理相关 API")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // GET 请求：通过 ID 获取
    @Operation(summary = "根据 ID 查询产品", description = "返回指定 ID 的产品信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回产品信息"),
            @ApiResponse(responseCode = "404", description = "产品不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ProductDTO>> getProductById (
            @PathVariable("id") Long id) {
        return productService.getProductById(id);
    }

    @Operation(summary = "查询所有产品", description = "返回所有产品的列表")
    @ApiStandardResponse
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ProductDTO>>> getAllProducts() {
        return productService.getAllProducts();
    }

    // Get 请求：获取用RequestParam过滤后的信息, http://localhost:8080/products?category=electronics&minPrice=100
    @Operation(summary = "根据 category 和 minPrice 查询产品", description = "返回指定产品列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功返回产品列表"),
            @ApiResponse(responseCode = "404", description = "没有符合条件的产品")
    })
    @GetMapping("/category-price")
    public ResponseEntity<ApiResponseDTO<List<ProductDTO>>> getProductsByCategoryAndPrice(
            @RequestParam String category, @RequestParam double minPrice) {
        return  productService.getProductByCategoryAndMinPrice(category, minPrice);
    }


    // POST 请求：新增产品
    @Operation(summary = "新增产品", description = "创建新产品")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "产品创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDTO<ProductDTO>> addProduct(
            @RequestBody @Validated ProductDTO productDTO) throws IOException {
         productService.addProduct(productDTO);
        return productService.addProduct(productDTO);
    }

    // PUT 请求：更新产品
    @Operation(summary = "更新产品", description = "更新指定 ID 的产品信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "产品更新成功"),
            @ApiResponse(responseCode = "400", description = "产品未找到"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Long>> updateProduct(
            @PathVariable("id") Long id, @RequestBody @Validated ProductDTO productDTO) throws IOException {
        return productService.updateProduct(id, productDTO);
    }

    // DELETE 请求：删除产品
    @Operation(summary = "删除产品", description = "删除指定 ID 的产品")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "产品删除成功"),
            @ApiResponse(responseCode = "404", description = "产品未找到"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Long>>deleteProduct(
            @PathVariable("id") Long id) throws IOException {
        return productService.deleteProduct(id);
    }

}
