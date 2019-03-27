object TypedExample {
  StreamingApplication.run[User, TaggedUser](convert)

  def convert(user: User): Option[TaggedUser] = {
    import user._
    Some(TaggedUser(Some("tag"), name, action, dt, platform))
  }
}

case class User(
    name: Option[String],
    action: Option[String],
    dt: Option[String],
    platform: Option[String]
)

case class TaggedUser(
    tag: Option[String],
    name: Option[String],
    action: Option[String],
    dt: Option[String],
    platform: Option[String]
)
