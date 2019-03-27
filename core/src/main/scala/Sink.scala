case class Sink(
    format: String,
    saveMode: String = "append",
    partitionBy: Option[Seq[String]],
    triggerInSeconds: Int,
    finalNumberOfPartitions: Option[Int],
    path: String,
    checkpointLocation: String,
    options: Option[Map[String, String]]
)
