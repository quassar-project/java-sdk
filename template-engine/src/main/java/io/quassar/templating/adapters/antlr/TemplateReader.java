package io.quassar.templating.adapters.antlr;

import io.quassar.templating.ports.TemplateReadException;
import io.quassar.templating.ports.TemplateLoader;

import io.quassar.templating.adapters.antlr.generated.ItrLexer;
import io.quassar.templating.adapters.antlr.generated.ItrParser;
import io.quassar.templating.model.Template;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.antlr.v4.runtime.CharStreams.fromString;

public final class TemplateReader implements TemplateLoader {
	private static final System.Logger LOGGER = System.getLogger(TemplateReader.class.getName());

	private final InputStream input;

	public TemplateReader(InputStream input) {
		this.input = input;
	}

	public static Template read(Path path) throws TemplateReadException {
		try (InputStream input = Files.newInputStream(path)) {
			return new TemplateReader(input).read();
		} catch (IOException exception) {
			throw new TemplateReadException("Could not read template " + path, exception);
		}
	}

	@Override
	public Template read() throws TemplateReadException {
		return read(java.nio.charset.StandardCharsets.UTF_8);
	}


	public Template read(Charset charset) throws TemplateReadException {
		try {
			return read(fromString(IOUtils.toString(input, charset).trim()));
		} catch (IOException e) {
			throw new TemplateReadException("Could not read template", e);
		}
	}

	private Template read(CodePointCharStream stream) throws TemplateReadException {
		ItrLexer lexer = new ItrLexer(stream);
		lexer.reset();
		lexer.setState(1);
		ItrParser parser = new ItrParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new TemplateErrorStrategy());
		ItrParser.RootContext root = read(parser);
		ParseTreeWalker walker = new ParseTreeWalker();
		TemplateModelBuilder model = new TemplateModelBuilder();
		walker.walk(model, root);
		return model.template();
	}

	private ItrParser.RootContext read(ItrParser parser) throws TemplateReadException {
		try {
			return parser.root();
		} catch (RecognitionException e) {
			Token token = parser.getCurrentToken();
			LOGGER.log(System.Logger.Level.WARNING, "Rules not well formed. Error in: " + token.getLine() + ": " + token.getCharPositionInLine());
			throw new TemplateReadException("Template not well formed. Line:" + token.getLine() + "; Column: " + token.getCharPositionInLine() + ": " + e.getMessage());
		}
	}
}
