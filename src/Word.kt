
class Word(
	val lemma: String,
	val definitions: List<Definition>,
	val relations: List<Relation>,
	val atolasEtymology: String?,
	val etymology: String?,
	val otherLanguages: String?,
	val tags: List<String>,
	val details: List<Detail>,
	val examples: List<Example>
)

class Definition(
	val tags: List<String>,
	val translations: List<String>,
	val description: String?
)

class Relation(
	val tags: List<String>,
	val words: List<String>
)

class Detail(
	val tag: String,
	val text: String
)

class Example(
	val tag: String,
	val texts: List<String>
)
