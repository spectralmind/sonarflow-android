package com.spectralmind.sf4android.definitions;

import android.graphics.PointF;

import com.google.common.base.Objects;

public class ClusterAttributeDefinition {
	private final PointF center;
	private final Integer color;

	public ClusterAttributeDefinition(PointF center, Integer color) {
		this.center = center;
		this.color = color;
	}

	public PointF getCenter() {
		return center;
	}
	
	public Integer getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this).toString();
	}
}
