package givers.i18n

import helpers.BaseSpec
import utest._

object Vue18nSerializerSpec extends BaseSpec {
  val tests = Tests {
    'apply - {
      val output = VueI18nSerializer.apply(
        locale = "en-GB",
        map = Map(
          "test.string" -> "sss",
          "test.map.a" -> "aaa",
          "test.map.b" -> "bbb"
        )
      )

      output ==>
        """
          |"use strict";
          |
          |var vueI18n = new VueI18n({
          |  locale: 'en-GB',
          |  messages: {"en-GB":{"test":{"string":"sss","map":{"a":"aaa","b":"bbb"}}}}
          |});
          |
          |Vue.options.beforeCreate.unshift(function() {
          |  this.$options['i18n'] = vueI18n;
          |});
        """.stripMargin.trim
    }

    'errorOnRedundantKeys - {
      val ex = intercept[Exception] {
        VueI18nSerializer.apply(
          locale = "en-GB",
          map = Map(
            "test.string" -> "sss",
            "test.map" -> "mmm",
            "test.map.a" -> "aaa"
          )
        )
      }
      ex.getMessage ==> "test.map is a string and a map at the same time. vue-i18n doesn't allow it."
    }
  }

}
