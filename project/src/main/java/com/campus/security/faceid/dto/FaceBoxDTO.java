package com.campus.security.faceid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaceBoxDTO {
    private int x;
    private int y;
    private int width;
    private int height;
    private float confidence;
}