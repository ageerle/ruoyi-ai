package org.ruoyi.fusion.enums;


import lombok.Getter;

@Getter
public enum BlendDimensions {

	PORTRAIT("2:3"),

	SQUARE("1:1"),

	LANDSCAPE("3:2");

	private final String value;

	BlendDimensions(String value) {
		this.value = value;
	}

}
