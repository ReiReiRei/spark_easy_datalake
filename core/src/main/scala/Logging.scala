import org.apache.log4j.Logger

trait Logging {
  private lazy val name = this.getClass.toString
  @transient lazy val logger: Logger = org.apache.log4j.Logger.getLogger(name)
}
