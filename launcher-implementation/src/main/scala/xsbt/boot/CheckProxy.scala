/* sbt -- Simple Build Tool
 * Copyright 2009 Mark Harrah
 */
package xsbt.boot

import Pre._
import java.net.{ MalformedURLException, URL, Authenticator, PasswordAuthentication }
import java.net.Authenticator.RequestorType

object CheckProxy {
  def apply() {
    import ProxyProperties._
    for (pp <- Seq(http, https, ftp))
      setFromEnv(pp)
    Authenticator.setDefault(new Authenticator() {
      override protected def getPasswordAuthentication: PasswordAuthentication = {
        if (getRequestorType() == RequestorType.PROXY) {
          val prot = getRequestingProtocol().toLowerCase()
          val host = System.getProperty(prot + ".proxyHost", "")
          val port = Integer.parseInt(System.getProperty(prot + ".proxyPort", "80"))
          val user = System.getProperty(prot + ".proxyUser", "")
          val password = System.getProperty(prot + ".proxyPassword", "")
          if (getRequestingHost().equalsIgnoreCase(host) && port == getRequestingPort()) {
            new PasswordAuthentication(user, password.toCharArray())
          }
        }
        null
      }
    })
  }

  private[this] def setFromEnv(conf: ProxyProperties) {
    import conf._
    val proxyURL = System.getenv(envURL)
    if (isDefined(proxyURL) && !isPropertyDefined(sysHost) && !isPropertyDefined(sysPort)) {
      try {
        val proxy = new URL(proxyURL)
        setProperty(sysHost, proxy.getHost)
        val port = proxy.getPort
        if (port >= 0)
          System.setProperty(sysPort, port.toString)
        copyEnv(envUser, sysUser)
        copyEnv(envPassword, sysPassword)
      } catch {
        case e: MalformedURLException =>
          System.out.println(s"Warning: could not parse $envURL setting: ${e.toString}")
      }
    }
  }

  private def copyEnv(envKey: String, sysKey: String) { setProperty(sysKey, System.getenv(envKey)) }
  private def setProperty(key: String, value: String) { if (value != null) System.setProperty(key, value) }
  private def isPropertyDefined(k: String) = isDefined(System.getProperty(k))
  private def isDefined(s: String) = s != null && isNonEmpty(s)
}
