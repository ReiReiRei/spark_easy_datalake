import java.nio.file.Paths

import pureconfig.{CamelCase, ConfigFieldMapping, ProductHint, loadConfig}

case class StreamingJob(
    source: Source,
    sink: Sink,
    hive: Option[Hive]
)

object StreamingJob {
  implicit def hint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
  def fromConfig(path: Option[String] = None): StreamingJob =
    path.fold(loadConfig[StreamingJob])(
      x => loadConfig[StreamingJob](Paths.get(x))
    ) match {
      case Left(_)     => throw new IllegalArgumentException("Bad config")
      case Right(conf) => conf
    }
}
