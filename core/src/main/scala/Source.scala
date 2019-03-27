case class Source(
    format: String,
    options: Option[Map[String, String]],
    schema: Option[String],
    columns: Option[Seq[String]],
    filter: Option[String]
)
