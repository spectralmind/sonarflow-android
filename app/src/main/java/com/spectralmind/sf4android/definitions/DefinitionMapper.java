package com.spectralmind.sf4android.definitions;

import java.util.List;

public class DefinitionMapper {
	private List<ClusterDefinition> definitions;

	public DefinitionMapper(List<ClusterDefinition> definitions) {
		this.definitions = definitions;
	}

	public ClusterDefinitionWithPos findDefinitionForName(String candidate) {
		int i = 0;
		for (ClusterDefinition definition : definitions) {
			if(definition.containsName(candidate)) {
				return new ClusterDefinitionWithPos(definition, i); 
			}
			i++;
		}
		throw new IllegalStateException("Unmapped cluster name: " + candidate);
	}


}
