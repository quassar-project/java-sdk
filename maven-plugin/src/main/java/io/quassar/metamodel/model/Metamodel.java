package io.quassar.metamodel.model;

import java.util.List;
import java.util.Optional;

public record Metamodel(List<MetamodelType> types, Optional<MetamodelManifest> manifest) {
	public Metamodel {
		types = List.copyOf(types);
	}

	public Metamodel(List<MetamodelType> types) {
		this(types, Optional.empty());
	}

	public Optional<String> name() {
		return manifest.map(MetamodelManifest::name).filter(name -> !name.isBlank());
	}
}
