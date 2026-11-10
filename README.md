# Quassar Java SDK

Build your own domain-specific language (DSL) in [Quassar](https://quassar.io), then use the Java SDK to build applications and code generators for models written in that language.

## From a DSL to Java

1. **Define your language in Forge.** Choose **New DSL** and define the concepts, properties, and relationships in its metamodel (M2).
2. **Create a model in Studio.** Choose **New model**, select your DSL, and add the objects you want to describe. This is your M1 model.
3. **Connect both to your Java project.** Configure the Maven plugin with the M2 download URL to generate a typed API. Use the M1 download URL in Java to load a model through that API.

## Explore the SDK

### [Maven Plugin](maven-plugin/README.md)

Turn your DSL's metamodel into a typed Java API, compile `.itr` templates, and scaffold an application or a Maven code generator.

### [Template Engine](template-engine/README.md)

Use `.itr` templates to turn models written in your DSL into source code, JSON, or other text formats.

### [Model Reader](model-reader/README.md)

Load and query models written in your DSL directly from Java, with access to mograms, children, and references.

---

Java 17+ · [MIT License](LICENSE)
