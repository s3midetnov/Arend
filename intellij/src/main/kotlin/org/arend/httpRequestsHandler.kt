package org.arend

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.arend.ext.module.ModuleLocation
import org.arend.ext.module.ModuleLocation.LocationKind
import org.arend.ext.module.ModulePath
import org.arend.naming.reference.TCDefReferable
import org.arend.typechecking.runner.RunnerService
import org.jetbrains.ide.RestService

class MyRemoteTriggerHandler : RestService() {
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
//    var modulePath = urlDecoder.parameters()["action"]?.firstOrNull()
    var modulePath = "myfirstfile"
    ApplicationManager.getApplication().invokeLater {
      modulePath?.let{
        val module = ModuleLocation("demo", LocationKind.SOURCE, ModulePath.fromString(modulePath))
        getLastFocusedOrOpenedProject()?.service<RunnerService>()?.runChecker(module, false, true)
      }

    }

    sendOk(request, context)
    return null
  }

  fun typeCheckModules(modules : List<ModuleLocation>){
    val project = getLastFocusedOrOpenedProject() ?: return
    for (module in modules){
      project.service<RunnerService>().runChecker(module, false, true)
    }
  }

  override fun isMethodSupported(method: HttpMethod): Boolean {
    return method == HttpMethod.GET || method == HttpMethod.POST
  }
}