package com.sensible.common.util;

import org.jcodec.common.model.Picture;

import java.awt.image.BufferedImage;

public class PictureToBufferedImageConverter {
    public static BufferedImage toBufferedImage(Picture src) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] srcData = src.getPlaneData(0); // YUV 데이터를 가져옴

        // YUV 데이터를 RGB로 변환
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int r = srcData[index]; // 간단히 Y 값만 가져오는 방식
                image.getRaster().setSample(x, y, 0, r); // Red
                image.getRaster().setSample(x, y, 1, r); // Green
                image.getRaster().setSample(x, y, 2, r); // Blue
            }
        }
        return image;
    }
}
