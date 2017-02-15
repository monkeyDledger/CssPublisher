package com.unionpay.model;

import java.io.Serializable;

/**
 * 图片bean
 * @author lichen2
 */
public class PhotoUpImageItem implements Serializable {

    private static final long serialVersionUID = -3920133685864272265L;
    
    // 图片ID
    private String imageId;
    // 图片名称
    private String imageName;
    // 原图路径
    private String imagePath;
    // 是否被选择
    private boolean isSelected = false;

    public String getImageId() {
	return imageId;
    }

    public void setImageId(String imageId) {
	this.imageId = imageId;
    }
    
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImagePath() {
	return imagePath;
    }

    public void setImagePath(String imagePath) {
	this.imagePath = imagePath;
    }

    public boolean isSelected() {
	return isSelected;
    }

    public void setSelected(boolean isSelected) {
	this.isSelected = isSelected;
    }

}
