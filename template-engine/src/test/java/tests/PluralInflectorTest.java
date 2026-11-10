package tests;

import io.quassar.templating.domain.formatters.inflectors.EnglishPluralInflector;
import io.quassar.templating.domain.formatters.inflectors.SpanishPluralInflector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluralInflectorTest {

	private EnglishPluralInflector englishPluralInflector;
	private SpanishPluralInflector spanishPluralInflector;

	@BeforeEach
	public void setUp() {
		englishPluralInflector = new EnglishPluralInflector();
		spanishPluralInflector = new SpanishPluralInflector();
	}

	@Test
	public void nouns() {
		assertThat(englishPlural("car")).isEqualTo("cars");
		assertThat(englishPlural("house")).isEqualTo("houses");
		assertThat(englishPlural("book")).isEqualTo("books");
		assertThat(englishPlural("bird")).isEqualTo("birds");
		assertThat(englishPlural("pencil")).isEqualTo("pencils");

	}

	@Test
	public void endingInSS() {
		assertThat(englishPlural("kiss")).isEqualTo("kisses");
		assertThat(englishPlural("boss")).isEqualTo("bosses");
		assertThat(englishPlural("address")).isEqualTo("addresses");
		assertThat(englishPlural("class")).isEqualTo("classes");
		assertThat(englishPlural("dress")).isEqualTo("dresses");

	}

	@Test
	public void endingInSH() {
		assertThat(englishPlural("whish")).isEqualTo("whishes");
		assertThat(englishPlural("fish")).isEqualTo("fishes");
		assertThat(englishPlural("brush")).isEqualTo("brushes");
		assertThat(englishPlural("flash")).isEqualTo("flashes");
	}

	@Test
	public void endingInCH() {
		assertThat(englishPlural("match")).isEqualTo("matches");
		assertThat(englishPlural("beach")).isEqualTo("beaches");
		assertThat(englishPlural("clutch")).isEqualTo("clutches");
		assertThat(englishPlural("peach")).isEqualTo("peaches");
		assertThat(englishPlural("inch")).isEqualTo("inches");
		assertThat(englishPlural("branch")).isEqualTo("branches");
		assertThat(englishPlural("speech")).isEqualTo("speeches");
	}

	@Test
	public void endingInX() {
		assertThat(englishPlural("fox")).isEqualTo("foxes");
		assertThat(englishPlural("box")).isEqualTo("boxes");
		assertThat(englishPlural("suffix")).isEqualTo("suffixes");
		assertThat(englishPlural("prefix")).isEqualTo("prefixes");
	}

	@Test
	public void endingInZZ() {
		assertThat(englishPlural("buzz")).isEqualTo("buzzes");
	}

	@Test
	public void endingInVowelY() {
		assertThat(englishPlural("boy")).isEqualTo("boys");
		assertThat(englishPlural("holiday")).isEqualTo("holidays");
		assertThat(englishPlural("key")).isEqualTo("keys");
		assertThat(englishPlural("guy")).isEqualTo("guys");
	}

	@Test
	public void endingInConsonantY() {
		assertThat(englishPlural("party")).isEqualTo("parties");
		assertThat(englishPlural("lady")).isEqualTo("ladies");
		assertThat(englishPlural("story")).isEqualTo("stories");
		assertThat(englishPlural("nanny")).isEqualTo("nannies");
		assertThat(englishPlural("city")).isEqualTo("cities");
	}

	@Test
	public void endingInFE() {
		assertThat(englishPlural("life")).isEqualTo("lives");
		assertThat(englishPlural("wife")).isEqualTo("wives");
	}

	@Test
	public void endingInF() {
		assertThat(englishPlural("leaf")).isEqualTo("leaves");
		assertThat(englishPlural("thief")).isEqualTo("thieves");
	}

	@Test
	public void endingInIs() {
		assertThat(englishPlural("analysis")).isEqualTo("analyses");
		assertThat(englishPlural("basis")).isEqualTo("bases");
		assertThat(englishPlural("crisis")).isEqualTo("crises");
	}

	@Test
	public void endingInO() {
		assertThat(englishPlural("potato")).isEqualTo("potatoes");
		assertThat(englishPlural("echo")).isEqualTo("echoes");
		assertThat(englishPlural("tomato")).isEqualTo("tomatoes");
		assertThat(englishPlural("hero")).isEqualTo("heroes");
		assertThat(englishPlural("torpedo")).isEqualTo("torpedoes");
		assertThat(englishPlural("mosquito")).isEqualTo("mosquitos");
	}

	@Test
	public void irregular() {
		assertThat(englishPlural("man")).isEqualTo("men");
		assertThat(englishPlural("woman")).isEqualTo("women");
		assertThat(englishPlural("child")).isEqualTo("children");
		assertThat(englishPlural("foot")).isEqualTo("feet");
		assertThat(englishPlural("tooth")).isEqualTo("teeth");
		assertThat(englishPlural("goose")).isEqualTo("geese");
		assertThat(englishPlural("mouse")).isEqualTo("mice");
		assertThat(englishPlural("sheep")).isEqualTo("sheep");
		assertThat(englishPlural("deer")).isEqualTo("deer");
		assertThat(englishPlural("moose")).isEqualTo("moose");
		assertThat(englishPlural("aircraft")).isEqualTo("aircraft");
	}

	@Test
	public void testReward() {
		assertThat(englishPlural("ant")).isEqualTo("ants");
		assertThat(englishPlural("lock")).isEqualTo("locks");
		assertThat(englishPlural("banana")).isEqualTo("bananas");
		assertThat(englishPlural("magazine")).isEqualTo("magazines");
		assertThat(englishPlural("bicycle")).isEqualTo("bicycles");
		assertThat(englishPlural("mongoose")).isEqualTo("mongooses");
		assertThat(englishPlural("cow")).isEqualTo("cows");
		assertThat(englishPlural("nose")).isEqualTo("noses");
		assertThat(englishPlural("daughter")).isEqualTo("daughters");
		assertThat(englishPlural("orange")).isEqualTo("oranges");
		assertThat(englishPlural("egg")).isEqualTo("eggs");
		assertThat(englishPlural("picture")).isEqualTo("pictures");
		assertThat(englishPlural("flower")).isEqualTo("flowers");
		assertThat(englishPlural("rose")).isEqualTo("roses");
		assertThat(englishPlural("grape")).isEqualTo("grapes");
		assertThat(englishPlural("spoonful")).isEqualTo("spoonfuls");
		assertThat(englishPlural("human")).isEqualTo("humans");
		assertThat(englishPlural("toy")).isEqualTo("toys");
		assertThat(englishPlural("jug")).isEqualTo("jugs");
		assertThat(englishPlural("vase")).isEqualTo("vases");
		assertThat(englishPlural("king")).isEqualTo("kings");
		assertThat(englishPlural("well")).isEqualTo("wells");
		assertThat(englishPlural("address")).isEqualTo("addresses");
		assertThat(englishPlural("fish")).isEqualTo("fishes");
		assertThat(englishPlural("batch")).isEqualTo("batches");
		assertThat(englishPlural("inch")).isEqualTo("inches");
		assertThat(englishPlural("beach")).isEqualTo("beaches");
		assertThat(englishPlural("itch")).isEqualTo("itches");
		assertThat(englishPlural("boss")).isEqualTo("bosses");
		assertThat(englishPlural("kiss")).isEqualTo("kisses");
		assertThat(englishPlural("box")).isEqualTo("boxes");
		assertThat(englishPlural("lens")).isEqualTo("lenses");
		assertThat(englishPlural("branch")).isEqualTo("branches");
		assertThat(englishPlural("lunch")).isEqualTo("lunches");
		assertThat(englishPlural("brush")).isEqualTo("brushes");
		assertThat(englishPlural("match")).isEqualTo("matches");
		assertThat(englishPlural("bus")).isEqualTo("buses");
		assertThat(englishPlural("minus")).isEqualTo("minuses");
		assertThat(englishPlural("buzz")).isEqualTo("buzzes");
		assertThat(englishPlural("patch")).isEqualTo("patches");
		assertThat(englishPlural("catch")).isEqualTo("catches");
		assertThat(englishPlural("peach")).isEqualTo("peaches");
		assertThat(englishPlural("church")).isEqualTo("churches");
		assertThat(englishPlural("plus")).isEqualTo("pluses");
		assertThat(englishPlural("class")).isEqualTo("classes");
		assertThat(englishPlural("prefix")).isEqualTo("prefixes");
		assertThat(englishPlural("clutch")).isEqualTo("clutches");
		assertThat(englishPlural("prospectus")).isEqualTo("prospectuses");
		assertThat(englishPlural("coach")).isEqualTo("coaches");
		assertThat(englishPlural("quiz")).isEqualTo("quizzes");
		assertThat(englishPlural("complex")).isEqualTo("complexes");
		assertThat(englishPlural("six")).isEqualTo("sixes");
		assertThat(englishPlural("cross")).isEqualTo("crosses");
		assertThat(englishPlural("slash")).isEqualTo("slashes");
		assertThat(englishPlural("dish")).isEqualTo("dishes");
		assertThat(englishPlural("speech")).isEqualTo("speeches");
		assertThat(englishPlural("dress")).isEqualTo("dresses");
		assertThat(englishPlural("suffix")).isEqualTo("suffixes");
		assertThat(englishPlural("eyelash")).isEqualTo("eyelashes");
		assertThat(englishPlural("tax")).isEqualTo("taxes");
		assertThat(englishPlural("flash")).isEqualTo("flashes");
		assertThat(englishPlural("thrush")).isEqualTo("thrushes");
		assertThat(englishPlural("fox")).isEqualTo("foxes");
		assertThat(englishPlural("virus")).isEqualTo("viruses");
		assertThat(englishPlural("gas")).isEqualTo("gases");
		assertThat(englishPlural("watch")).isEqualTo("watches");
		assertThat(englishPlural("glass")).isEqualTo("glasses");
		assertThat(englishPlural("wish")).isEqualTo("wishes");
		assertThat(englishPlural("army")).isEqualTo("armies");
		assertThat(englishPlural("enemy")).isEqualTo("enemies");
		assertThat(englishPlural("baby")).isEqualTo("babies");
		assertThat(englishPlural("fairy")).isEqualTo("fairies");
		assertThat(englishPlural("beauty")).isEqualTo("beauties");
		assertThat(englishPlural("family")).isEqualTo("families");
		assertThat(englishPlural("berry")).isEqualTo("berries");
		assertThat(englishPlural("lady")).isEqualTo("ladies");
		assertThat(englishPlural("cherry")).isEqualTo("cherries");
		assertThat(englishPlural("library")).isEqualTo("libraries");
		assertThat(englishPlural("city")).isEqualTo("cities");
		assertThat(englishPlural("nappy")).isEqualTo("nappies");
		assertThat(englishPlural("country")).isEqualTo("countries");
		assertThat(englishPlural("sky")).isEqualTo("skies");
		assertThat(englishPlural("cry")).isEqualTo("cries");
		assertThat(englishPlural("spy")).isEqualTo("spies");
		assertThat(englishPlural("curry")).isEqualTo("curries");
		assertThat(englishPlural("story")).isEqualTo("stories");
		assertThat(englishPlural("diary")).isEqualTo("diaries");
		assertThat(englishPlural("strawberry")).isEqualTo("strawberries");
		assertThat(englishPlural("discovery")).isEqualTo("discoveries");
		assertThat(englishPlural("territory")).isEqualTo("territories");
		assertThat(englishPlural("duty")).isEqualTo("duties");
		assertThat(englishPlural("theory")).isEqualTo("theories");
		assertThat(englishPlural("alloy")).isEqualTo("alloys");
		assertThat(englishPlural("joy")).isEqualTo("joys");
		assertThat(englishPlural("bay")).isEqualTo("bays");
		assertThat(englishPlural("key")).isEqualTo("keys");
		assertThat(englishPlural("boy")).isEqualTo("boys");
		assertThat(englishPlural("kidney")).isEqualTo("kidneys");
		assertThat(englishPlural("chimney")).isEqualTo("chimneys");
		assertThat(englishPlural("monkey")).isEqualTo("monkeys");
		assertThat(englishPlural("day")).isEqualTo("days");
		assertThat(englishPlural("play")).isEqualTo("plays");
		assertThat(englishPlural("decoy")).isEqualTo("decoys");
		assertThat(englishPlural("ray")).isEqualTo("rays");
		assertThat(englishPlural("delay")).isEqualTo("delays");
		assertThat(englishPlural("toy")).isEqualTo("toys");
		assertThat(englishPlural("donkey")).isEqualTo("donkeys");
		assertThat(englishPlural("tray")).isEqualTo("trays");
		assertThat(englishPlural("guy")).isEqualTo("guys");
		assertThat(englishPlural("trolley")).isEqualTo("trolleys");
		assertThat(englishPlural("jersey")).isEqualTo("jerseys");
		assertThat(englishPlural("valley")).isEqualTo("valleys");
		assertThat(englishPlural("journey")).isEqualTo("journeys");
		assertThat(englishPlural("way")).isEqualTo("ways");
		assertThat(englishPlural("beef")).isEqualTo("beeves");
		assertThat(englishPlural("loaf")).isEqualTo("loaves");
		assertThat(englishPlural("calf")).isEqualTo("calves");
		assertThat(englishPlural("self")).isEqualTo("selves");
		assertThat(englishPlural("half")).isEqualTo("halves");
		assertThat(englishPlural("sheaf")).isEqualTo("sheaves");
		assertThat(englishPlural("housewife")).isEqualTo("housewives");
		assertThat(englishPlural("shelf")).isEqualTo("shelves");
		assertThat(englishPlural("knife")).isEqualTo("knives");
		assertThat(englishPlural("thief")).isEqualTo("thieves");
		assertThat(englishPlural("leaf")).isEqualTo("leaves");
		assertThat(englishPlural("wife")).isEqualTo("wives");
		assertThat(englishPlural("life")).isEqualTo("lives");
		assertThat(englishPlural("wolf")).isEqualTo("wolves");
		assertThat(englishPlural("bamboo")).isEqualTo("bamboos");
		assertThat(englishPlural("ratio")).isEqualTo("ratios");
		assertThat(englishPlural("casino")).isEqualTo("casinos");
		assertThat(englishPlural("scenario")).isEqualTo("scenarios");
		assertThat(englishPlural("commando")).isEqualTo("commandos");
		assertThat(englishPlural("silo")).isEqualTo("silos");
		assertThat(englishPlural("disco")).isEqualTo("discos");
		assertThat(englishPlural("shampoo")).isEqualTo("shampoos");
		assertThat(englishPlural("duo")).isEqualTo("duos");
		assertThat(englishPlural("solo")).isEqualTo("solos");
		assertThat(englishPlural("dynamo")).isEqualTo("dynamos");
		assertThat(englishPlural("stereo")).isEqualTo("stereos");
		assertThat(englishPlural("ghetto")).isEqualTo("ghettos");
		assertThat(englishPlural("studio")).isEqualTo("studios");
		assertThat(englishPlural("kangaroo")).isEqualTo("kangaroos");
		assertThat(englishPlural("tattoo")).isEqualTo("tattoos");
		assertThat(englishPlural("kilo")).isEqualTo("kilos");
		assertThat(englishPlural("trio")).isEqualTo("trios");
		assertThat(englishPlural("photo")).isEqualTo("photos");
		assertThat(englishPlural("video")).isEqualTo("videos");
		assertThat(englishPlural("piano")).isEqualTo("pianos");
		assertThat(englishPlural("zero")).isEqualTo("zeros");
		assertThat(englishPlural("radio")).isEqualTo("radios");
		assertThat(englishPlural("zoo")).isEqualTo("zoos");
		assertThat(englishPlural("buffalo")).isEqualTo("buffaloes");
		assertThat(englishPlural("potato")).isEqualTo("potatoes");
		assertThat(englishPlural("echo")).isEqualTo("echoes");
		assertThat(englishPlural("tomato")).isEqualTo("tomatoes");
		assertThat(englishPlural("hero")).isEqualTo("heroes");
		assertThat(englishPlural("torpedo")).isEqualTo("torpedoes");
	}

	@Test
	public void endingInConsonant() {
		assertThat(spanishPlural("Arbol")).isEqualTo("Arboles");
		assertThat(spanishPlural("Matriz")).isEqualTo("Matrices");
		assertThat(spanishPlural("Climax")).isEqualTo("Climax");
		assertThat(spanishPlural("Clan")).isEqualTo("Clanes");
		assertThat(spanishPlural("Gafas")).isEqualTo("Gafas");
		assertThat(spanishPlural("sesos")).isEqualTo("sesos");
		assertThat(spanishPlural("sesion")).isEqualTo("sesiones");
	}

	@Test
	public void endingInVowel() {
		assertThat(spanishPlural("copa")).isEqualTo("copas");
		assertThat(spanishPlural("cafe")).isEqualTo("cafes");
		assertThat(spanishPlural("taxi")).isEqualTo("taxis");
		assertThat(spanishPlural("colibri")).isEqualTo("colibris");
		assertThat(spanishPlural("amigo")).isEqualTo("amigos");
		assertThat(spanishPlural("plato")).isEqualTo("platos");
	}

	private String spanishPlural(String word) {
		return spanishPluralInflector.plural(word);
	}

	private String englishPlural(String word) {
		return englishPluralInflector.plural(word);
	}
}
