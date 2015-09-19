package com.spectralmind.sf4android.bubble.rendering;

import java.util.Map;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;

import com.google.common.collect.Maps;

public class ColorFilters {
	private final Mode filterMode;
	private final Map<Integer, ColorFilter> filtersByColor;

	public ColorFilters(Mode filterMode) {
		this.filterMode = filterMode;
		this.filtersByColor = Maps.newHashMap();
	}

	public ColorFilter getFilterForColor(Integer color) {
		ColorFilter result = filtersByColor.get(color);
		if(result == null) {
			result = new PorterDuffColorFilter(color.intValue(), filterMode);
			filtersByColor.put(color, result);
		}

		return result;
	}

}
