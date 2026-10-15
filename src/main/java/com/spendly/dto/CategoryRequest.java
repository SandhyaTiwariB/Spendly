package com.spendly.dto;

import com.spendly.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name is required.")
    @Size(max = 50, message = "Category name cannot exceed 50 characters.")
    private String name;

    private CategoryType type = CategoryType.EXPENSE;

    private String color;

    private String icon;

    public CategoryRequest() {
    }

    public CategoryRequest(String name, CategoryType type, String color, String icon) {
        this.name = name;
        this.type = type != null ? type : CategoryType.EXPENSE;
        this.color = color;
        this.icon = icon;
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
}
