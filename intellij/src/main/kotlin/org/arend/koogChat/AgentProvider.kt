package org.arend.koogChat

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgent.Companion.invoke
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.xml.xml
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.io.File

class AgentProvider(val project : Project) {
    val apiKey = System.getenv("OPENAI_API")
    val executor = simpleOpenAIExecutor(apiKey)
    val targetModel = OpenAIModels.Chat.GPT4o

  fun createProofPlannerAgent(promptExecutor: PromptExecutor): AIAgent<String, List<String>> {
    val agentConfig = AIAgentConfig(
      prompt = prompt(
        "proofPlannerAgent", params = LLMParams(temperature = 0.2)
      ) {
        system(
          """
                You are an expert in mathematics and proof formalization. 
                You know Arend proof assistant and have worked with it.
                """.trimIndent()
        )
      }, model = OpenAIModels.Chat.GPT4o, maxAgentIterations = 50
    )

    val proofPlannerStrategy = proofPlannerStrategy()

    return AIAgent<String, List<String>>(
      promptExecutor = promptExecutor,
      strategy = proofPlannerStrategy,
      agentConfig = agentConfig,
    ) {
      handleEvents {
        onToolCallStarting { eventContext ->
          println("Tool called: ${eventContext.toolName} with args ${eventContext.toolArgs}")
        }

        onAgentCompleted { eventContext ->
          println("Agent finished with result: ${eventContext.result}")
        }
      }
  }
  }

  //probably later it should become strategy<String, List<Definition>)
  fun proofPlannerStrategy() = strategy<String, List<String>>("proofPlanner") {

    //llm call is inside
    val naturalLanguageFormulation by subgraphWithTask<String, List<String>>() { initialMessage ->
      xml {
        tag("instructions") {
          +File("/Users/artem.semidetnov/Documents/testLLMArendCompletion/src/main/kotlin/systemPrompt.md").readText()
            .trimIndent()
        }
        tag("initial_user_message") {
          +initialMessage
        }
      }
    }

    val arendFormulation by node<List<String>, List<String>>() {
      lemmas ->
      lemmas.map {
        lemma ->
        val translationPrompt = prompt("translate-item") {
          system("""You are a professional math formalizer in the language Arend. " +
                        "Translate the following lemma into Arend. Make it into a \func or a \lemma with correct types.
                        Instead of body of the function or a lemma write the unsolved goal symbol {?}""")
          user(lemma)
        }
        val response = executor.execute(
          prompt = translationPrompt,
          model = targetModel
        )
        response.last().content.trim()
      }
    }

    val writeToArendFile by node<List<String>, List<String>>(){
      lemmas ->
      val path = FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile?.path
      path?.let{
      lemmas.forEach { lemma ->
        val arendFile = File(path)
        arendFile.appendText("\n\n")
        arendFile.appendText(lemma)
      }}
      lemmas
    }

    nodeStart then naturalLanguageFormulation then arendFormulation then writeToArendFile then nodeFinish
  }
  }