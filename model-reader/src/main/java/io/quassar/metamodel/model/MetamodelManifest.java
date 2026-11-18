package io.quassar.metamodel.model;

import java.util.List;

public record MetamodelManifest(String id, String name, String version, String commit, List<String> views, String owner) {
	public MetamodelManifest {
		views = views == null ? List.of() : List.copyOf(views);
	}
}
