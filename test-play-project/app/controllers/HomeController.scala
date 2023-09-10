package controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{AbstractController, ControllerComponents, MessagesAbstractController, MessagesControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(
  cc: MessagesControllerComponents
)(
  implicit ec: ExecutionContext
) extends MessagesAbstractController(cc) with I18nSupport {

  def index() = Action.async { request =>
    Future(Ok(views.html.index()))
  }

  def vue() = Action.async { request =>
    Future(Ok(views.html.vue(request.messages)))
  }

  def vanilla() = Action.async { request =>
    Future(Ok(views.html.vanilla(request.messages)))
  }

  def change(locale: String) = Action.async { request =>
    Future(Redirect(request.headers.get("referer").getOrElse("/")).withLang(Lang.get(locale).getOrElse(Lang.defaultLang)))
  }
}
