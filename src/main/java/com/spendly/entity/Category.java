package com.spendly.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type = CategoryType.EXPENSE;

    @Column(length = 20)
    private String color;

    @Column(length = 50)
    private String icon;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "is_custom")
    private Boolean isCustom = false;

    public Category() {
    }

    public Category(String name, CategoryType type, String color, String icon) {
        this.name = name;
        this.type = type != null ? type : CategoryType.EXPENSE;
        this.color = color;
        this.icon = icon;
        this.userId = null;
        this.isCustom = false;
    }

    public Category(String name, CategoryType type, String color, String icon, Long userId, Boolean isCustom) {
        this.name = name;
        this.type = type != null ? type : CategoryType.EXPENSE;
        this.color = color;
        this.icon = icon;
        this.userId = userId;
        this.isCustom = isCustom != null ? isCustom : false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isCustom() {
        return isCustom != null && isCustom;
    }

    public Boolean getIsCustom() {
        return isCustom != null ? isCustom : false;
    }

    public void setCustom(boolean custom) {
        this.isCustom = custom;
    }

    public void setIsCustom(Boolean isCustom) {
        this.isCustom = isCustom != null ? isCustom : false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
