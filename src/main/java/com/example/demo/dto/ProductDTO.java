package com.example.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

// Data Transfer Object 数据封装 为了美观
@Data // 自动生成 Getter、Setter、toString、equals、hashCode
@AllArgsConstructor @NoArgsConstructor
public class ProductDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "产品名称不能为空")
    private String name;

    @Min(value = 0, message = "价格必须大于等于 0")
    private Double price;



}
