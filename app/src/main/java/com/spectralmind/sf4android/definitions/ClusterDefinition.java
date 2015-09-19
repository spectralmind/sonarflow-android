package com.spectralmind.sf4android.definitions;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class ClusterDefinition {
	private final String name;
	private final List<String> lowercaseSubExpressions;

	public ClusterDefinition(String name, List<String> subExpressions) {
		this.name = name;
		this.lowercaseSubExpressions = toLower(subExpressions);
	}

	private List<String> toLower(List<String> subExpressions) {
		if (subExpressions == null) return null;
		List<String> results = Lists.newArrayList();
		for(String sub : subExpressions) {
			results.add(sub.toLowerCase());
		}
		return results;
	}

	public boolean containsName(String candidate) {
		if (name.equalsIgnoreCase(candidate)) 
			return true;
		else {
			if (lowercaseSubExpressions != null) {
				for(String sub : lowercaseSubExpressions) {
					//				if (sub.isEmpty()) {
					//					return false;
					//				}

					// why is an empty subexpression list always a match??
					//				if(isMatchAll(sub)) {
					//					return true;
					//				}

					if(candidate.toLowerCase().contains(sub)) {
						return true;
					}
				}
			} 
			return false;
		}
	}

	public String getName() {
		return name;
	}


	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("name", name).add("subexpressions", lowercaseSubExpressions).toString();
	}
}
