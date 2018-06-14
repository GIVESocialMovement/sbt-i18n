package givers.i18n

import play.api.libs.json.{JsValue, Json}

object VueI18nSerializer extends Serializer {
  def apply(locale: String, map: Map[String, String]): String = {
    Json.obj(locale -> buildMap(map)).toString
  }

  private[i18n] def buildMap(m: Map[String, String]): JsValue = {
    val (nodes, leaves) = m.partition { case (key, _) => key.contains(".") }

    val resultLeaves = leaves.mapValues { v => Json.toJsFieldJsValueWrapper(v) }.toSeq

    val resultNodes = nodes
      .groupBy { case (key, _) =>
        key.split('.').head
      }
      .map { case (prefix, child) =>
        prefix -> child.map { case (key, value) =>
          key.substring(prefix.length + 1) -> value
        }
      }
      .mapValues { v => Json.toJsFieldJsValueWrapper(buildMap(v)) }
      .toList

    Json.obj(resultLeaves:_*) ++ Json.obj(resultNodes:_*)
  }


}
