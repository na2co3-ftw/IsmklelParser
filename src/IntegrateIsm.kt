
private val ismPriorEtymologyRegex = Regex("^＠?(?:1[0-9]?|2[0-3]?|[03-9])(?!\\d)")

fun integrateIsm(axn: Word, ism: Word): Word {
	val lozDefs = mutableListOf<Definition>()
	var ismDefs = ism.definitions
	for (axnDef in axn.definitions) {
		lozDefs.add(axnDef)
		val (defs, rest) = ismDefs.partition {
			it.tags == axnDef.tags
		}
		lozDefs.addAll(defs)
		ismDefs = rest
	}
	lozDefs.addAll(ismDefs)

	val lozRels = axn.relations.toMutableList()
	lozRels.addAll(ism.relations)
	lozRels.sortBy(fun (rel: Relation): Int {
		for (tag in rel.tags) {
			when (tag) {
				"類義語" -> return 0
				"反意語" -> return 1
				"類音" -> return 2
			}
		}
		return 3
	})

	val lozAtolasEtym = if (ism.atolasEtymology == null) {
		axn.atolasEtymology
	} else {
		if (axn.atolasEtymology == null) {
			ism.atolasEtymology
		} else {
			println("${ism.lemma}: duplicate atolasEtymology")
			axn.atolasEtymology + "\n" + ism.atolasEtymology
		}
	}

	val lozEtym = if (ism.etymology == null) {
		axn.etymology
	} else {
		if (axn.etymology == null) {
			ism.etymology
		} else {
			if (ism.etymology.contains(ismPriorEtymologyRegex)) {
				ism.etymology
			} else {
				axn.etymology
			}
		}
	}

	val lozOtherLang = if (ism.otherLanguages == null) {
		axn.otherLanguages
	} else {
		if (axn.otherLanguages == null) {
			ism.otherLanguages
		} else {
			println("${ism.lemma}: duplicate otherLanguages")
			axn.otherLanguages + "\n" + ism.otherLanguages
		}
	}

	val lozDets = mutableListOf<Detail>()
	var ismDets = ism.details
	for (axnDetail in axn.details) {
		val (dets, rest) = ismDets.partition {
			it.tag == axnDetail.tag
		}
		if (dets.isNotEmpty()) {
			val text = StringBuilder(axnDetail.text).append('\n')
			dets.joinTo(text, "\n", transform = { it.text })
			lozDets.add(Detail(axnDetail.tag, text.toString()))
		} else {
			lozDets.add(axnDetail)
		}
		ismDets = rest
	}
	lozDets.addAll(ismDets)

	val lozExps = mutableListOf<Example>()
	var ismExps = ism.examples
	for (axnExp in axn.examples) {
		val (exps, rest) = ismExps.partition {
			it.tag == axnExp.tag
		}
		if (exps.isNotEmpty()) {
			val texts = axnExp.texts.toMutableList()
			exps.flatMapTo(texts, { it.texts })
			lozExps.add(Example(axnExp.tag, texts))
		} else {
			lozExps.add(axnExp)
		}
		ismExps = rest
	}
	lozExps.addAll(ismExps)

	return Word(
		axn.lemma,
		lozDefs,
		lozRels,
		lozAtolasEtym,
		lozEtym,
		lozOtherLang,
		axn.tags,
		lozDets,
		lozExps
	)
}
