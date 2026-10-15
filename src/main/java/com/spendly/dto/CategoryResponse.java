package com.spendly.dto;

import com.spendly.entity.Category;
import com.spendly.entity.CategoryType;

public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;
    private String color;
    private String icon;
    private Long userId;
    private boolean isCustom;
    private boolean canDelete;

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String name, CategoryType type, String color, String icon, Long userId, boolean isCustom, boolean canDelete) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
        this.userId = userId;
        this.isCustom = isCustom;
        this.canDelete = canDelete;
    }

    public static CategoryResponse fromEntity(Category c, boolean canDelete) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getType(),
                c.getColor(),
                c.getIcon(),
                c.getUserId(),
                c.isCustom(),
                canDelete
        );
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
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}
