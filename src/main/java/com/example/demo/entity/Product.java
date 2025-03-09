package com.example.demo.entity;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Entity // JPA 实体(Entity）类（class）表示这个类对应数据库里的一个表
@Table(name = "product") //指定数据库表名 默认product
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id // 数据库主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 自增
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double price;

//    //无参数构造函数 必须！
//    public Product () {}
//    // 构造函数 (Constructor)
//    public Product(String name, Double price) {
//        this.name = name;
//        this.price = price;
//    }
//    // getter, setter
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public String getName() { return name; }
//    public void setName(String name) { this.name = name; }
//
//    public Double getPrice() { return price; }
//    public void setPrice(double price) { this.price = price; }
//
//    @Override
//    public String toString(){
//        return "Product{id=" + id + ", name='" + name + "', price=" + price + "}";
//    }
}
