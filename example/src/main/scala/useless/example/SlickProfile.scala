package useless.example

import com.github.tminglei.slickpg._

trait SlickProfile extends ExPostgresProfile {

  override val api: ExtendedAPI.type = ExtendedAPI

  object ExtendedAPI extends API
}

object SlickProfile extends SlickProfile
