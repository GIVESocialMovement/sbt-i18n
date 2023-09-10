package givers.i18n

import play.api.libs.json.{JsValue, Json}

object VanillaJsI18nSerializer extends Serializer {
  def apply(locale: String, map: Map[String, String]): String = {
    /**
      * We add the option `i18n` to every Vue instantiation. We need to make sure we add the option before
      * VueI18n's `beforeCreate` (https://github.com/kazupon/vue-i18n/blob/dev/dist/vue-i18n.js#L247).
      * So, we use `unshift`.
      */
    s"""
       |var messages = ${Json.stringify(buildMap(map))};
       |
       |function t(key, variables) {
       |  const comps = key.split('.');
       |
       |  var value = messages;
       |
       |  for (var comp of comps) {
       |    if (value[comp]) {
       |      value = value[comp];
       |    } else {
       |      return key;
       |    }
       |  }
       |
       |  if (!variables) { return value; }
       |
       |  for (const [key, v] of Object.entries(variables)) {
       |    value = value.replaceAll(`{$${key}}`, v);
       |  }
       |
       |  return value;
       |}
     """.stripMargin.trim
  }

  private[i18n] def buildMap(m: Map[String, String], prefix: String = ""): JsValue = {
    val (nodes, leaves) = m.partition { case (key, _) => key.contains(".") }

    val resultLeaves = leaves.mapValues { v => Json.toJsFieldJsValueWrapper(v) }

    val resultNodes = nodes
      .groupBy { case (key, _) =>
        key.split('.').head
      }
      .map { case (first, child) =>
        first -> child.map { case (key, value) =>
          key.substring(first.length + 1) -> value
        }
      }
      .map { case (key, v) => key -> Json.toJsFieldJsValueWrapper(buildMap(v, s"$prefix$key.")) }

    resultLeaves.keys.toSet.intersect(resultNodes.keys.toSet).headOption.foreach { firstOverlapped =>
      throw new Exception(s"$prefix$firstOverlapped is a string and a map at the same time. This is not allowed.")
    }

    Json.obj(resultLeaves.toSeq:_*) ++ Json.obj(resultNodes.toSeq:_*)
  }
}
