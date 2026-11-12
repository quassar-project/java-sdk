package io.quassar.reader.domain;

import io.quassar.metamodel.domain.MetamodelReader;
import io.quassar.metamodel.model.MetamodelType;
import io.quassar.metamodel.model.PropertyDefinition;
import io.quassar.metamodel.model.PropertyType;
import io.quassar.reader.Models;
import io.quassar.reader.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class EffectiveValueResolver {
    private final Map<String, MetamodelType> types;
    private final Map<String, MetamodelType> parents;
    private final Map<String, Map<String, PropertyDefinition>> definitions;

    public EffectiveValueResolver(String metamodelText) throws IOException {
        Map<String, MetamodelType> parsed = new LinkedHashMap<>();
        try (var input = new ByteArrayInputStream(metamodelText.getBytes(StandardCharsets.UTF_8))) {
            for (var type : new MetamodelReader().read(input).types())
                if (parsed.putIfAbsent(type.name(), type) != null)
                    throw new IllegalArgumentException("Duplicate metamodel type: " + type.name());
        }
        types = Map.copyOf(parsed);
        Map<String, MetamodelType> parentTypes = new LinkedHashMap<>();
        for (var type : parsed.values()) linkParent(type, parentTypes, new LinkedHashSet<>());
        parents = Map.copyOf(parentTypes);
        Map<String, Map<String, PropertyDefinition>> resolved = new LinkedHashMap<>();
        for (var type : parsed.values()) inherit(type, resolved, new LinkedHashSet<>());
        definitions = Map.copyOf(resolved);
    }

    public Model resolve(Model model) throws IOException {
        List<Mogram> roots = new ArrayList<>();
        for (var root : model.mograms()) roots.add(resolve(root, find(root.type(), "")));
        return new Model(roots);
    }

    private Mogram resolve(Mogram mogram, MetamodelType type) throws IOException {
        List<MetamodelType> facets = new ArrayList<>();
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>(definitions.get(type.name()));
        for (String facet : mogram.facets()) {
            MetamodelType facetType = find(facet, type.name());
            if (!facetType.isFacet()) throw new IllegalArgumentException("Not a facet: " + facet);
            facets.add(facetType);
            definitions.get(facetType.name()).forEach(properties::putIfAbsent);
        }
        Map<String, Property> values = new LinkedHashMap<>();
        for (Property property : mogram.properties()) {
            if (property.origin() != Property.Origin.EXPLICIT) continue;
            var definition = properties.get(property.name());
            values.put(property.name(), new Property(property.name(), shape(property.value(), definition), property.origin()));
        }
        for (var property : properties.values()) {
            if (values.containsKey(property.name()) || !property.hasDefault()) continue;
            Value value = Models.create("Value holder\n\tvalue = " + property.defaultValue() + "\n").get("holder").property("value");
            values.put(property.name(), new Property(property.name(), shape(value, property), Property.Origin.DEFAULT));
        }
        List<Mogram> children = new ArrayList<>();
        for (var child : mogram.children()) children.add(resolve(child, childType(child.type(), type, facets)));
        return new Mogram(mogram.type(), mogram.identifier(), List.copyOf(values.values()), mogram.facets(), children);
    }

    private static Value shape(Value value, PropertyDefinition definition) {
        if (definition == null) return value;
        if (definition.declaredType().matches("word(?:\\[.*|:.*)?")) value = word(value);
        if (definition.type().kind() != PropertyType.Kind.SEQUENCE) return value;
        if (value instanceof Value.Sequence) return value;
        return new Value.Sequence(value.isEmpty() ? List.of() : List.of(value));
    }

    private static Value word(Value value) {
        if (value instanceof Value.Sequence sequence) return new Value.Sequence(sequence.values().stream().map(EffectiveValueResolver::word).toList());
        if (value instanceof Value.Number number) return new Value.Reference(number.value().toPlainString());
        if (value instanceof Value.Bool bool) return new Value.Reference(Boolean.toString(bool.value()));
        return value;
    }

    private Map<String, PropertyDefinition> inherit(MetamodelType type, Map<String, Map<String, PropertyDefinition>> resolved, Set<String> visiting) {
        if (resolved.containsKey(type.name())) return resolved.get(type.name());
        if (!visiting.add(type.name())) throw new IllegalArgumentException("Inheritance cycle: " + String.join(" -> ", visiting) + " -> " + type.name());
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
        for (var property : type.properties()) properties.put(property.name(), property);
        if (!type.parent().isBlank()) {
            MetamodelType parent = parents.get(type.name());
            if (parent == null) throw new IllegalArgumentException("Unknown parent: " + type.parent());
            inherit(parent, resolved, visiting).forEach(properties::putIfAbsent);
        }
        visiting.remove(type.name());
        var result = Collections.unmodifiableMap(properties);
        resolved.put(type.name(), result);
        return result;
    }

    private MetamodelType childType(String name, MetamodelType owner, List<MetamodelType> facets) {
        MetamodelType own = scoped(name, owner.name());
        if (own != null) return own;
        Set<MetamodelType> matches = new LinkedHashSet<>();
        for (var facet : facets) {
            MetamodelType member = scoped(name, facet.name());
            if (member != null) matches.add(member);
        }
        if (matches.size() > 1) throw new IllegalArgumentException("Ambiguous component '" + name + "' in applied facets: " + matches.stream().map(MetamodelType::name).toList());
        if (!matches.isEmpty()) return matches.iterator().next();
        return find(name, owner.name());
    }

    private MetamodelType find(String name, String scope) {
        MetamodelType scoped = scoped(name, scope);
        if (scoped != null) return scoped;
        var exact = types.get(name);
        if (exact != null) return exact;
        throw new IllegalArgumentException("Unknown metamodel type '" + name + "' in scope '" + scope + "'");
    }

    private MetamodelType scoped(String name, String scope) {
        for (String current = scope; !current.isEmpty();) {
            MetamodelType member = member(name, types.get(current), new HashSet<>());
            if (member != null) return member;
            int dot = current.lastIndexOf('.');
            current = dot < 0 ? "" : current.substring(0, dot);
        }
        return null;
    }

    private MetamodelType member(String name, MetamodelType owner, Set<String> visited) {
        for (var current = owner; current != null && visited.add(current.name()); current = parents.get(current.name())) {
            var direct = types.get(current.name() + "." + name);
            if (direct != null) return direct;
            int dot = name.indexOf('.');
            if (dot > 0) {
                var container = member(name.substring(0, dot), current, new HashSet<>());
                if (container != null) {
                    var nested = member(name.substring(dot + 1), container, new HashSet<>());
                    if (nested != null) return nested;
                }
            }
        }
        return null;
    }

    private MetamodelType linkParent(MetamodelType type, Map<String, MetamodelType> linked, Set<String> resolving) {
        if (type == null || type.parent().isBlank()) return null;
        if (linked.containsKey(type.name())) return linked.get(type.name());
        if (!resolving.add(type.name())) throw new IllegalArgumentException("Inheritance cycle while resolving parent: " + resolving);
        try {
            MetamodelType parent = type.declaration() == MetamodelType.Declaration.SUB ? types.get(type.parent()) :
                    parentInScope(type.parent(), type.scope(), linked, resolving);
            if (parent == null) throw new IllegalArgumentException("Unknown parent '" + type.parent() + "' of '" + type.name() + "'");
            linked.put(type.name(), parent);
            return parent;
        } finally {
            resolving.remove(type.name());
        }
    }

    private MetamodelType parentInScope(String name, String scope, Map<String, MetamodelType> linked, Set<String> resolving) {
        for (String current = scope; !current.isEmpty();) {
            Set<String> visited = new HashSet<>();
            for (MetamodelType owner = types.get(current); owner != null && visited.add(owner.name()); owner = linkParent(owner, linked, resolving)) {
                var candidate = types.get(owner.name() + "." + name);
                if (candidate != null) return candidate;
            }
            int dot = current.lastIndexOf('.');
            current = dot < 0 ? "" : current.substring(0, dot);
        }
        return types.get(name);
    }
}
