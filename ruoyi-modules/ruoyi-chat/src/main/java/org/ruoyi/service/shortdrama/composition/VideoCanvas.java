package org.ruoyi.service.shortdrama.composition;

public record VideoCanvas(int width, int height, int fps) {

    public VideoCanvas {
        if (width <= 0 || height <= 0 || fps <= 0 || width % 2 != 0 || height % 2 != 0) {
            throw new IllegalArgumentException("Canvas dimensions must be positive even numbers and fps must be positive");
        }
    }
}
