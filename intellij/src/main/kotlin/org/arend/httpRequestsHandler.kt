package org.arend

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.arend.ext.module.ModuleLocation
import org.arend.ext.module.ModuleLocation.LocationKind
import org.arend.ext.module.ModulePath
import org.arend.typechecking.runner.RunnerService
import org.jetbrains.ide.RestService

class MyRemoteTriggerHandler() : RestService() {
  val delimiter = "%%"

  override fun getServiceName(): String {
    return "detachedTypechecker"
  }

  override fun isSupported(request: FullHttpRequest): Boolean {
    return isMethodSupported(request.method()) && request.uri().startsWith("/api/${getServiceName()}")
  }

  override fun execute(
    urlDecoder: QueryStringDecoder,
    request: FullHttpRequest,
    context: ChannelHandlerContext
  ): String? {
    val encodedPayload = urlDecoder.parameters()["action"]?.firstOrNull() ?: ""
    val parsedUserRequest : DecodedRequestData = parseServerData(encodedPayload)
    val modules : List<ModuleLocation> = parsedUserRequest.modulePaths.map{
      ModuleLocation(parsedUserRequest.libraryName, LocationKind.SOURCE, ModulePath.fromString(it.split("/").last()))
    }
    for (modulePath in modules){
      ApplicationManager.getApplication().invokeLater {
        modulePath.let {
          val module = ModuleLocation(parsedUserRequest.libraryName, LocationKind.SOURCE, ModulePath.fromString(modulePath.toString()))
//          TODO : check, if this works with new libraryName
          getLastFocusedOrOpenedProject()?.service<RunnerService>()?.runCheckerWithFile(module, false)
        }
      }
    }

    sendOk(request, context)
    return null
  }

  fun parseServerData(encodedPayload: String): DecodedRequestData {
    if (encodedPayload.isBlank()) return DecodedRequestData(emptyList(), "")
    val parts = encodedPayload.split(delimiter)
    val extraData = parts.last()
    val items = parts.dropLast(1)
    return DecodedRequestData(items, extraData)
  }

  override fun isMethodSupported(method: HttpMethod): Boolean {
    return method == HttpMethod.GET || method == HttpMethod.POST
  }

  data class DecodedRequestData(
    val modulePaths: List<String>,
    val libraryName: String
  )

}