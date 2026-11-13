# Quassar Maven Plugin

Use the Quassar Maven plugin to build a compiler or an interpreter for your DSL. Both use the typed Java classes the plugin generates from your metamodel. 1. Set `scaffold` and the project's `packaging` to `maven-plugin`. Run `mvn generate-sources` to create an editable Mojo with a `generate` goal.
2. Put your `.itr` templates in `src/main/resources`. Implement the goal to load M1 through the generated model class, render the templates, and write the result to the consumer's generated sources directory.
3. Run `mvn install` in the DSL plugin project. In the consuming project, add your plugin's Maven coordinates and bind its `generate` goal to run before compilation.

### Interpretation

1. Set `scaffold` to `application`. Run `mvn generate-sources` to create an editable `Main.java`.
2. In `Main`, call the generated model class's `load` method with the M1 download URI.
3. Use the generated getters to read the model and implement its behavior in Java. Compile and run the application with the M1 you want to interpret.

Use `scaffold=none` if you want to supply your own entry point.

If M2 is configured and the templates directory has no `.itr` files, both scaffolds create `model-json.itr` as a starter example. The application starter prints the rendered result when one template is available. To generate Java from templates alone, omit `metamodel`.

The generated Maven goal reads `m1` and writes `target/generated-resources/model.json` when a metamodel and one template are available. Set `-Dquassar.source` and `-Dquassar.output` to use other paths.

## Configuration reference

| Parameter | Default |
| --- | --- |
| `package` | `generated` |
| `metamodel` | Optional M2 folder or ZIP URL |
| `scaffold` | `application` |
| `templatesDirectory` | `${project.basedir}/src/main/resources` |
| `outputDirectory` | `${project.basedir}/src/main/gen` |

For property types, cardinality, and the generated Java API, see the [M2 guide](metamodel.md).
