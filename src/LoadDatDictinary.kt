
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

fun loadDatDictionary(file: File): List<Word> {
	val words = mutableListOf<Word>()
	val reader = BufferedReader(InputStreamReader(FileInputStream(file), "UTF-8"))

	var prevWord: Word? = null
	var prevLemma: String? = null
	while (true) {
		val text = reader.readLine() ?: break

		val splitPos = text.indexOf(" ///  / ")
		if (splitPos < 0) {
			continue
		}
		val pdicWord = text.substring(0, splitPos)
		val pdicExp = text.substring(splitPos + 8).replace(" \\ ", "\n")

		val ism = pdicWord.endsWith("#")
		val lemma =	if (ism) pdicWord.dropLast(1) else pdicWord
		val word = parseWord(lemma, pdicExp, ism) ?: continue

		if (ism) {
			if (prevLemma == lemma) {
				words[words.size - 1] = integrateIsm(prevWord!!, word)
			} else {
				words.add(word)
			}
			prevWord = null
			prevLemma = null
		} else {
			words.add(word)
			prevWord = word
			prevLemma = lemma
		}
	}
	return words
}
