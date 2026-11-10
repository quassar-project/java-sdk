package io.quassar.plugin;

import io.quassar.plugin.adapters.MavenDependencies;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public final class TranslatorLifecycle extends AbstractMavenLifecycleParticipant {
	@Override
	public void afterProjectsRead(MavenSession session) {
		for (MavenProject project : session.getProjects()) {
			Plugin plugin = project.getPlugin("quassar.io:maven-plugin");
			if (plugin != null && plugin.isExtensions())
				new MavenDependencies().addTo(project, plugin.getVersion());
		}
	}
}
