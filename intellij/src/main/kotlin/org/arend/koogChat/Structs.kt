package org.arend.koogChat

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

data class LLMSuggestion(
  val suggestedCode : List<String>
)

data class CorrectnessOfLLMSuggestion(
  val areSuggestionsCorrect : Map<String, Boolean>
)


fun checkArendCorrectness(suggestion : LLMSuggestion) : CorrectnessOfLLMSuggestion{

}
