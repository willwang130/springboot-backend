package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "通用 API 响应格式")
public class ApiResponseDTO<T> {
    @Schema(description = "状态码", example = "200")
    private int code;

    @Schema(description = "返回信息", example = "成功")
    private String message;

    @Schema(description = "返回数据")
    private T data;
}
