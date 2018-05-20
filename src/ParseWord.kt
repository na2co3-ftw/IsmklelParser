
private val lemmaExcludeRegex = Regex(".,\\S|[^ -~]|^@")
private val homonymPostfixRegex = Regex("\\(\\d+\\)$")
private val postDescRegex = Regex("[。:→←].*$")
private val preDescRegex = Regex("^（[a-z].*?）")
private val etymologyRegex = Regex("^[-a-z0-9制古赤初中先定高恣＠]")
private val etymologyPriorRegex = Regex("^(?:[制古赤初中先定高]|[-a-z0-9恣＠].*?[制古赤初中先定高])")

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
			}
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

	while (row < contents.size) {
		val line = contents[row]
		if (line.startsWith('［') || line.startsWith('【')) {
			break
		}
		if (line.contains(etymologyRegex)) {
			if (row + 1 < contents.size) {
				if (line.contains(';') && contents[row + 1].contains(etymologyRegex) ||
						contents[row + 1].contains(etymologyPriorRegex)
				) {
					if (atolasEtymology == null) {
						atolasEtymology = line
					} else {
						println("$lemma: duplicate atolasEtymology")
						atolasEtymology += "\n" + line
					}

					row++
					etymology = contents[row]
					row++
					break
				}
			}
			etymology = line
			row++
			break
		} else {
			if (atolasEtymology == null) {
				atolasEtymology = line
			} else {
				println("$lemma: duplicate atolasEtymology")
				atolasEtymology += "\n" + line
			}
		}
		row++
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

	var detailsSection = true
	while (row < contents.size) {
		val tagLine = contents[row]
		if (tagLine.startsWith('［')) {
			if (!detailsSection && !ism) {
				println("$lemma: ［］ after 【】")
			}
			val tagEndPos = tagLine.indexOf('］', 1)
			if (tagEndPos < 0) {
				println("$lemma: missing ］")
			} else if (tagLine.length > tagEndPos + 1) {
				println("$lemma: trailing text")
			}
			val tag = if (tagEndPos >= 0) tagLine.substring(1, tagEndPos) else ""
			row++

			val detailText = StringBuilder()
			while (row < contents.size) {
				if (contents[row].startsWith('［') || contents[row].startsWith('【')) {
					break
				}
				detailText.append(contents[row]).append("\n")
				row++
			}
			details.add(Detail(tag, detailText.dropLast(1).toString()))
		} else if (tagLine.startsWith('【')) {
			detailsSection = false;
			val tagEndPos = tagLine.indexOf('】', 1)
			if (tagEndPos < 0) {
				println("$lemma: missing 】")
			} else if (tagLine.length > tagEndPos + 1) {
				println("$lemma: trailing text")
			}
			val tag = if (tagEndPos >= 0) tagLine.substring(1, tagEndPos) else ""
			row++

			if (tag == "画像") {
				continue
			}
			val texts = mutableListOf<String>()
			while (row < contents.size) {
				if (contents[row].startsWith('［') || contents[row].startsWith('【')) {
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
			examples
	)
}
