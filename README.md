sbt-i18n
=========

[![CircleCI](https://circleci.com/gh/GIVESocialMovement/sbt-i18n/tree/master.svg?style=shield)](https://circleci.com/gh/GIVESocialMovement/sbt-i18n/tree/master)
[![codecov](https://codecov.io/gh/GIVESocialMovement/sbt-i18n/branch/master/graph/badge.svg)](https://codecov.io/gh/GIVESocialMovement/sbt-i18n)

This plugin unifies (server-side and client-side) internationalization for Playframework.

It hot-reloads [internationalization manifest files](https://www.playframework.com/documentation/2.6.x/ScalaI18N) into Javascript files.
In turn, the Javascript files can be used for internationalization on client-side.

This plugin is currently used at [GIVE.asia](https://give.asia)

Support
----------------

* [vue-i18n](https://github.com/kazupon/vue-i18n)


How to use it
---------------

### 1. Install the plugin

Add the below lines to `project/plugins.sbt`:

```
lazy val root = Project("plugins", file(".")).aggregate(sbtI18n).dependsOn(sbtI18n)

lazy val sbtI18n = RootProject(uri("git://github.com/GIVESocialMovement/sbt-i18n.git#f033c964a0b53d1b1aa10dc97328a612dc7d5425"))
```

You may change `f033c964a0b53d1b1aa10dc97328a612dc7d5425` to a specific commit that you want.


### 2. Configure build.sbt and conf/application.conf

In `build.sbt`, you may set the below values:

```
I18nKeys.i18n / I18nKeys.defaultLocale := "en" // Set the default locale (which serves as a fallback). Default to "en"
I18nKeys.i18n / I18nKeys.path := new File("./conf/locale") // Set the path that contains messages.*. The path must be a sub-path of ./conf. Default to ./conf/locale
I18nKeys.i18n / I18nKeys.serializer := givers.i18n.VueI18nSerializer // Define the serializer. Default to VueI18nSerializer for vue-i18n.
```

Since `I18nKeys.path` is `./conf/locale` and `I18nKeys.defaultLocale` is `en`,
we need to set the Playframework's i18n path and langs in `application.conf`:

```
play.i18n.path = "locale" // We only need to specify the sub path of `./conf`
play.i18n.langs = [ "en", "th", "zh" ] // The first lang becomes the default lang
```


### 3. Move messages.* to I18nKeys.path

Since `I18nKeys.path` is `./conf/locale`, we need to move all `messages.*`, including `messages`, to `./conf/locale`.


### 4. Inject the Javascript file

The Javascript file for a `messages` file will be created under `locale`. We can inject it into an `.scala.html` file.
For example, you may include it in `main.scala.html` as shown below:

```
@(messages: play.api.i18n.Messages)

<script src="@routes.Assets.versioned("locale/messages." + messages.lang.code + ".js")" type="text/javascript"></script>
```

Please note that `messages` is compiled to `messages.[defaultLocale].js` while `messages.de` is compiled to `messages.de.js`.


### 5. Use the translation on server and client side

```
@(messages: play.api.i18n.Messages)

<script src="https://cdnjs.cloudflare.com/ajax/libs/vue/2.5.16/vue.js"></script>
<script src="https://unpkg.com/vue-i18n/dist/vue-i18n.js"></script>
<script src="@routes.Assets.versioned("locale/messages." + messages.lang.code + ".js")" type="text/javascript"></script>

<p>From server side: <strong>@messages("home.messageFromServerSide")</strong></p>

<p id="app">From client side: <strong>{{ $t('home.messageFromClientSide') }}</strong></p>

<script>
  var app = new Vue({el: '#app'})
</script>
```

Please inspect `messages.en.js` (or similar file). With `VueI18nSerializer` (for [vue-i18n](https://github.com/kazupon/vue-i18n)), we automatically inject `i18n` into every Vue's instance.
Therefore, we don't need to install [vue-i18n](https://github.com/kazupon/vue-i18n) manually when instantiating Vue.

See the `test-play-project` directory for a complete example.


Interested in using the plugin?
---------------------------------

Please feel free to open an issue to ask questions. Let us know how you want to use the plugin. We want you to use the plugin successfully.


Contributing
--------------

1. `sbt test` to run tests.
2. To test the plugin on an actual Playframework, please go to `test-play-project`.


Future improvement
--------------------

* Some keys might not be used by client side. We should allow users to exclude some keys.
