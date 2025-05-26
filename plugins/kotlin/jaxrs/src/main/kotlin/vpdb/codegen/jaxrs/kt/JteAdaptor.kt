package vpdb.codegen.jaxrs.kt

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.TemplateOutput
import gg.jte.output.StringOutput
import gg.jte.output.WriterOutput
import org.openapitools.codegen.api.TemplatingEngineAdapter
import org.openapitools.codegen.api.TemplatingExecutor
import org.openapitools.codegen.model.OperationsMap
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.reflect.cast

object JteAdaptor: TemplatingEngineAdapter {
  private val compiler = TemplateEngine.createPrecompiled(ContentType.Plain)
  private val extensions = arrayOf("kte")

  val templateToBundleTypeMapping = mutableMapOf<String, (Map<String, Any>) -> Any>(
    "model/model.kte" to ::ModelRoot,
    "api/api.kte"    to OperationsMap::class::cast,
  )

  override fun getIdentifier() = "jte"

  override fun getFileExtensions(): Array<String> = extensions

  override fun compileTemplate(executor: TemplatingExecutor, bundle: Map<String, Any>, templateFile: String) =
    StringOutput()
      .also { compileTemplate(bundle, templateFile, it) }
      .toString()

  fun compileTemplate(bundle: Map<String, Any>, templateFile: String, target: Path) {
    target.bufferedWriter().use {
      compileTemplate(bundle, templateFile, WriterOutput(it))
      it.flush()
    }
  }

  fun compileTemplate(param: Any, templateFile: String, target: Path) {
    target.bufferedWriter().use {
      compiler.render(templateFile, param, WriterOutput(it))
      it.flush()
    }
  }

  private fun compileTemplate(bundle: Map<String, Any>, templateFile: String, target: TemplateOutput) {
    templateToBundleTypeMapping[templateFile]
      ?.let { compiler.render(templateFile, it(bundle), target) }
      ?: compiler.render(templateFile, bundle, target)
  }
}
