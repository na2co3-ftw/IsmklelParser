
private val lemmaExcludeRegex = Regex(".,\\S|[^ -~]|^@")
private val homonymPostfixRegex = Regex("\\(\\d+\\)$")
private val postDescRegex = Regex("[。:→←].*$")
private val preDescRegex = Regex("^（[a-z].*?）")
private val atolasEtymologyRegex = Regex(";|^(?!(?:a?lakta|kako|sorn|ryuu|seren))[a-z]+(?::(?!a?lakta)[a-z]+)?$")
private val detailTagRegex = Regex("^［(文化|語法|擬声|注釈)］(?:［(文化|語法)］)?")

fun parseWord(lemma: String, exp: String, ism: Boolean): Word? {
	if (lemma.contains(lemmaExcludeRegex)) {
		return null
	}
	val contents = exp.split('\n').dropLastWhile { it == "" }
	var row = 0

	val definitions = mutableListOf<Definition>()
	val relations = mutableListOf<Relation>()
	var atolasEtymology: String? = null
	var etymology: String? = null
	var otherLanguages: String? = null
	val tags = mutableListOf<String>()
	val details = mutableListOf<Detail>()
	val examples = mutableListOf<Example>()
	var image: String? = null

	while (row < contents.size) {
		val line = contents[row]
		if (!line.startsWith('［') && (row != 0 || ism)) {
			break
		}

		val defTags = mutableListOf<String>()
		var pos = 0
		while (pos < line.length && line[pos] == '［') {
			val tagEndPos = line.indexOf('］', pos + 1)
			if (tagEndPos < 0) {
				println("$lemma: missing ］")
				break
			}
			defTags.add(line.substring(pos + 1, tagEndPos))
			pos = tagEndPos + 1
		}
		var lineContents = line.substring(pos)

		if (defTags.any { it == "類義語" || it == "反意語" || it == "類音" }) {
			val relation = Relation(defTags, lineContents.split('、'))
			relations.add(relation)
		} else if (defTags.contains("レベル")) {
			if (lineContents.isNotEmpty() && '１' <= lineContents[0] && lineContents[0] <= '６') {
				tags.add("レベル" + (lineContents[0] - '０'))
			} else {
				println("$lemma: invalid level")
			}
		} else if (defTags.any { it == "文化" || it == "語法" }) {
			break
		} else {
			var desc: String? = null
			val postDesc = postDescRegex.find(lineContents)
			if (postDesc != null) {
				if (postDesc.value.startsWith('。')) {
					if (postDesc.value.length > 1) {
						desc = postDesc.value.substring(1)
					}
				} else {
					desc = postDesc.value
				}
				lineContents = lineContents.substring(0, postDesc.range.start)
			}
			val preDesc = preDescRegex.find(lineContents)
			if (preDesc != null) {
				desc = preDesc.value + (desc ?: "")
				lineContents = lineContents.substring(preDesc.range.endInclusive)
			}
			definitions.add(Definition(defTags, lineContents.split('、'), desc))
		}
		row++
	}

	if (row < contents.size) {
		val line = contents[row]
		if (!line.startsWith('［') && !line.startsWith('【')) {
			if (line.contains(atolasEtymologyRegex)) {
				atolasEtymology = line
				row++
			}
		}
	}

	if (row < contents.size) {
		val line = contents[row]
		if (!line.startsWith('［') && !line.startsWith('【')) {
			etymology = line
			row++
		}
	}

	while (row < contents.size) {
		val line = contents[row]
		if (line.startsWith('［') || line.startsWith('【')) {
			break
		}
		if (otherLanguages == null) {
			otherLanguages = line
		} else {
			println("$lemma: duplicate otherLanguages")
			otherLanguages += "\n" + line
		}
		row++
	}

	while (row < contents.size) {
		val tagLine = contents[row]
		val detailTagsMatch = detailTagRegex.find(tagLine)
		if (detailTagsMatch != null) {
			val detailTags = if (detailTagsMatch.groups[2] != null) {
				listOf(detailTagsMatch.groups[1]!!.value, detailTagsMatch.groups[2]!!.value)
			} else {
				listOf(detailTagsMatch.groups[1]!!.value)
			}
			if (tagLine.length > detailTagsMatch.range.endInclusive + 1) {
				println("$lemma: trailing text")
			}
			row++

			val detailText = StringBuilder()
			while (row < contents.size) {
				if (contents[row].contains(detailTagRegex) || contents[row].startsWith('【')) {
					break
				}
				detailText.append(contents[row]).append("\n")
				row++
			}
			details.add(Detail(detailTags, detailText.dropLast(1).toString()))
			continue
		}

		if (tagLine.startsWith('【')) {
			val tagEndPos = tagLine.indexOf('】', 1)
			if (tagEndPos < 0) {
				println("$lemma: missing 】")
			} else if (tagLine.length > tagEndPos + 1) {
				println("$lemma: trailing text")
			}
			val tag = if (tagEndPos >= 0) tagLine.substring(1, tagEndPos) else ""
			row++

			if (tag == "画像") {
				image = "$lemma.jpg"
				continue
			}
			val texts = mutableListOf<String>()
			while (row < contents.size) {
				if (contents[row].contains(detailTagRegex) || contents[row].startsWith('【')) {
					break
				}
				texts.add(contents[row])
				row++
			}
			examples.add(Example(tag, texts))
		} else {
			println("$lemma: unexpected text")
			break
		}
	}

	return Word(
			lemma.replace(homonymPostfixRegex, ""),
			definitions,
			relations,
			atolasEtymology,
			etymology,
			otherLanguages,
			tags,
			details,
			examples,
			image
	)
}
