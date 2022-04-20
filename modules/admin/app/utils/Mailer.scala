package utils.admin

import models.{ PrivateUsers, Users }
import mailer.MailService
import play.twirl.api.Html
import play.api.i18n.Messages
import views.html.admin.mails
import views.html.admin.mails

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def activateAccount(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("b2b.mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

  def createStaff(account: PrivateUsers, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(account.getEmail)(
      subject = "Account Created",
      bodyHtml = mails.welcomeStaff(account, link),
      bodyText = mails.welcomeStaff(account, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("admin.mail.forgotpwd.subject"),
      bodyHtml = mails.forgotPassword(email, link),
      bodyText = mails.forgotPasswordTxt(email, link)
    )
  }

  def b2bAgentApprove(user: Users, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(user.getEmail)(
      subject = Messages("b2b.mail.account.activated"),
      bodyHtml = mails.agentSignUp(user.getFirstName, link),
      bodyText = mails.agentSignUpTxt(user.getFirstName, link)
    )
  }

}