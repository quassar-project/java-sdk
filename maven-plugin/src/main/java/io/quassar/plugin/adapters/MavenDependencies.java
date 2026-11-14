package io.quassar.plugin.adapters;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MavenDependencies {
	private static final List<String> ARTIFACTS = List.of("model-reader", "template-engine");
	private static final String CONTEXT = MavenDependencies.class.getName();

	public void addTo(MavenProject project, String version) {
		Model additions = project.getContextValue(CONTEXT) instanceof Model model ? model : new Model();
		for (DependencySpec spec : requiredDependencies(project, version)) {
			if (hasDependency(project, spec.group(), spec.artifact())) continue;
			Dependency dependency = dependency(project, spec);
			project.getDependencies().add(dependency);
			additions.addDependency(dependency.clone());
		}
		project.setContextValue(CONTEXT, additions);
	}

	public boolean isConfigured(MavenProject project) {
		return ARTIFACTS.stream().allMatch(artifact -> hasDependency(project, "quassar.io", artifact));
	}

	private List<DependencySpec> requiredDependencies(MavenProject project, String version) {
		List<DependencySpec> dependencies = new ArrayList<>();
		for (String artifact : ARTIFACTS) dependencies.add(new DependencySpec("quassar.io", artifact, version, "compile"));
		if ("maven-plugin".equals(project.getPackaging())) {
			dependencies.add(new DependencySpec("org.apache.maven", "maven-plugin-api", "3.9.9", "provided"));
			dependencies.add(new DependencySpec("org.apache.maven", "maven-core", "3.9.9", "provided"));
			dependencies.add(new DependencySpec("org.apache.maven.plugin-tools", "maven-plugin-annotations", "3.15.1", "provided"));
		}
		return dependencies;
	}

	private Dependency dependency(MavenProject project, DependencySpec spec) {
		Dependency dependency = managed(project, spec);
		dependency.setGroupId(spec.group());
		dependency.setArtifactId(spec.artifact());
		if (dependency.getVersion() == null) dependency.setVersion(spec.version());
		if (dependency.getScope() == null) dependency.setScope(spec.scope());
		return dependency;
	}

	private boolean hasDependency(MavenProject project, String group, String artifact) {
		return project.getDependencies().stream().anyMatch(dependency -> matches(dependency, group, artifact));
	}

	public void writePom(MavenProject project) throws IOException {
		if (!(project.getContextValue(CONTEXT) instanceof Model additions) || additions.getDependencies().isEmpty()) return;
		Model pom = project.getOriginalModel().clone();
		for (Dependency dependency : additions.getDependencies()) pom.addDependency(dependency.clone());
		Path file = Path.of(project.getBuild().getDirectory(), "metta", "consumer-pom.xml");
		Files.createDirectories(file.getParent());
		try (Writer writer = Files.newBufferedWriter(file)) {
			new MavenXpp3Writer().write(writer, pom);
		}
		project.setPomFile(file.toFile());
	}

	private Dependency managed(MavenProject project, DependencySpec spec) {
		if (project.getDependencyManagement() == null) return new Dependency();
		return project.getDependencyManagement().getDependencies().stream()
			.filter(dependency -> matches(dependency, spec.group(), spec.artifact()))
			.findFirst().map(Dependency::clone).orElseGet(Dependency::new);
	}

	private boolean matches(Dependency dependency, String group, String artifact) {
		return group.equals(dependency.getGroupId()) && artifact.equals(dependency.getArtifactId())
			&& "jar".equals(dependency.getType()) && dependency.getClassifier() == null;
	}

	private record DependencySpec(String group, String artifact, String version, String scope) {
	}
}
