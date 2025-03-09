package com.example.demo.controller;

import com.example.demo.dto.ProductDTO;
import com.example.demo.service.ProductService;
import com.example.demo.dto.ApiResponseDTO;
import com.example.demo.swagger.ApiStandardResponse;
import com.example.demo.webSocket.WebSocketNotificationHandler;
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
    private final WebSocketNotificationHandler notificationHandler;


    @Operation(summary = "根据 ID 查询产品", description = "返回指定 ID 的产品信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Object>> getProductById (@PathVariable("id") Long id) {

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功找到产品", productService.getProductById(id)));
    }

    @Operation(summary = "查询所有产品", description = "返回数据库中所有产品的列表")
    @ApiStandardResponse
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ProductDTO>>> getAllProducts() {
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "返回产品", productService.getAllProducts()));
    }

    // Get 请求：获取用RequestParam过滤后的信息, http://localhost:8080/products?category=electronics&minPrice=100
    @Operation(summary = "根据 category 和 minPrice 查询产品", description = "返回指定产品列表")
    @ApiStandardResponse
    @GetMapping("/category-price")
    public ResponseEntity<ApiResponseDTO<List<ProductDTO>>> getProductsByCategoryAndPrice(
            @RequestParam String category, @RequestParam double minPrice) {
        List<ProductDTO> products = productService.getProductByCategoryAndMinPrice(category, minPrice);
        return ResponseEntity.ok(new ApiResponseDTO<>(200, "成功",products));
    }


    // POST 请求：新增产品
    @Operation(summary = "新增产品", description = "创建新产品")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "产品创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponseDTO<ProductDTO>> addProduct(@RequestBody @Validated ProductDTO productDTO) throws IOException {
        ProductDTO addedProduct = productService.addProduct(productDTO);

        log.debug("Product successfully created with ID: {}", addedProduct.getId());
        // **WebSocket 发送通知**
        notificationHandler.sendNotification("新增产品: " + addedProduct.getName());

        return ResponseEntity
                .status(201) // 201 Created 表示资源已成功创建
                .body(new ApiResponseDTO<>(201, "产品创建成功", addedProduct));
    }

    // PUT 请求：更新产品
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "产品更新成功"),
            @ApiResponse(responseCode = "400", description = "产品未找到"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @Operation(summary = "更新产品", description = "不返回")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Long>> updateProduct(
            @PathVariable("id") Long id, @RequestBody @Validated ProductDTO productDTO) throws IOException {

        Long updatedId = productService.updateProduct(id, productDTO);

        // **WebSocket 发送通知**
        notificationHandler.sendNotification("产品更新: " + productDTO.getName());

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "产品更新成功", updatedId));
    }

    // DELETE 请求：删除产品
    @Operation(summary = "删除产品", description = "删除指定 ID 的产品")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "产品删除成功"),
            @ApiResponse(responseCode = "404", description = "产品未找到"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Long>>deleteProduct(@PathVariable("id") Long id) throws IOException {
        productService.deleteProduct(id);
        // **WebSocket 发送通知**
        notificationHandler.sendNotification("产品删除: ID " + id);

        return ResponseEntity.ok(new ApiResponseDTO<>(200, "产品删除成功", id));
    }

}
